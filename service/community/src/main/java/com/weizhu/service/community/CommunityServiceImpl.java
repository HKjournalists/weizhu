package com.weizhu.service.community;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.CommunityProtos.GetHotCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetHotCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetPostCommentByIdResponse;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.GetAbilityTagUserIdResponse;
import com.weizhu.proto.UserService;
import com.weizhu.proto.CommunityProtos.CreateCommentRequest;
import com.weizhu.proto.CommunityProtos.CreateCommentResponse;
import com.weizhu.proto.CommunityProtos.CreatePostRequest;
import com.weizhu.proto.CommunityProtos.CreatePostResponse;
import com.weizhu.proto.CommunityProtos.DeleteCommentRequest;
import com.weizhu.proto.CommunityProtos.DeleteCommentResponse;
import com.weizhu.proto.CommunityProtos.DeletePostRequest;
import com.weizhu.proto.CommunityProtos.DeletePostResponse;
import com.weizhu.proto.CommunityProtos.GetBoardListRequest;
import com.weizhu.proto.CommunityProtos.GetBoardListResponse;
import com.weizhu.proto.CommunityProtos.GetCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetCommunityRequest;
import com.weizhu.proto.CommunityProtos.GetCommunityResponse;
import com.weizhu.proto.CommunityProtos.GetMyCommentListRequest;
import com.weizhu.proto.CommunityProtos.GetMyCommentListResponse;
import com.weizhu.proto.CommunityProtos.GetMyPostListRequest;
import com.weizhu.proto.CommunityProtos.GetMyPostListResponse;
import com.weizhu.proto.CommunityProtos.GetPostByIdsRequest;
import com.weizhu.proto.CommunityProtos.GetPostByIdsResponse;
import com.weizhu.proto.CommunityProtos.GetPostListRequest;
import com.weizhu.proto.CommunityProtos.GetPostListResponse;
import com.weizhu.proto.CommunityProtos.LikeCommentResponse;
import com.weizhu.proto.CommunityProtos.LikePostRequest;
import com.weizhu.proto.CommunityProtos.LikePostResponse;
import com.weizhu.proto.CommunityService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.zaxxer.hikari.HikariDataSource;

public class CommunityServiceImpl implements CommunityService {

	private static final Logger logger = LoggerFactory.getLogger(CommunityServiceImpl.class);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final ScheduledExecutorService scheduledExecutorService;
	private final PushService pushService;
	private final UserService userService;
	private final AdminOfficialService adminOfficialService;

	private final static int POST_HOT_COMMENT_MAX_NUM = 5;
	private final AllowService allowService;

	@Inject
	public CommunityServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor,
			@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutorService, PushService pushService, UserService userService, AdminOfficialService adminOfficialService, AllowService allowService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.scheduledExecutorService = scheduledExecutorService;
		this.pushService = pushService;
		this.userService = userService;
		this.adminOfficialService = adminOfficialService;
		this.allowService = allowService;

		long delay = (System.currentTimeMillis()) % (60 * 60 * 1000);
		if (delay > 30 * 60 * 1000) {
			delay -= 30 * 60 * 1000;
		}

		// 每整点或整点半运行一次，例如： 12:00 12:30 13:00 ...
		// 时间衰减系数：0.998/半小时, 半衰期大概一周
		this.scheduledExecutorService.scheduleAtFixedRate(new RefreshBoardHotPostTask(), delay, 30 * 60 * 1000, TimeUnit.MILLISECONDS);
	}

	private Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> doGetBoardHotPostIdMap(long companyId, Collection<Integer> boardIds) {
		if (boardIds.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> resultMap = new HashMap<Integer, List<CommunityDAOProtos.HotPostListIndex>>();

		Set<Integer> noCacheBoardIdSet = new TreeSet<Integer>();
		Jedis jedis = this.jedisPool.getResource();
		try {
			resultMap.putAll(CommunityCache.getBoardHotPostListIndex(jedis, companyId, boardIds, noCacheBoardIdSet));
		} finally {
			jedis.close();
		}

		if (noCacheBoardIdSet.isEmpty()) {
			return resultMap;
		}

		Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> noCacheMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			noCacheMap = CommunityDB.getBoardHotPostIdList(dbConn, companyId, noCacheBoardIdSet, 1, 5, 3, 100);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setBoardHotPostListIndex(jedis, companyId, noCacheBoardIdSet, noCacheMap);
		} finally {
			jedis.close();
		}

		resultMap.putAll(noCacheMap);

		return resultMap;
	}

	private final class RefreshBoardHotPostTask implements Runnable {

		@Override
		public void run() {

			Connection dbConn = null;

			Set<Long> companyIds = new TreeSet<Long>();
			try {
				dbConn = CommunityServiceImpl.this.hikariDataSource.getConnection();
				companyIds = CommunityDB.getCompanyId(dbConn);
			} catch (Throwable th) {
				logger.error("refresh board hot post fail!", th);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}

			for (long companyId : companyIds) {
				try {
					CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(CommunityServiceImpl.this.jedisPool,
							CommunityServiceImpl.this.hikariDataSource,
							companyId);

					List<Integer> leafBoardIdList = new ArrayList<Integer>();
					if (communityInfo != null) {
						for (CommunityProtos.Board board : communityInfo.getBoardList()) {
							if (board.getIsLeafBoard()) {
								leafBoardIdList.add(board.getBoardId());
							}
						}
					}

					if (leafBoardIdList.isEmpty()) {
						return;
					}

					final int now = (int) (System.currentTimeMillis() / 1000L);

					Map<Integer, List<CommunityDAOProtos.HotPostListIndex>> boardHotPostIdListMap;
					try {
						dbConn = CommunityServiceImpl.this.hikariDataSource.getConnection();
						CommunityDB.refreshBoardHotPostCount(dbConn, companyId, now, 0.998D);
						boardHotPostIdListMap = CommunityDB.getBoardHotPostIdList(dbConn, companyId, leafBoardIdList, 1, 5, 3, 100);
					} finally {
						DBUtil.closeQuietly(dbConn);
					}

					Jedis jedis = CommunityServiceImpl.this.jedisPool.getResource();
					try {
						CommunityCache.setBoardHotPostListIndex(jedis, companyId, leafBoardIdList, boardHotPostIdListMap);
					} finally {
						jedis.close();
					}

				} catch (Throwable th) {
					logger.error("refresh board hot post fail! companyId = " + companyId, th);
				}
			}
			logger.info("refresh board hot post succ. ");
		}
	}

	@Override
	public ListenableFuture<GetCommunityResponse> getCommunity(RequestHead head, GetCommunityRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(this.jedisPool, this.hikariDataSource, companyId);
		// 收集所有的model id
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		Map<Integer, CommunityProtos.Board> modelMap = new HashMap<Integer, CommunityProtos.Board>();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			if (board.hasAllowModelId()) {
				modelIdSet.add(board.getAllowModelId());
			}
			modelMap.put(board.getBoardId(), board);
		}
		
		Set<Integer> allowModelIds = this.doCheckAllowModelId(head, modelIdSet);

		// 根据allowModelIds过滤board
		List<Integer> rootBoardIds = new ArrayList<Integer>();
		Map<Integer, List<Integer>> parentIdChildrenIdsMap = new HashMap<Integer, List<Integer>>();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			if (!this.isValid(board, allowModelIds)) {
				continue;
			}
			if (!board.hasParentBoardId()) {
				rootBoardIds.add(board.getBoardId());
			} else {
				List<Integer> childrenIds = parentIdChildrenIdsMap.get(board.getParentBoardId());
				if (childrenIds == null) {
					childrenIds = new ArrayList<Integer>();
					parentIdChildrenIdsMap.put(board.getParentBoardId(), new ArrayList<Integer>());
				}
				childrenIds.add(board.getBoardId());
			}
		}

		Set<Integer> leafBoardIdSet = new TreeSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>(rootBoardIds);
		while (!queue.isEmpty()) {
			int boardId = queue.poll();
			List<Integer> childrenIds = parentIdChildrenIdsMap.get(boardId);
			if (childrenIds != null && !childrenIds.isEmpty()) {
				queue.addAll(childrenIds);
			}
			CommunityProtos.Board board = modelMap.get(boardId);
			if (board.getIsLeafBoard()) {
				leafBoardIdSet.add(board.getBoardId());
			}
		}
		
		Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap = CommunityUtil.doGetBoardExt(this.jedisPool,
				this.hikariDataSource,
				companyId,
				leafBoardIdSet);
		Set<Integer> boardLatestPostIdSet = new TreeSet<Integer>(request.getBoardLatestPostIdList());

		int totalPostNewCount = 0;
		for (Entry<Integer, CommunityDAOProtos.BoardExt> entry : boardExtMap.entrySet()) {
			CommunityProtos.Board board = modelMap.get(entry.getKey());
			if (board == null || entry.getValue() == null) {
				continue;
			}
			
			for (CommunityDAOProtos.PostListIndex postListIndex : entry.getValue().getIndexList()) {
				if (boardLatestPostIdSet.contains(postListIndex.getPostId())) {
					break;
				}
				if (CommunityUtil.USER_POST_STATE_LIST.contains(postListIndex.getState())) {
					totalPostNewCount++;
				}
			}
		}

		return Futures.immediateFuture(
				GetCommunityResponse.newBuilder().setCommunityName(communityInfo.getCommunityName()).setPostNewCount(totalPostNewCount).build());
	}

	@Override
	public ListenableFuture<GetBoardListResponse> getBoardList(RequestHead head, GetBoardListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(this.jedisPool, this.hikariDataSource, companyId);
		
		// 收集所有的model id
		Set<Integer> modelIdSet = new TreeSet<Integer>();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			if(board.hasAllowModelId()){
				modelIdSet.add(board.getAllowModelId());
			}
		}
		Set<Integer> allowModelIds = this.doCheckAllowModelId(head, modelIdSet);
		List<Integer> rootBoardIdList = new ArrayList<Integer>();
		Map<Integer, List<Integer>> parentChildrenBoardIdListMap = new HashMap<Integer, List<Integer>>();
		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
		Set<Integer> leafBoardIdSet = new TreeSet<Integer>();

		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			if (!this.isValid(board, allowModelIds)) {
				continue;
			}
			
			boardMap.put(board.getBoardId(), board);
			if (board.getIsLeafBoard()) {
				leafBoardIdSet.add(board.getBoardId());
			}
			
			if (!board.hasParentBoardId()) {
				rootBoardIdList.add(board.getBoardId());
			} else {
				int parentBoardId = board.getParentBoardId();
				List<Integer> parentChildrenBoardIdList = parentChildrenBoardIdListMap.get(parentBoardId);
				if (parentChildrenBoardIdList == null) {
					parentChildrenBoardIdList = new ArrayList<Integer>();
					parentChildrenBoardIdListMap.put(parentBoardId, parentChildrenBoardIdList);
				}
				parentChildrenBoardIdList.add(board.getBoardId());
			}
		}
		
		// 过滤出当前用户能过访问的版块的ID
		List<Integer> tmpfillteredBoardIdList = new ArrayList<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>(rootBoardIdList);
		while (!queue.isEmpty()) {
			int boardId = queue.poll();
			List<Integer> parentChildrenBoardIdList = parentChildrenBoardIdListMap.get(boardId);

			tmpfillteredBoardIdList.add(boardId);
			if (parentChildrenBoardIdList != null && !parentChildrenBoardIdList.isEmpty()) {
				queue.addAll(parentChildrenBoardIdList);
			}
		}
		
		// 若板块下没有叶子板块，则板块不显示
		List<Integer> fillteredBoardIdList = new ArrayList<Integer>(tmpfillteredBoardIdList);
		LinkedList<Integer> stack = new LinkedList<Integer>();
		for(int boardId : tmpfillteredBoardIdList){
			stack.push(boardId);
		}
		while (!stack.isEmpty()) {
			Integer boardId = stack.pop();
			CommunityProtos.Board board = boardMap.get(boardId);
			if (board == null) {
				fillteredBoardIdList.remove(boardId);
				continue;
			}
			List<Integer> childrenBoardIdList = parentChildrenBoardIdListMap.get(boardId);

			if (!board.getIsLeafBoard() && (childrenBoardIdList == null || childrenBoardIdList.isEmpty())) {
				fillteredBoardIdList.remove(boardId);

				if (board.hasParentBoardId()) {
					List<Integer> parentChildrenBoardIdList = parentChildrenBoardIdListMap.get(board.getParentBoardId());
					if (parentChildrenBoardIdList != null) {
						parentChildrenBoardIdList.remove(boardId);
					}
				}
			}
		}
		
//		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
//		Set<Integer> leafBoardIdSet = new TreeSet<Integer>();
//		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
//			boardMap.put(board.getBoardId(), board);
//			if (board.getIsLeafBoard()) {
//				leafBoardIdSet.add(board.getBoardId());
//			}
//		}

		Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap = CommunityUtil.doGetBoardExt(this.jedisPool, this.hikariDataSource, companyId, leafBoardIdSet);
		Set<Integer> boardLatestPostIdSet = new TreeSet<Integer>(request.getBoardLatestPostIdList());

		Map<Integer, Integer> postTotalCountMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> postNewCountMap = new HashMap<Integer, Integer>();

		for (Entry<Integer, CommunityDAOProtos.BoardExt> entry : boardExtMap.entrySet()) {
			CommunityProtos.Board board = boardMap.get(entry.getKey());
			if (board == null) {
				continue;
			}

			int totalCnt = 0;
			for (CommunityDAOProtos.PostCount cnt : entry.getValue().getCountList()) {
				if (CommunityUtil.USER_POST_STATE_LIST.contains(cnt.getState())) {
					totalCnt += cnt.getCount();
				}
			}

			postTotalCountMap.put(entry.getKey(), totalCnt);

			int newCnt = 0;
			for (CommunityDAOProtos.PostListIndex postListIndex : entry.getValue().getIndexList()) {
				if (boardLatestPostIdSet.contains(postListIndex.getPostId())) {
					break;
				}
				if (CommunityUtil.USER_POST_STATE_LIST.contains(postListIndex.getState())) {
					newCnt++;
				}
			}
			postNewCountMap.put(entry.getKey(), newCnt);

			while (board.hasParentBoardId()) {
				board = boardMap.get(board.getParentBoardId());
				if (board == null) {
					// error
					break;
				}

				Integer oldTotalCnt = postTotalCountMap.get(board.getBoardId());
				postTotalCountMap.put(board.getBoardId(), oldTotalCnt == null ? totalCnt : oldTotalCnt + totalCnt);
				Integer oldNewCnt = postNewCountMap.get(board.getBoardId());
				postNewCountMap.put(board.getBoardId(), oldNewCnt == null ? newCnt : oldNewCnt + newCnt);
			}
		}

		GetBoardListResponse.Builder responseBuilder = GetBoardListResponse.newBuilder();

		CommunityProtos.Board.Builder tmpBoardBuilder = CommunityProtos.Board.newBuilder();
		for (int boardId : fillteredBoardIdList) {
			
			CommunityProtos.Board board = boardMap.get(boardId);
			if(board == null){
				continue;
			}
			
			Integer totalCnt = postTotalCountMap.get(board.getBoardId());
			Integer newCnt = postNewCountMap.get(board.getBoardId());
			CommunityDAOProtos.BoardExt boardExt = boardExtMap.get(board.getBoardId());
			
			if ((totalCnt == null || totalCnt == 0) && (newCnt == null || newCnt == 0) && boardExt == null) {
				responseBuilder.addBoard(board);
			} else {
				tmpBoardBuilder.clear();

				tmpBoardBuilder.mergeFrom(board);
				tmpBoardBuilder.setPostTotalCount(totalCnt == null ? 0 : totalCnt);
				tmpBoardBuilder.setPostNewCount(newCnt == null ? 0 : newCnt);
				if(boardExt != null && boardExt.getTagCount() > 0){
					tmpBoardBuilder.addAllTag(boardExt.getTagList());
				}
				responseBuilder.addBoard(tmpBoardBuilder.build());
			}
		}

		return Futures.immediateFuture(responseBuilder.build());
	}
	private boolean isValid(CommunityProtos.Board board, @Nullable Collection<Integer> allowModelIds) {
		if (allowModelIds != null && board.hasAllowModelId() && !allowModelIds.contains(board.getAllowModelId())) {
			return false;
		}
		return true;
	}
	private Set<Integer> doCheckAllowModelId(RequestHead head, Set<Integer> modelIdSet) {
		if (modelIdSet.isEmpty()) {
			return Collections.emptySet();
		} 
		
		AllowProtos.CheckAllowResponse checkAllowResponse = Futures.getUnchecked(
				this.allowService.checkAllow(head, AllowProtos.CheckAllowRequest.newBuilder()
						.addAllModelId(modelIdSet)
						.addUserId(head.getSession().getUserId())
						.build()));
		
		Set<Integer> allowedModelIdSet = new TreeSet<Integer>();
		for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
			if (checkResult.getAllowUserIdList().contains(head.getSession().getUserId())) {
				allowedModelIdSet.add(checkResult.getModelId());
			}
		}
		
		return allowedModelIdSet;
	}
	
	@Override
	public ListenableFuture<GetPostListResponse> getPostList(RequestHead head, GetPostListRequest request) {
		switch (request.getListType()) {
			case CREATE_TIME:
				return Futures.immediateFuture(this.doGetPostListCreateTime(head, request));
			case TOP_HOT:
				return Futures.immediateFuture(this.doGetPostListTopHot(head, request));
			default:
				return Futures.immediateFuture(GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(ByteString.EMPTY).build());
		}	}

	private static final CommunityDAOProtos.PostListIndex POST_LIST_INDEX_END = CommunityDAOProtos.PostListIndex.newBuilder()
			.setCreateTime(0)
			.setPostId(0)
			.setState(CommunityProtos.Post.State.NORMAL)
			.build();
	private GetPostListResponse doGetPostListCreateTime(RequestHead head, GetPostListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int boardId = request.getBoardId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final CommunityDAOProtos.PostListIndex currentOffsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityDAOProtos.PostListIndex tmp = null;
			try {
				tmp = CommunityDAOProtos.PostListIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		if (currentOffsetIndex != null && currentOffsetIndex.equals(POST_LIST_INDEX_END)) {
			return GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(POST_LIST_INDEX_END.toByteString()).build();
		}

		CommunityDAOProtos.BoardExt boardExt = CommunityUtil.doGetBoardExt(this.jedisPool, this.hikariDataSource, companyId, Collections.singleton(boardId))
				.get(boardId);
		if (boardExt == null) {
			return GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(POST_LIST_INDEX_END.toByteString()).build();
		}
		
		
		List<CommunityDAOProtos.PostListIndex> postListIndexList = this.doGetPostIndexListCreateTime(head.getSession().getCompanyId(), boardExt, currentOffsetIndex, size + 1, boardId, false);
		
		final boolean hasMore;
		final ByteString nextOffsetIndex;
		
		if (postListIndexList.size() > size) {
			hasMore = true;
			postListIndexList = postListIndexList.subList(0, size);
			
			if (postListIndexList.isEmpty()) {
				nextOffsetIndex = currentOffsetIndex == null ? ByteString.EMPTY : currentOffsetIndex.toByteString();
			} else {
				nextOffsetIndex = postListIndexList.get(postListIndexList.size() - 1).toByteString();
			}
		} else {
			hasMore = false;
			nextOffsetIndex = POST_LIST_INDEX_END.toByteString();
		}
		
		List<Integer> postIdList = new ArrayList<Integer>(postListIndexList.size());
		for (CommunityDAOProtos.PostListIndex postListIndex : postListIndexList) {
			postIdList.add(postListIndex.getPostId());
		}
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool, this.hikariDataSource, companyId, postIdList, CommunityUtil.USER_POST_STATE_LIST);
		
		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());

		return GetPostListResponse.newBuilder().setHasMore(hasMore).setOffsetIndex(nextOffsetIndex).addAllPost(postList).build();
	}
	private GetPostListResponse doGetPostListCreateTimeSticky(RequestHead head, GetPostListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int boardId = request.getBoardId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final CommunityDAOProtos.PostListIndex currentOffsetIndex;
		
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityDAOProtos.PostListIndex tmp = null;
			try {
				tmp = CommunityDAOProtos.PostListIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		if (currentOffsetIndex != null && currentOffsetIndex.equals(POST_LIST_INDEX_END)) {
			return GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(POST_LIST_INDEX_END.toByteString()).build();
		}

		CommunityDAOProtos.BoardExt boardExt = CommunityUtil.doGetBoardExt(this.jedisPool, this.hikariDataSource, companyId, Collections.singleton(boardId))
				.get(boardId);
		if (boardExt == null) {
			return GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(POST_LIST_INDEX_END.toByteString()).build();
		}
		
		
		int sizeHasMore = size + 1;
		List<CommunityDAOProtos.PostListIndex> postIndexList = new ArrayList<CommunityDAOProtos.PostListIndex>();
		List<CommunityDAOProtos.PostListIndex> postIndexListCreateTime = new ArrayList<CommunityDAOProtos.PostListIndex>();
		List<CommunityDAOProtos.PostListIndex> stickyPostIndexList = new ArrayList<CommunityDAOProtos.PostListIndex>();
		
		for (CommunityDAOProtos.PostListIndex stickyPostListIndex : boardExt.getStickyPostIndexList()) {
			if (stickyPostListIndex == null) {
				continue;
			}

			if (CommunityUtil.USER_POST_STATE_LIST.contains(stickyPostListIndex.getState())) {
				stickyPostIndexList.add(stickyPostListIndex);
			}
		}

		if (stickyPostIndexList.size() > CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE) {
			stickyPostIndexList = stickyPostIndexList.subList(0, CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE);
		}
		
		int stickyPostIndexListSize = stickyPostIndexList.size();
		
		// 确定索引是否在置顶贴列表中，并确认其位置
		Integer stickyStart = null;
		if (currentOffsetIndex != null && currentOffsetIndex.getIsSticky()) {

			for (int i = 0; i < stickyPostIndexListSize; i++) {
				if (stickyPostIndexList.get(i).getStickyTime() < currentOffsetIndex.getStickyTime()
						|| (stickyPostIndexList.get(i).getStickyTime() == currentOffsetIndex.getStickyTime() && stickyPostIndexList.get(i).getPostId() < currentOffsetIndex.getPostId())) {
					stickyStart = i;
					break;
				}
			}
		}
		// 添加置顶贴
		if (currentOffsetIndex == null || stickyStart != null) {

			Iterator<CommunityDAOProtos.PostListIndex> stickyPIndexIterator = stickyPostIndexList.iterator();
			for (int i = 0; postIndexList.size() < sizeHasMore && stickyPIndexIterator.hasNext(); i++) {
				if (stickyStart != null && i < stickyStart) {
					stickyPIndexIterator.next();
					continue;
				}
				postIndexList.add(stickyPIndexIterator.next());
			}
		}
			
		// 添加创建时间倒序的普通贴
		postIndexListCreateTime = this.doGetPostIndexListCreateTime(companyId, boardExt,
				stickyStart == null ? currentOffsetIndex : null,
				sizeHasMore <= postIndexList.size() ? 0 : sizeHasMore - postIndexList.size(),
				boardId, true);
		

		Iterator<CommunityDAOProtos.PostListIndex> pIdCreTimeIterator = postIndexListCreateTime.iterator();
		while (postIndexList.size() < sizeHasMore && pIdCreTimeIterator.hasNext()) {
			postIndexList.add(pIdCreTimeIterator.next());
		}
		
		final boolean hasMore;
		final ByteString nextOffsetIndex;
		
		if (postIndexList.size() > size) {
			hasMore = true;
			postIndexList = postIndexList.subList(0, size);
			nextOffsetIndex = postIndexList.get(postIndexList.size() - 1).toByteString();
		} else {
			hasMore = false;
			nextOffsetIndex = POST_LIST_INDEX_END.toByteString();
		}
		
		
		List<Integer> postIdList = new ArrayList<Integer>();
		for (CommunityDAOProtos.PostListIndex postListIndex : postIndexList) {
			if (postListIndex != null) {
				postIdList.add(postListIndex.getPostId());
			}
		}
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool, this.hikariDataSource, companyId, postIdList, CommunityUtil.USER_POST_STATE_LIST);
		
		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());

		return GetPostListResponse.newBuilder().setHasMore(hasMore).setOffsetIndex(nextOffsetIndex).addAllPost(postList).build();
	}
	
	private List<CommunityDAOProtos.PostListIndex> doGetPostIndexListCreateTime(long companyId, CommunityDAOProtos.BoardExt boardExt,CommunityDAOProtos.PostListIndex currentOffsetIndex, int size, int boardId, boolean isSticky){
		
		List<CommunityDAOProtos.PostListIndex> list = new ArrayList<CommunityDAOProtos.PostListIndex>();
		for (CommunityDAOProtos.PostListIndex postListIndex : boardExt.getIndexList()) {
			if (CommunityUtil.USER_POST_STATE_LIST.contains(postListIndex.getState()) 
					&& (currentOffsetIndex == null || postListIndex.getCreateTime() < currentOffsetIndex.getCreateTime() || (postListIndex.getCreateTime() == currentOffsetIndex.getCreateTime() && postListIndex.getPostId() < currentOffsetIndex.getPostId()))) {
				
				// 置顶贴单独缓存，当置顶显示置顶帖时，此处过滤掉置顶贴
				if(isSticky && postListIndex.getIsSticky()){
					continue;
				}
				
				list.add(postListIndex);
				if (list.size() >= size + 1) {
					break;
				}
			}
		}

		if (list.size() < size && boardExt.getIndexCount() >= CommunityUtil.MAX_BOARD_POST_LIST_INDEX_SIZE) {
			// 没有取完 且db可能还有数据
			Connection dbConn = null;
			try {
				dbConn = this.hikariDataSource.getConnection();
				if (currentOffsetIndex == null) {
					list = CommunityDB.getBoardPostListIndexList(dbConn, companyId, boardId, CommunityUtil.USER_POST_STATE_LIST, size, isSticky);
				} else {
					list = CommunityDB.getBoardPostListIndexList(dbConn, companyId, boardId, CommunityUtil.USER_POST_STATE_LIST, currentOffsetIndex, size, isSticky);
				}
			} catch (SQLException e) {
				throw new RuntimeException("db fail", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
		}
		
		return list;
	}
	

	private GetPostListResponse doGetPostListTopHot(RequestHead head, GetPostListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int boardId = request.getBoardId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final int currentOffsetIndex;
		if (request.hasOffsetIndex() && request.getOffsetIndex().size() == 4) {
			int tmp = Ints.fromByteArray(request.getOffsetIndex().toByteArray());
			currentOffsetIndex = tmp < 0 ? 0 : tmp;
		} else {
			currentOffsetIndex = 0;
		}
		
		List<Integer> hotPostIdList = new ArrayList<Integer>();
		final List<CommunityDAOProtos.HotPostListIndex> hotPostListIndexs = this.doGetBoardHotPostIdMap(companyId, Collections.singleton(boardId)).get(boardId);
		
		if(hotPostListIndexs == null || hotPostListIndexs.isEmpty()){
			return GetPostListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build();
		}
		
		for (CommunityDAOProtos.HotPostListIndex hotPostListIndex : hotPostListIndexs) {
			if (hotPostListIndex != null && CommunityUtil.USER_POST_STATE_LIST.contains(hotPostListIndex.getState())) {
				hotPostIdList.add(hotPostListIndex.getPostId());
			}
		}
		
	
		if (hotPostIdList == null || hotPostIdList.isEmpty()) {
			return GetPostListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build();
		}
		
		final List<Integer> postIdList;
		final boolean hasMore;
		final ByteString nextOffsetIndex;
		if (currentOffsetIndex < hotPostIdList.size()) {
			if (currentOffsetIndex + size < hotPostIdList.size()) {
				postIdList = hotPostIdList.subList(currentOffsetIndex, currentOffsetIndex + size);
				hasMore = true;
				nextOffsetIndex = ByteString.copyFrom(Ints.toByteArray(currentOffsetIndex + size));
			} else {
				postIdList = hotPostIdList.subList(currentOffsetIndex, hotPostIdList.size());
				hasMore = false;
				nextOffsetIndex = ByteString.copyFrom(Ints.toByteArray(hotPostIdList.size()));
			}
		} else {
			postIdList = Collections.emptyList();
			hasMore = false;
			nextOffsetIndex = ByteString.copyFrom(Ints.toByteArray(currentOffsetIndex));
		}
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool, this.hikariDataSource, companyId, postIdList, CommunityUtil.USER_POST_STATE_LIST);

		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());

		return GetPostListResponse.newBuilder().setHasMore(hasMore).setOffsetIndex(nextOffsetIndex).addAllPost(postList).build();
	}
	
	private static final CommunityDAOProtos.PostCommentListIndex POST_COMMENT_LIST_INDEX_END = 
			CommunityDAOProtos.PostCommentListIndex.newBuilder()
			.setCommentId(0)
			.build();

	@Override
	public ListenableFuture<GetCommentListResponse> getCommentList(RequestHead head, GetCommentListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int postId = request.getPostId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();
		final CommunityDAOProtos.PostCommentListIndex currentOffsetIndex;

		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityDAOProtos.PostCommentListIndex tmp = null;
			try {
				tmp = CommunityDAOProtos.PostCommentListIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		if (currentOffsetIndex != null && currentOffsetIndex.equals(POST_COMMENT_LIST_INDEX_END)) {
			return Futures.immediateFuture(GetCommentListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(POST_COMMENT_LIST_INDEX_END.toByteString())
					.build());
		}

		List<CommunityProtos.Comment> commentList;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			if (currentOffsetIndex == null) {
				commentList = CommunityDB.getPostCommentList(dbConn, companyId, postId, CommunityUtil.USER_COMMENT_STATE_LIST, size + 1);
			} else {
				commentList = CommunityDB.getPostCommentList(dbConn, companyId, postId, CommunityUtil.USER_COMMENT_STATE_LIST, currentOffsetIndex, size + 1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		final boolean hasMore;
		final ByteString nextOffsetIndex;
		if (commentList.size() > size) {
			hasMore = true;
			commentList = commentList.subList(0, size);

			if (commentList.isEmpty()) {
				nextOffsetIndex = currentOffsetIndex == null ? ByteString.EMPTY : currentOffsetIndex.toByteString();
			} else {
				nextOffsetIndex = CommunityDAOProtos.PostCommentListIndex.newBuilder()
						.setCommentId(commentList.get(commentList.size() - 1).getCommentId())
						.build()
						.toByteString();
			}
		} else {
			hasMore = false;
			nextOffsetIndex = POST_COMMENT_LIST_INDEX_END.toByteString();
		}

		// 给评论赋is_like和like_count的值
		List<CommunityProtos.Comment> resultCommentList = this.setPostCommentExtValue(companyId, postId, commentList, head.getSession().getUserId());
		
		return Futures.immediateFuture(GetCommentListResponse.newBuilder()
				.addAllComment(resultCommentList)
				.setHasMore(hasMore)
				.setOffsetIndex(nextOffsetIndex)
				.build());
	}

	@Override
	public ListenableFuture<CreatePostResponse> createPost(RequestHead head, CreatePostRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final String title = request.getTitle().trim();
		final List<String> tagList = request.getTagList();
		if (title.isEmpty()) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_TITLE_INVALID)
					.setFailText("帖子标题不能为空")
					.build());
		}
		if (title.length() > 50) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_TITLE_INVALID)
					.setFailText("帖子标题最多50个字")
					.build());
		}

		final String text = request.hasText() && !request.getText().isEmpty() ? request.getText() : null;
		if (text != null && text.length() > 65535) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_TEXT_INVALID)
					.setFailText("帖子文本内容超长")
					.build());
		}

		final String imageName = request.hasImageName() && !request.getImageName().isEmpty() ? request.getImageName() : null;
		if (imageName != null && imageName.length() > 191) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_IMAGE_INVALID)
					.setFailText("图片错误")
					.build());
		}

		final int boardId = request.getBoardId();
		final CommunityProtos.Board board = CommunityUtil.doGetBoard(this.jedisPool, this.hikariDataSource, companyId, Collections.singleton(boardId)).get(0);

		if (board == null) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_BOARD_INVALID)
					.setFailText("板块不存在")
					.build());
		}
		if (!board.getIsLeafBoard()) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_BOARD_INVALID)
					.setFailText("此板块不能发表帖子")
					.build());
		}

		for (String tag : tagList) {
			if (tag.length() > 191) {
				return Futures.immediateFuture(CreatePostResponse.newBuilder()
						.setResult(CreatePostResponse.Result.FAIL_TAG_INVALID)
						.setFailText("标签长度超出限制！")
						.build());
			}
		}
		
		int currentTime = (int) (System.currentTimeMillis() / 1000L);
		final long createUserId = head.getSession().getUserId();
		
 		CommunityProtos.Post.Builder postBuilder = CommunityProtos.Post.newBuilder();
				
		postBuilder.setPostId(0);
		postBuilder.setPostTitle(title);
		if (text != null || imageName != null) {
			CommunityProtos.Post.Part.Builder partBuilder = CommunityProtos.Post.Part.newBuilder();
			partBuilder.setPartId(0);
			if (text != null) {
				partBuilder.setText(text);
			}
			if (imageName != null) {
				partBuilder.setImageName(imageName);
			}
			postBuilder.addPostPart(partBuilder.build());
		}
		postBuilder.setBoardId(boardId);
		postBuilder.setCreateUserId(createUserId);
		postBuilder.setCreateTime(currentTime);
		postBuilder.setIsHot(false);
		postBuilder.setState(CommunityProtos.Post.State.NORMAL);
		postBuilder.setCommentCount(0);
		postBuilder.setLikeCount(0);
		postBuilder.setIsLike(false);

		final CommunityProtos.Post post = postBuilder.build();
		Integer postId = null;

		Map<Integer, CommunityProtos.Post> postMap;
		Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			postId = CommunityDB.insertPost(dbConn, companyId, Collections.singletonList(post)).get(0);

			if (postId == null) {
				throw new RuntimeException("db fail");
			}
			
			CommunityDB.insertBoardHotPostCount(dbConn, companyId, postId, boardId);
			
			CommunityDB.insertPostTag(dbConn, companyId, postId, tagList);
			
			postMap = CommunityDB.getPost(dbConn, companyId, Collections.singleton(postId));
			boardExtMap = CommunityDB.getBoardExt(dbConn, companyId, Collections.singleton(boardId), CommunityUtil.MAX_BOARD_POST_LIST_INDEX_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPost(jedis, companyId, Collections.singleton(postId), postMap);
			CommunityCache.setBoardExt(jedis, companyId, Collections.singleton(boardId), boardExtMap);
		} finally {
			jedis.close();
		}

		if (!tagList.isEmpty()) {
			
			// 根据标签获取人员id，然后根据人员ID推送消息
			GetAbilityTagUserIdResponse response = null;
			try {
				response = this.userService
						.getAbilityTagUserId(head, UserProtos.GetAbilityTagUserIdRequest.newBuilder().addAllTagName(tagList).build()).get();
			} catch (InterruptedException e) {
				throw new RuntimeException("根据标签获取人员id出错！", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("根据标签获取人员id出错！", e);
			}

			List<Long> userIds = response != null ? response.getUserIdList() : Collections.emptyList();
			
			// 不通知自己
			List<Long> userIdsResult = new ArrayList<Long>();
			for (long userId : userIds) {
				if (userId != createUserId) {
					userIdsResult.add(userId);
				}
			}
			
			if (!userIdsResult.isEmpty()) {
				this.adminOfficialService.sendSecretaryMessage(head,
						AdminOfficialProtos.SendSecretaryMessageRequest.newBuilder()
								.addAllUserId(userIdsResult)
								.setSendMsg(OfficialProtos.OfficialMessage.newBuilder()
										.setMsgSeq(0)
										.setMsgTime(0)
										.setIsFromUser(false)
										.setCommunityPost(OfficialProtos.OfficialMessage.CommunityPost.newBuilder()
												.setPostId(postId)
												.setText("有您可能感兴趣的新帖：" + title)
												.build())
										.build())
								.build());
			}
		}
		
		return Futures.immediateFuture(CreatePostResponse.newBuilder().setResult(CreatePostResponse.Result.SUCC).setPostId(postId).build());
	}

	@Override
	public ListenableFuture<DeletePostResponse> deletePost(RequestHead head, DeletePostRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final int postId = request.getPostId();
		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		if (post == null || post.getState() == CommunityProtos.Post.State.DELETE) {
			return Futures.immediateFuture(DeletePostResponse.newBuilder()
					.setResult(DeletePostResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("帖子不存在")
					.build());
		}
		if (post.getCreateUserId() != head.getSession().getUserId()) {
			return Futures.immediateFuture(DeletePostResponse.newBuilder()
					.setResult(DeletePostResponse.Result.FAIL_POST_OTHER)
					.setFailText("只能删除自己创建的帖子")
					.build());
		}

		Map<Integer, CommunityProtos.Post> postMap;
		Map<Integer, CommunityDAOProtos.PostExt> postExtMap;
		Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			CommunityDB.updatePostStateDelete(dbConn, companyId, Collections.singleton(postId));

			postMap = CommunityDB.getPost(dbConn, companyId, Collections.singleton(postId));
			postExtMap = CommunityDB.getPostExt(dbConn, companyId, Collections.singleton(postId));
			boardExtMap = CommunityDB.getBoardExt(dbConn, companyId, Collections.singleton(post.getBoardId()), CommunityUtil.MAX_BOARD_POST_LIST_INDEX_SIZE);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPost(jedis, companyId, Collections.singleton(postId), postMap);
			CommunityCache.setPostExt(jedis, companyId, Collections.singleton(postId), postExtMap);
			CommunityCache.setBoardExt(jedis, companyId, Collections.singleton(post.getBoardId()), boardExtMap);
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(DeletePostResponse.newBuilder().setResult(DeletePostResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<LikePostResponse> likePost(RequestHead head, LikePostRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final long userId = head.getSession().getUserId();
		final int postId = request.getPostId();

		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		if (post == null || post.getState() == CommunityProtos.Post.State.DELETE) {
			return Futures.immediateFuture(LikePostResponse.newBuilder()
					.setResult(LikePostResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("帖子不存在")
					.build());
		}

		Map<Integer, CommunityDAOProtos.PostExt> postExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();

			Set<Integer> postLikeIdSet = CommunityDB.getUserPostLikeIdSet(dbConn, companyId, userId, Collections.singleton(postId));
			if (request.getIsLike() == postLikeIdSet.contains(postId)) {
				return Futures.immediateFuture(LikePostResponse.newBuilder().setResult(LikePostResponse.Result.SUCC).build());
			}

			if (request.getIsLike()) {
				CommunityDB.insertUserPostLike(dbConn, companyId, userId, postId, (int) (System.currentTimeMillis() / 1000L));

				CommunityDB.updateBoardHotPostCount(dbConn, companyId, postId, false, false, true);

			} else {
				CommunityDB.deleteUserPostLike(dbConn, companyId, userId, postId);
			}

			postExtMap = CommunityDB.getPostExt(dbConn, companyId, Collections.singleton(postId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPostExt(jedis, companyId, Collections.singleton(postId), postExtMap);
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(LikePostResponse.newBuilder().setResult(LikePostResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<CreateCommentResponse> createComment(final RequestHead head, CreateCommentRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final String content = request.getContent().trim();
		if (content.isEmpty()) {
			return Futures.immediateFuture(CreateCommentResponse.newBuilder()
					.setResult(CreateCommentResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容不能为空")
					.build());
		}
		if (content.length() > 512) {
			return Futures.immediateFuture(CreateCommentResponse.newBuilder()
					.setResult(CreateCommentResponse.Result.FAIL_CONTENT_INVALID)
					.setFailText("评论内容最多512个字")
					.build());
		}

		final int postId = request.getPostId();
		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		if (post == null || post.getState() == CommunityProtos.Post.State.DELETE) {
			return Futures.immediateFuture(CreateCommentResponse.newBuilder()
					.setResult(CreateCommentResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("帖子不存在")
					.build());
		}

		CommunityProtos.Comment.Builder commentBuilder = CommunityProtos.Comment.newBuilder();
		commentBuilder.setPostId(postId);
		commentBuilder.setCommentId(0);
		if (request.hasReplyCommentId()) {
			commentBuilder.setReplyCommentId(request.getReplyCommentId());
		}
		commentBuilder.setContent(content);
		commentBuilder.setCreateUserId(head.getSession().getUserId());
		commentBuilder.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		commentBuilder.setState(CommunityProtos.Comment.State.NORMAL);
		// 初始化like_count和is_like
		commentBuilder.setLikeCount(0);
		commentBuilder.setIsLike(false);

		final CommunityProtos.Comment comment = commentBuilder.build();

		final int commentId;
		final CommunityProtos.Comment replyComment;
		Map<Integer, CommunityDAOProtos.PostExt> postExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();

			if (request.hasReplyCommentId()) {
				replyComment = CommunityDB.getComment(dbConn, companyId, postId, request.getReplyCommentId()); 
				if (replyComment == null) {
					return Futures.immediateFuture(CreateCommentResponse.newBuilder()
							.setResult(CreateCommentResponse.Result.FAIL_REPLY_COMMENT_NOT_EXIST)
							.setFailText("您回复的评论不存在")
							.build());
				}
			} else {
				replyComment = null;
			}

			commentId = CommunityDB.insertComment(dbConn, companyId, comment);

			CommunityDB.updateBoardHotPostCount(dbConn, companyId, postId, false, true, false);

			postExtMap = CommunityDB.getPostExt(dbConn, companyId, Collections.singleton(postId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPostExt(jedis, companyId, Collections.singleton(postId), postExtMap);
		} finally {
			jedis.close();
		}
		
		PushProtos.PushMsgRequest.Builder pushRequestBuilder = PushProtos.PushMsgRequest.newBuilder();
		
		if (post.getCreateUserId() != head.getSession().getUserId()) {
			pushRequestBuilder.addPushPacket(PushProtos.PushPacket.newBuilder()
					.addPushTarget(PushProtos.PushTarget.newBuilder()
							.setUserId(post.getCreateUserId())
							.setEnableOffline(true)
							.build())
					.setPushName("CommunityPostMessagePush")
					.setPushBody(CommunityProtos.CommunityPostMessagePush.newBuilder()
							.setPostId(postId)
							.setCommentId(commentId)
							.build()
							.toByteString())
					.build());
		}
		
		if (replyComment != null 
				&& replyComment.getCreateUserId() != head.getSession().getUserId() 
				&& replyComment.getCreateUserId() != post.getCreateUserId()
				) {
			pushRequestBuilder.addPushPacket(PushProtos.PushPacket.newBuilder()
					.addPushTarget(PushProtos.PushTarget.newBuilder()
							.setUserId(replyComment.getCreateUserId())
							.setEnableOffline(true)
							.build())
					.setPushName("CommunityCommentMessagePush")
					.setPushBody(CommunityProtos.CommunityCommentMessagePush.newBuilder()
							.setPostId(postId)
							.setCommentId(request.getReplyCommentId())
							.setReplyCommentId(commentId)
							.build()
							.toByteString())
					.build());
		}

		if (pushRequestBuilder.getPushPacketCount() > 0) {
			this.pushService.pushMsg(head, pushRequestBuilder.build());
		}

		return Futures.immediateFuture(CreateCommentResponse.newBuilder()
				.setResult(CreateCommentResponse.Result.SUCC)
				.setCommentId(commentId)
				.build());
	}

	@Override
	public ListenableFuture<DeleteCommentResponse> deleteComment(RequestHead head, DeleteCommentRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final int postId = request.getPostId();
		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		if (post == null || post.getState() == CommunityProtos.Post.State.DELETE) {
			return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
					.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXIST)
					.setFailText("评论所在帖子不存在")
					.build());
		}

		final int commentId = request.getCommentId();

		Map<Integer, CommunityDAOProtos.PostExt> postExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();

			CommunityProtos.Comment comment = CommunityDB.getComment(dbConn, companyId, postId, commentId);
			if (comment == null || comment.getState() == CommunityProtos.Comment.State.DELETE) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_NOT_EXIST)
						.setFailText("评论不存在")
						.build());
			}
			if (comment.getCreateUserId() != head.getSession().getUserId()) {
				return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
						.setResult(DeleteCommentResponse.Result.FAIL_COMMENT_OTHER)
						.setFailText("只能删除自己发表的评论")
						.build());
			}

			CommunityDB.updateCommentStateDelete(dbConn, companyId, postId, commentId);

			postExtMap = CommunityDB.getPostExt(dbConn, companyId, Collections.singleton(postId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPostExt(jedis, companyId, Collections.singleton(postId), postExtMap);
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(DeleteCommentResponse.newBuilder().setResult(DeleteCommentResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<GetMyPostListResponse> getMyPostList(RequestHead head, GetMyPostListRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final long userId = head.getSession().getUserId();
		final int size = request.getSize() < 0 ? 0 : request.getSize() > 100 ? 100 : request.getSize();

		final CommunityDAOProtos.PostListIndex currentOffsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityDAOProtos.PostListIndex tmp = null;
			try {
				tmp = CommunityDAOProtos.PostListIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		if (currentOffsetIndex != null && currentOffsetIndex.equals(POST_LIST_INDEX_END)) {
			return Futures.immediateFuture(GetMyPostListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(POST_LIST_INDEX_END.toByteString())
					.build());
		}

		List<CommunityDAOProtos.PostListIndex> list;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			if (currentOffsetIndex == null) {
				list = CommunityDB.getUserPostListIndexList(dbConn, companyId, userId, CommunityUtil.USER_POST_STATE_LIST, size + 1);
			} else {
				list = CommunityDB.getUserPostListIndexList(dbConn, companyId, userId, CommunityUtil.USER_POST_STATE_LIST, currentOffsetIndex, size + 1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		final boolean hasMore;
		final ByteString nextOffsetIndex;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);
			if (list.isEmpty()) {
				nextOffsetIndex = currentOffsetIndex == null ? ByteString.EMPTY : currentOffsetIndex.toByteString();
			} else {
				nextOffsetIndex = list.get(list.size() - 1).toByteString();
			}
		} else {
			hasMore = false;
			nextOffsetIndex = POST_LIST_INDEX_END.toByteString();
		}

		List<Integer> postIdList = new ArrayList<Integer>(list.size());
		for (CommunityDAOProtos.PostListIndex postListIndex : list) {
			postIdList.add(postListIndex.getPostId());
		}

		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				postIdList,
				CommunityUtil.USER_POST_STATE_LIST);

		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());

		return Futures.immediateFuture(GetMyPostListResponse.newBuilder().setHasMore(hasMore).setOffsetIndex(nextOffsetIndex).addAllPost(postList).build());
	}

	private static final CommunityDAOProtos.UserCommentListIndex USER_COMMENT_LIST_INDEX_END = CommunityDAOProtos.UserCommentListIndex.newBuilder()
			.setPostId(0)
			.setCommentId(0)
			.setCreateTime(0)
			.build();
	
	@Override
	public ListenableFuture<GetMyCommentListResponse> getMyCommentList(RequestHead head, GetMyCommentListRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final long userId = head.getSession().getUserId();
		final int size = request.getSize() < 1 ? 1 : request.getSize() > 100 ? 100 : request.getSize();

		final CommunityDAOProtos.UserCommentListIndex currentOffsetIndex;
		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityDAOProtos.UserCommentListIndex tmp = null;
			try {
				tmp = CommunityDAOProtos.UserCommentListIndex.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}

		if (currentOffsetIndex != null && currentOffsetIndex.equals(USER_COMMENT_LIST_INDEX_END)) {
			return Futures.immediateFuture(GetMyCommentListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(USER_COMMENT_LIST_INDEX_END.toByteString())
					.build());
		}

		List<CommunityProtos.Comment> list;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			if (currentOffsetIndex == null) {
				list = CommunityDB.getUserPostCommentList(dbConn, companyId, userId, CommunityUtil.USER_COMMENT_STATE_LIST, size + 1);
			} else {
				list = CommunityDB.getUserPostCommentList(dbConn, companyId, userId, CommunityUtil.USER_COMMENT_STATE_LIST, currentOffsetIndex, size + 1);
			}
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		final boolean hasMore;
		final ByteString nextOffsetIndex;
		if (list.size() > size) {
			hasMore = true;
			list = list.subList(0, size);

			if (list.isEmpty()) {
				nextOffsetIndex = currentOffsetIndex == null ? ByteString.EMPTY : currentOffsetIndex.toByteString();
			} else {
				CommunityProtos.Comment comment = list.get(list.size() - 1);
				nextOffsetIndex = CommunityDAOProtos.UserCommentListIndex.newBuilder()
						.setPostId(comment.getPostId())
						.setCommentId(comment.getCommentId())
						.setCreateTime(comment.getCreateTime())
						.build()
						.toByteString();
			}
		} else {
			hasMore = false;
			nextOffsetIndex = USER_COMMENT_LIST_INDEX_END.toByteString();
		}

		Map<Integer, List<Integer>> postCommentIdMap = new HashMap<Integer, List<Integer>>();
		for (CommunityProtos.Comment comment : list) {
			List<Integer> commentIdList = postCommentIdMap.get(comment.getPostId());
			if (commentIdList == null) {
				commentIdList = new ArrayList<Integer>();
			}
			commentIdList.add(comment.getCommentId());
			postCommentIdMap.put(comment.getPostId(), commentIdList);
		}
		
		Map<Integer, Set<Integer>> userCommentLikeIdSetMap = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>> postCommentExtMap = new HashMap<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>>();
		try {
			dbConn = this.hikariDataSource.getConnection();
			userCommentLikeIdSetMap = CommunityDB.getUserPostCommentLikeIdSet(dbConn, companyId, userId, postCommentIdMap);
			postCommentExtMap = CommunityDB.getPostCommentExt(dbConn, companyId, postCommentIdMap);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool, this.hikariDataSource, companyId, postCommentIdMap.keySet(), CommunityUtil.USER_POST_STATE_LIST);
		
		GetMyCommentListResponse.Builder responseBuilder = GetMyCommentListResponse.newBuilder();
		
		// 给评论添加like_count,is_like值
		CommunityProtos.Comment.Builder tmpCommentBuilder = CommunityProtos.Comment.newBuilder();
		for (CommunityProtos.Comment comment : list) {
			if (!postMap.containsKey(comment.getPostId())) {
				continue;
			}

			int postId = comment.getPostId();
			int commentId = comment.getCommentId();
			tmpCommentBuilder.clear();
			tmpCommentBuilder.mergeFrom(comment);

			Map<Integer, CommunityDAOProtos.PostCommentExt> commentMap = postCommentExtMap.get(postId);
			int likeCount = commentMap != null && commentMap.get(commentId) != null ? commentMap.get(commentId).getLikeCount() : 0;
			tmpCommentBuilder.setLikeCount(likeCount);
			tmpCommentBuilder.setIsLike(userCommentLikeIdSetMap.get(postId) == null ? false : userCommentLikeIdSetMap.get(postId).contains(commentId));
			responseBuilder.addComment(tmpCommentBuilder.build());
		}
	
		// 给post赋comment_count,like_count,is_like的值
		List<Integer> postIdList = new ArrayList<Integer>();
		for (int postId : postMap.keySet()) {
			postIdList.add(postId);
		}
		List<CommunityProtos.Post> refPostList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());
		
		return Futures.immediateFuture(responseBuilder.setHasMore(hasMore).setOffsetIndex(nextOffsetIndex).addAllRefPost(refPostList).build());
	}

	@Override
	public ListenableFuture<CommunityProtos.GetRecommendPostResponse> getRecommendPost(RequestHead head, EmptyRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(jedisPool, hikariDataSource, companyId);
		List<Integer> recommendPostIdList = new ArrayList<Integer>();
		for (CommunityDAOProtos.RecommendPostListIndex recommendPostIndex : communityInfo.getRecommendPostIndexList()) {
			if (recommendPostIndex != null) {
				recommendPostIdList.add(recommendPostIndex.getPostId());
			}
		}

		Map<Integer, CommunityProtos.Post> recommendPostMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, recommendPostIdList, CommunityUtil.ADMIN_POST_STATE_LIST);

		CommunityProtos.GetRecommendPostResponse.Builder responseBuilder = CommunityProtos.GetRecommendPostResponse.newBuilder();
		//添加依赖的版块
		Set<Integer> boardIds = new TreeSet<Integer>();
		for (CommunityProtos.Post post : recommendPostMap.values()) {
			if (null != post) {
				boardIds.add(post.getBoardId());
			}
		}
		responseBuilder.addAllRefBoard(CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, boardIds));

		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> recommendPostList = this.setPostExtValue(companyId, recommendPostIdList, recommendPostMap, head.getSession().getUserId());
		
		if (recommendPostList.size() > CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE) {
			recommendPostList = recommendPostList.subList(0, CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE);
		}
		responseBuilder.addAllPost(recommendPostList);
				
		return Futures.immediateFuture(responseBuilder.build());
	}


	@Override
	public ListenableFuture<CommunityProtos.LikeCommentResponse> likeComment(RequestHead head, CommunityProtos.LikeCommentRequest request) {
		final long companyId = head.getSession().getCompanyId();

		final long userId = head.getSession().getUserId();
		final int postId = request.getPostId();
		final int commentId = request.getCommentId();

		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		if (post == null || post.getState() == CommunityProtos.Post.State.DELETE) {
			return Futures.immediateFuture(LikeCommentResponse.newBuilder()
					.setResult(LikeCommentResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("帖子不存在")
					.build());
		}

		
		Map<Integer, CommunityDAOProtos.PostExt> postExtMap;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();

			CommunityProtos.Comment comment = CommunityDB.getComment(dbConn, companyId, postId, commentId);
			if (comment == null || !CommunityUtil.USER_COMMENT_STATE_LIST.contains(comment.getState())) {
				return Futures.immediateFuture(LikeCommentResponse.newBuilder()
						.setResult(LikeCommentResponse.Result.FAIL_COMMENT_NOT_EXIST)
						.setFailText("帖子评论不存在")
						.build());
			}
			
			if (request.getIsLike()) {
				CommunityDB.insertUserCommentLike(dbConn, companyId, userId, postId, commentId, (int) (System.currentTimeMillis() / 1000L));

			} else {
				CommunityDB.deleteUserCommentLike(dbConn, companyId, userId, postId, commentId);
			}

			postExtMap = CommunityDB.getPostExt(dbConn, companyId, Collections.singleton(postId));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// update cache
		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.setPostExt(jedis, companyId, Collections.singleton(postId), postExtMap);
		} finally {
			jedis.close();
		}

		return Futures.immediateFuture(LikeCommentResponse.newBuilder().setResult(LikeCommentResponse.Result.SUCC).build());
	}
	
	
	/**
	 * 给post赋comment_count,like_count,is_like的值,标签列表
	 * 
	 * @param recommendPostIdList
	 * @param recommendPostMap
	 * @param userId
	 * @return
	 */
	private List<CommunityProtos.Post> setPostExtValue(long companyId, List<Integer> postIdList,Map<Integer, CommunityProtos.Post> postMap,long userId){
		
		List<CommunityProtos.Post> recommendPostList = new ArrayList<CommunityProtos.Post>();
		Set<Integer> userPostLikeIdSet;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			userPostLikeIdSet = CommunityDB.getUserPostLikeIdSet(dbConn, companyId, userId, postMap.keySet());
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		Map<Integer, CommunityDAOProtos.PostExt> postExtMap = CommunityUtil.doGetPostExt(jedisPool, hikariDataSource, companyId, postMap.keySet());
		CommunityProtos.Post.Builder tmpPostBuilder = CommunityProtos.Post.newBuilder();
		for (Integer postId : postIdList) {
			CommunityProtos.Post post = postMap.get(postId);
			if (post == null) {
				continue;
			}
			
			tmpPostBuilder.clear();
			tmpPostBuilder.mergeFrom(post);
			
			CommunityDAOProtos.PostExt postExt = postExtMap.get(postId);
			if (postExt != null) {
				
				int commentCount = 0;
				for (CommunityDAOProtos.CommentCount cnt : postExt.getCommentCountList()) {
					if (CommunityUtil.USER_COMMENT_STATE_LIST.contains(cnt.getState())) {
						commentCount += cnt.getCount();
					}
				}
				
				tmpPostBuilder.setCommentCount(commentCount);
				tmpPostBuilder.setLikeCount(postExt.getLikeCount());
				tmpPostBuilder.addAllTag(postExt.getTagList());
			}
			
			tmpPostBuilder.setIsLike(userPostLikeIdSet.contains(postId));
			
			// 添加热门评论
//			tmpPostBuilder.a
			recommendPostList.add(tmpPostBuilder.build());
		}
		return recommendPostList;
	}
	
	/**
	 * 给评论赋is_like,like_count的值
	 * 
	 * @param postId
	 * @param commentList
	 * @param userId
	 * @return
	 */
	private List<CommunityProtos.Comment> setPostCommentExtValue(long companyId, int postId, List<CommunityProtos.Comment> commentList, long userId){
		
		List<Integer> commentIdList = new ArrayList<Integer>();
		for (CommunityProtos.Comment comment : commentList) {
			if (comment != null) {
				commentIdList.add(comment.getCommentId());
			}
		}
		
		Set<Integer> userCommentLikeIdSet = new TreeSet<Integer>();
		Map<Integer, CommunityDAOProtos.PostCommentExt> postCommentExtMap = new HashMap<Integer, CommunityDAOProtos.PostCommentExt>();
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			userCommentLikeIdSet = CommunityDB.getUserPostCommentLikeIdSet(dbConn, companyId, userId, Collections.singletonMap(postId, commentIdList)).get(postId);
			postCommentExtMap = CommunityDB.getPostCommentExt(dbConn, companyId, Collections.singletonMap(postId, commentIdList)).get(postId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 给评论赋is_like和like_count的值
		List<CommunityProtos.Comment> resultCommentList = new ArrayList<CommunityProtos.Comment>();
		CommunityProtos.Comment.Builder tmpCommentBuilder = CommunityProtos.Comment.newBuilder();
		for (CommunityProtos.Comment comment : commentList) {
			if (comment == null) {
				continue;
			}
			int commentId = comment.getCommentId();

			tmpCommentBuilder.clear();
			tmpCommentBuilder.mergeFrom(comment);
			tmpCommentBuilder.setIsLike(userCommentLikeIdSet == null ? false : userCommentLikeIdSet.contains(commentId));

			tmpCommentBuilder.setLikeCount((postCommentExtMap ==null || postCommentExtMap.get(commentId) == null) ? 0 : postCommentExtMap.get(commentId).getLikeCount());

			resultCommentList.add(tmpCommentBuilder.build());
		}
		return resultCommentList;
	}

	/**
	 * 添加置顶帖后为兼容老版本，新建接口对应新版本
	 * 
	 * @param head
	 * @param request
	 * @return
	 */
	@Override
	public ListenableFuture<GetPostListResponse> getPostListV2(RequestHead head, GetPostListRequest request) {
		switch (request.getListType()) {
			case CREATE_TIME:
				return Futures.immediateFuture(this.doGetPostListCreateTimeSticky(head, request));
			case TOP_HOT:
				return Futures.immediateFuture(this.doGetPostListTopHot(head, request));
			default:
				return Futures.immediateFuture(GetPostListResponse.newBuilder().setHasMore(false).setOffsetIndex(ByteString.EMPTY).build());
		}
	}

	@Override
	public ListenableFuture<CommunityProtos.GetPostCommentByIdResponse> getPostCommentById(RequestHead head, CommunityProtos.GetPostCommentByIdRequest request) {

		final long companyId = head.getSession().getCompanyId();
		
		List<CommunityProtos.GetPostCommentByIdRequest.PostCommentId> postCommentIds = request.getPostCommentIdList();
		if(postCommentIds.isEmpty()){
			return Futures.immediateFuture(GetPostCommentByIdResponse.newBuilder().build());
		}
		
		
		Set<Integer> postIdSet = new TreeSet<Integer>();
		for(CommunityProtos.GetPostCommentByIdRequest.PostCommentId postCommentId : postCommentIds){
			if(postCommentId!=null){
				postIdSet.add(postCommentId.getPostId());
			}	
		}
		
		List<Integer> postIdList = new ArrayList<Integer>();
		postIdList.addAll(postIdSet);
		
		List<CommunityProtos.Comment> commentList = new ArrayList<CommunityProtos.Comment>();
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			commentList = CommunityDB.getPostCommentListById(dbConn, companyId, postCommentIds, CommunityUtil.USER_COMMENT_STATE_LIST);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		CommunityProtos.GetPostCommentByIdResponse.Builder response = CommunityProtos.GetPostCommentByIdResponse.newBuilder();
		List<CommunityProtos.Comment> commentListNew = new ArrayList<CommunityProtos.Comment> ();
		for(CommunityProtos.Comment comment : commentList){
			commentListNew.addAll(this.setPostCommentExtValue(companyId, comment.getPostId(), Collections.singletonList(comment), head.getSession().getUserId()));
		}
		
		response.addAllComment(commentListNew);
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, postIdList, CommunityUtil.USER_POST_STATE_LIST);
		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIdList, postMap, head.getSession().getUserId());
		
		
		response.addAllRefPost(postList);
		
		return Futures.immediateFuture(response.build());
	}

	@Override
	public ListenableFuture<GetHotCommentListResponse> getHotCommentList(RequestHead head, GetHotCommentListRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final int postId = request.getPostId();

		List<CommunityProtos.Comment> commentList = null;
		Connection dbConn = null;
		try {
			dbConn = this.hikariDataSource.getConnection();
			commentList = CommunityDB.getHotCommentList(dbConn, companyId, Collections.singleton(postId), POST_HOT_COMMENT_MAX_NUM, CommunityUtil.USER_COMMENT_STATE_LIST).get(postId);
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (commentList == null) {
			return Futures.immediateFuture(GetHotCommentListResponse.newBuilder().build());
		}
		// 给评论赋is_like和like_count的值
		List<CommunityProtos.Comment> resultCommentList = this.setPostCommentExtValue(companyId, postId, commentList, head.getSession().getUserId());

		return Futures.immediateFuture(GetHotCommentListResponse.newBuilder().addAllComment(resultCommentList).build());
	}

	@Override
	public ListenableFuture<GetPostByIdsResponse> getPostByIds(RequestHead head, GetPostByIdsRequest request) {
		final long companyId = head.getSession().getCompanyId();
		final List<Integer> postIds = request.getPostIdList();
		
		if(postIds.isEmpty()){
			return Futures.immediateFuture(GetPostByIdsResponse.newBuilder().build());
		}
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(this.jedisPool, this.hikariDataSource, companyId, postIds, CommunityUtil.USER_POST_STATE_LIST);
		
		// 给post赋comment_count,like_count,is_like的值
		List<CommunityProtos.Post> postList = this.setPostExtValue(companyId, postIds, postMap, head.getSession().getUserId());

		return Futures.immediateFuture(GetPostByIdsResponse.newBuilder().addAllPost(postList).build());
	}


}
