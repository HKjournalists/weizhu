package com.weizhu.service.community;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardResponse;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.CreateCommentRequest;
import com.weizhu.proto.AdminCommunityProtos.CreateCommentResponse;
import com.weizhu.proto.AdminCommunityProtos.CreatePostRequest;
import com.weizhu.proto.AdminCommunityProtos.CreatePostResponse;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardResponse;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.DeleteCommentRequest;
import com.weizhu.proto.AdminCommunityProtos.DeleteCommentResponse;
import com.weizhu.proto.AdminCommunityProtos.DeletePostRequest;
import com.weizhu.proto.AdminCommunityProtos.DeletePostResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportCommentListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportCommentListResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportPostLikeListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportPostLikeListResponse;
import com.weizhu.proto.AdminCommunityProtos.ExportPostListRequest;
import com.weizhu.proto.AdminCommunityProtos.ExportPostListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetBoardTagRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardTagResponse;
import com.weizhu.proto.AdminCommunityProtos.GetCommentListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetCommentListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetCommunityResponse;
import com.weizhu.proto.AdminCommunityProtos.GetPostListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetPostListResponse;
import com.weizhu.proto.AdminCommunityProtos.GetRecommendPostResponse;
import com.weizhu.proto.AdminCommunityProtos.MigratePostRequest;
import com.weizhu.proto.AdminCommunityProtos.MigratePostResponse;
import com.weizhu.proto.AdminCommunityProtos.RecommendPostRequest;
import com.weizhu.proto.AdminCommunityProtos.RecommendPostResponse;
import com.weizhu.proto.AdminCommunityProtos.SetCommunityRequest;
import com.weizhu.proto.AdminCommunityProtos.SetCommunityResponse;
import com.weizhu.proto.AdminCommunityProtos.SetStickyPostRequest;
import com.weizhu.proto.AdminCommunityProtos.SetStickyPostResponse;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardOrderRequest;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardOrderResponse;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardRequest;
import com.weizhu.proto.AdminCommunityProtos.UpdateBoardResponse;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.PushProtos;
import com.weizhu.proto.PushService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos.Post;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.zaxxer.hikari.HikariDataSource;

public class AdminCommunityServiceImpl implements AdminCommunityService {

	private static final Logger logger = LoggerFactory.getLogger(AdminCommunityServiceImpl.class);

	private final HikariDataSource hikariDataSource;
	private final JedisPool jedisPool;
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final PushService pushService;
	private final AdminUserService adminUserService;
	private final AdminOfficialService adminOfficialService;
	@Inject
	public AdminCommunityServiceImpl(HikariDataSource hikariDataSource, JedisPool jedisPool, @Named("service_executor") Executor serviceExecutor,
			PushService pushService, AdminUserService adminUserService, AdminOfficialService adminOfficialService) {
		this.hikariDataSource = hikariDataSource;
		this.jedisPool = jedisPool;
		this.serviceExecutor = serviceExecutor;
		this.pushService = pushService;
		this.adminUserService = adminUserService;
		this.adminOfficialService = adminOfficialService;
	}

	@Override
	public ListenableFuture<GetCommunityResponse> getCommunity(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCommunityResponse.newBuilder().setCommunityName("").build());
		}
		return Futures.immediateFuture(GetCommunityResponse.newBuilder()
				.setCommunityName(CommunityUtil.doGetCommunityInfo(jedisPool, hikariDataSource, head.getCompanyId()).getCommunityName())
				.build());
	}

	@Override
	public ListenableFuture<SetCommunityResponse> setCommunity(AdminHead head, SetCommunityRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetCommunityResponse.newBuilder()
					.setResult(SetCommunityResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		String communityName = request.hasCommunityName() ? request.getCommunityName() : null;

		if (null == communityName || communityName.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.SetCommunityResponse.newBuilder()
					.setResult(AdminCommunityProtos.SetCommunityResponse.Result.FAIL_COMMUNITY_NAME_INVALID)
					.setFailText("社区名称不能为空！")
					.build());
		}
		if (communityName.length() > 10) {
			return Futures.immediateFuture(AdminCommunityProtos.SetCommunityResponse.newBuilder()
					.setResult(AdminCommunityProtos.SetCommunityResponse.Result.FAIL_COMMUNITY_NAME_INVALID)
					.setFailText("社区名称不能超过10个字符！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.setCommunity(dbConn, companyId, communityName, null);
		} catch (SQLException e) {
			throw new RuntimeException("保存版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		logger.info("set Community end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.SetCommunityResponse.newBuilder()
				.setResult(AdminCommunityProtos.SetCommunityResponse.Result.SUCC)
				.build());

	}

	@Override
	public ListenableFuture<UpdateBoardOrderResponse> updateBoardOrder(AdminHead head, UpdateBoardOrderRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateBoardOrderResponse.newBuilder()
					.setResult(UpdateBoardOrderResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		String boardIdOrderStr = request.getBoardIdOrderStr();

		if (boardIdOrderStr.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.UpdateBoardOrderResponse.newBuilder()
					.setResult(AdminCommunityProtos.UpdateBoardOrderResponse.Result.FAIL_BOARD_ID_ORDER_STR_INVALID)
					.setFailText("版块ID序列不能为空！")
					.build());
		}

		if (boardIdOrderStr.length() > 65535) { // 2的16次方-1
			return Futures.immediateFuture(AdminCommunityProtos.UpdateBoardOrderResponse.newBuilder()
					.setResult(AdminCommunityProtos.UpdateBoardOrderResponse.Result.FAIL_BOARD_ID_ORDER_STR_INVALID)
					.setFailText("版块ID序列不能超过65535个字符！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.setCommunity(dbConn, companyId, null, boardIdOrderStr);
		} catch (SQLException e) {
			throw new RuntimeException("保存版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}

		logger.info("set Community end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.UpdateBoardOrderResponse.newBuilder()
				.setResult(AdminCommunityProtos.UpdateBoardOrderResponse.Result.SUCC)
				.build());

	}

	@Override
	public ListenableFuture<GetBoardListResponse> getBoardList(AdminHead head, GetBoardListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetBoardListResponse.newBuilder().build());
		}
		final long comapnyId = head.getCompanyId();
		final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(jedisPool, hikariDataSource, comapnyId);
		AdminCommunityProtos.GetBoardListResponse.Builder responseBuilder = AdminCommunityProtos.GetBoardListResponse.newBuilder();

		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
		Set<Integer> leafBoardIdSet = new TreeSet<Integer>();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			boardMap.put(board.getBoardId(), board);
			if (board.getIsLeafBoard()) {
				leafBoardIdSet.add(board.getBoardId());
			}
		}

		Map<Integer, CommunityDAOProtos.BoardExt> boardExtMap = CommunityUtil.doGetBoardExt(jedisPool, hikariDataSource, comapnyId, leafBoardIdSet);

		Map<Integer, Integer> postTotalCountMap = new HashMap<Integer, Integer>();

		for (Entry<Integer, CommunityDAOProtos.BoardExt> entry : boardExtMap.entrySet()) {
			CommunityProtos.Board board = boardMap.get(entry.getKey());
			if (board == null) {
				continue;
			}

			int totalCnt = 0;
			for (CommunityDAOProtos.PostCount cnt : entry.getValue().getCountList()) {
				if (CommunityUtil.ADMIN_POST_STATE_LIST.contains(cnt.getState())) {
					totalCnt += cnt.getCount();
				}
			}

			postTotalCountMap.put(entry.getKey(), totalCnt);

			while (board.hasParentBoardId()) {
				board = boardMap.get(board.getParentBoardId());
				if (board == null) {
					// error
					break;
				}

				Integer oldTotalCnt = postTotalCountMap.get(board.getBoardId());
				postTotalCountMap.put(board.getBoardId(), oldTotalCnt == null ? totalCnt : oldTotalCnt + totalCnt);
			}
		}

		CommunityProtos.Board.Builder tmpBoardBuilder = CommunityProtos.Board.newBuilder();
		for (CommunityProtos.Board board : communityInfo.getBoardList()) {
			Integer totalCnt = postTotalCountMap.get(board.getBoardId());
			CommunityDAOProtos.BoardExt boardExt = boardExtMap.get(board.getBoardId());

			if ((totalCnt == null || totalCnt == 0) && boardExt == null) {
				responseBuilder.addBoard(board);
			} else {
				tmpBoardBuilder.clear();

				tmpBoardBuilder.mergeFrom(board);
				tmpBoardBuilder.setPostTotalCount(totalCnt == null ? 0 : totalCnt);
				if (boardExt != null && boardExt.getTagCount() > 0) {
					tmpBoardBuilder.addAllTag(boardExt.getTagList());
				}
				responseBuilder.addBoard(tmpBoardBuilder.build());
			}

		}
		if (logger.isDebugEnabled()) {
			logger.info("get BoardList end , adminId:" + head.getSession().getAdminId());
		}
		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<CreateBoardResponse> createBoard(AdminHead head, CreateBoardRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateBoardResponse.newBuilder()
					.setResult(CreateBoardResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		String boardName = request.getBoardName();
		String boardIcon = request.getBoardIcon();
		String boardDesc = request.getBoardDesc();
		Integer parentBoardId = request.hasParentBoardId() ? request.getParentBoardId() : null;
		Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		AdminCommunityProtos.CreateBoardResponse.Builder responseBuilder = AdminCommunityProtos.CreateBoardResponse.newBuilder();

		if (boardName.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_NAME_INVALID)
					.setFailText("版块名称不能为空！")
					.build());
		} else if (boardName.length() > 10) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_NAME_INVALID)
					.setFailText("版块名称不能超过10个字符！")
					.build());
		}

		if (boardIcon.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_ICON_INVALID)
					.setFailText("板块图标名称不能为空！")
					.build());
		} else if (boardIcon.length() > 191) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_ICON_INVALID)
					.setFailText("板块图标名称不能超过191个字符！")
					.build());
		}

		if (boardDesc.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_DESC_INVALID)
					.setFailText("板块介绍描述不能为空！")
					.build());
		} else if (boardDesc.length() > 10) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.FAIL_BOARD_DESC_INVALID)
					.setFailText("板块介绍描述不能超过10个字符！")
					.build());
		}
		
		List<Integer> postIds = new ArrayList<Integer>();
		Connection dbConn = null;
		Integer boardIdNew = null;
		try {
			dbConn = hikariDataSource.getConnection();

			// 获取子板块id集合
			Set<Integer> childrenBoardIds = new TreeSet<Integer>();
			
			if (parentBoardId != null) {
				childrenBoardIds = CommunityDB.getChildrenBoardId(dbConn, companyId, Collections.singleton(parentBoardId)).get(parentBoardId);
				if (childrenBoardIds == null) {
					childrenBoardIds = Collections.emptySet();
				}
				Integer lastPostId = null;
				while (true) {
					
					lastPostId = postIds.size() == 0 ? null : postIds.get(postIds.size() - 1);
					List<Integer> tmpPostIds = CommunityDB.getBoardPostIdListByLastId(dbConn, companyId, parentBoardId, null, lastPostId, 1000, CommunityUtil.POST_ALL_STATE_LIST);
					if (tmpPostIds.isEmpty()) {
						break;
					}
					postIds.addAll(tmpPostIds);
				}
			}
			boardIdNew = CommunityDB.insertBoard(dbConn, companyId, boardName, boardIcon, boardDesc, parentBoardId, true, false, allowModelId);
			// 更新其父节点状态
			if (parentBoardId != null && childrenBoardIds.isEmpty()) {
				CommunityDB.updateBoard(dbConn, companyId, parentBoardId, null, null, null, false, null);
				if (!postIds.isEmpty()) {
					CommunityDB.updatePostBoard(dbConn, companyId, postIds, boardIdNew);
				}	
			}

		} catch (SQLException e) {
			throw new RuntimeException("插入新版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		// 子节点和父节点缓存都需要更新
		List<Integer> boardIds = new ArrayList<Integer>();
		boardIds.add(boardIdNew);
		if (parentBoardId != null) {
			boardIds.add(parentBoardId);
		}
		
		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
			CommunityCache.delBoardExt(jedis, companyId, boardIds);
			CommunityCache.delPost(jedis, companyId, postIds);
		} finally {
			jedis.close();
		}

		logger.info("create Board end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.CreateBoardResponse.Result.SUCC).setBoadId(boardIdNew).build());

	}

	@Override
	public ListenableFuture<UpdateBoardResponse> updateBoard(AdminHead head, UpdateBoardRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(UpdateBoardResponse.newBuilder()
					.setResult(UpdateBoardResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		
		final long companyId = head.getCompanyId();
		String boardName = request.getBoardName();
		String boardIcon = request.getBoardIcon();
		String boardDesc = request.getBoardDesc();
		int boardId = request.getBoardId();
		Integer allowModelId = request.hasAllowModelId() ? request.getAllowModelId() : null;

		AdminCommunityProtos.UpdateBoardResponse.Builder responseBuilder = AdminCommunityProtos.UpdateBoardResponse.newBuilder();

		if (boardName.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_NAME_INVALID)
					.setFailText("版块名称不能为空！")
					.build());
		} else if (boardName.length() > 10) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_NAME_INVALID)
					.setFailText("版块名称不能超过10个字符！")
					.build());
		}

		if (boardIcon.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_ICON_INVALID)
					.setFailText("板块图标名称不能为空！")
					.build());
		} else if (boardIcon.length() > 191) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_ICON_INVALID)
					.setFailText("板块图标名称不能超过191个字符！")
					.build());
		}

		if (boardDesc.isEmpty()) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_DESC_INVALID)
					.setFailText("板块介绍描述不能为空！")
					.build());
		} else if (boardDesc.length() > 10) {
			return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.FAIL_BOARD_DESC_INVALID)
					.setFailText("板块介绍描述不能超过10个字符！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.updateBoard(dbConn, companyId, boardId, boardName, boardIcon, boardDesc, null, allowModelId);

		} catch (SQLException e) {
			throw new RuntimeException("更新版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(request.getBoardId()));
		} finally {
			jedis.close();
		}

		logger.info("update Board end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(responseBuilder.setResult(AdminCommunityProtos.UpdateBoardResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<DeleteBoardResponse> deleteBoard(AdminHead head, DeleteBoardRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteBoardResponse.newBuilder()
					.setResult(DeleteBoardResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		Connection dbConn = null;
		List<Integer> postIds = null;
		int boardId = request.getBoardId();
		Boolean isForceDelete = request.hasIsForceDelete() ? request.getIsForceDelete() : null;
		CommunityProtos.Board board = null;
		List<CommunityProtos.Board> boardList = CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId));
		if (boardList == null || boardList.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.DeleteBoardResponse.newBuilder()
					.setResult(AdminCommunityProtos.DeleteBoardResponse.Result.FAIL_BOARD_NOT_EXIST)
					.setFailText("该版块不存在!")
					.build());
		} else {
			board = boardList.get(0);
		}

		try {
			dbConn = hikariDataSource.getConnection();
			Set<Integer> boardIds = new TreeSet<Integer>();
			boardIds.add(request.getBoardId());
			if (board.hasParentBoardId()) {
				boardIds.add(board.getParentBoardId());
			}
			Map<Integer, Set<Integer>> parentBoardIdChildrenBoardIdsMap = CommunityDB.getChildrenBoardId(dbConn, companyId, boardIds);
			
			if (parentBoardIdChildrenBoardIdsMap.get(request.getBoardId()) != null) {
				return Futures.immediateFuture(AdminCommunityProtos.DeleteBoardResponse.newBuilder()
						.setResult(AdminCommunityProtos.DeleteBoardResponse.Result.FAIL_BOARD_EXIST_CHILDREN_BOARD)
						.setFailText("版块下有子板块，不能删除!")
						.build());
			}
			
			Set<Integer> parentChildrenBoardIds;
			if (board.hasParentBoardId()) {
				parentChildrenBoardIds = parentBoardIdChildrenBoardIdsMap.get(board.getParentBoardId());
				if (parentChildrenBoardIds == null) {
					parentChildrenBoardIds = Collections.emptySet();
				}
			} else {
				parentChildrenBoardIds = Collections.emptySet();
			}
			// 获取该版块下的所有帖子id,在板块下有帖子的情况下若is_force_delete参数为true，则强制删除，否则不能删除
			postIds = CommunityDB.getBoardPostIdListByLastId(dbConn, companyId, boardId, null, null, null, Collections.<CommunityProtos.Post.State> emptyList());
			if (!postIds.isEmpty() && (null == isForceDelete || isForceDelete == false)) {
				return Futures.immediateFuture(AdminCommunityProtos.DeleteBoardResponse.newBuilder()
						.setResult(AdminCommunityProtos.DeleteBoardResponse.Result.FAIL_BOARD_EXIST_POST)
						.setFailText("版块下有帖子，不能删除!")
						.build());
			}
			// 删除版块，并将其下所有帖子和评论的状态变为删除状态和版块下标签
			CommunityDB.deleteBoard(dbConn, companyId, boardId);
			// 更新其父节点状态
			if (board.hasParentBoardId() && !parentChildrenBoardIds.isEmpty()) {
				CommunityDB.updateBoard(dbConn, companyId, board.getParentBoardId(), null, null, null, parentChildrenBoardIds.size() == 1
						&& parentChildrenBoardIds.contains(boardId), null);
			}
		} catch (SQLException e) {

			throw new RuntimeException("删除版块信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
			CommunityCache.delPost(jedis, companyId, postIds);
			CommunityCache.delPostExt(jedis, companyId, postIds);
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(request.getBoardId()));
			CommunityCache.delBoardHotPostList(jedis, companyId, Collections.singleton(request.getBoardId()));

			// 父节点的BoardExt缓存不用更新
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(boardId));
		} finally {
			jedis.close();
		}

		logger.info("delete Board end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.DeleteBoardResponse.newBuilder()
				.setResult(AdminCommunityProtos.DeleteBoardResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetPostListResponse> getPostList(AdminHead head, GetPostListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetPostListResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		DataPage<Integer> postIdDataPage = this.getPostIdList(companyId, request);

		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, postIdDataPage.dataList(), CommunityUtil.ADMIN_POST_STATE_LIST);

		AdminCommunityProtos.GetPostListResponse.Builder responseBuilder = AdminCommunityProtos.GetPostListResponse.newBuilder();

		//添加依赖的版块
		Set<Integer> boardIds = new TreeSet<Integer>();
		for (CommunityProtos.Post post : postMap.values()) {
			if (null != post) {
				boardIds.add(post.getBoardId());
			}
		}
		responseBuilder.addAllRefBoard(CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, boardIds));

		// 给post赋comment_count,like_count的值
		responseBuilder.addAllPost(this.setPostExtValue(companyId, postMap, postIdDataPage.dataList()));

		logger.info("get PostList end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(responseBuilder.setTotalSize(postIdDataPage.totalSize())
				.setFilteredSize(postIdDataPage.filteredSize())
				.build());
	}
	
	
	private DataPage<Integer> getPostIdList(final long companyId, AdminCommunityProtos.GetPostListRequest request) {
		Integer boardId = request.hasBoardId() ? request.getBoardId() : null;
		String postTitle = request.hasPostTitle() && !request.getPostTitle().isEmpty() ? request.getPostTitle() : null;
		Integer start = request.hasStart() ? request.getStart() < 0 ? 0 : request.getStart() : null;
		int length = request.getLength() < 0 ? 0 : request.getLength();
		List<Long> createUserIds = request.getCreateUserIdList();
		
		// 当板块id不为空且帖子标题为空时，置顶贴置顶，否则所有帖子按创建时间倒序排列，置顶贴不置顶
		boolean isSticky = boardId != null && postTitle == null && createUserIds.isEmpty();
		
		// 若不用置顶显示置顶贴，则直接查询并返回
		if (!isSticky) {
			Connection dbConn = null;
			try {
				dbConn = hikariDataSource.getConnection();
				return CommunityDB.getBoardPostIdListByStart(dbConn, companyId, boardId, postTitle, start, length, CommunityUtil.ADMIN_POST_STATE_LIST, createUserIds, isSticky);
				
			} catch (SQLException e) {
				throw new RuntimeException("获取帖子id列表失败！", e);
			} finally {
				DBUtil.closeQuietly(dbConn);
			}
			
		}
		
		// 需要置顶显示置顶贴，则需要进行帖子id的拼接
		
		CommunityDAOProtos.BoardExt boardExt = CommunityUtil.doGetBoardExt(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId)).get(boardId);
		List<Integer> stickyPostIdList = new ArrayList<Integer>();
		if (boardExt != null) {
			for (CommunityDAOProtos.PostListIndex stickyPostListIndex : boardExt.getStickyPostIndexList()) {
				if (stickyPostListIndex == null) {
					continue;
				}

				if (CommunityUtil.USER_POST_STATE_LIST.contains(stickyPostListIndex.getState())) {
					stickyPostIdList.add(stickyPostListIndex.getPostId());
				}
			}
		}

		// 添加置顶贴
		List<Integer> postIds = new ArrayList<Integer>();
		Iterator<Integer> stickyPIdIterator = stickyPostIdList.iterator();
		Integer startNew = null;
		if(start == null || start < stickyPostIdList.size()){
			for(int i = 0; postIds.size() < length && stickyPIdIterator.hasNext(); i++){
				if(start != null && i <= start){
					continue;
				}
				postIds.add(stickyPIdIterator.next());
			}
			
			startNew = null;
		}else{
			startNew = start-stickyPostIdList.size();
		}

		int lengthNew = length <= postIds.size() ? 0 : length - postIds.size();
		
		
		// 添加创建时间倒序的普通贴
		DataPage<Integer> postIdDataPage = null;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			postIdDataPage = CommunityDB.getBoardPostIdListByStart(dbConn, companyId, boardId, postTitle, startNew, lengthNew, CommunityUtil.ADMIN_POST_STATE_LIST, createUserIds, isSticky);
		} catch (SQLException e) {
			throw new RuntimeException("DB FAILED!");
		}finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		Iterator<Integer> commonPIdIterator = postIdDataPage.dataList().iterator();
		while(postIds.size() < length && commonPIdIterator.hasNext()){
			postIds.add(commonPIdIterator.next());
		}
		
		
		return new DataPage<Integer>(postIds, postIdDataPage.totalSize(), postIdDataPage.filteredSize());
	}

	@Override
	public ListenableFuture<ExportPostListResponse> exportPostList(AdminHead head, ExportPostListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ExportPostListResponse.newBuilder()
					.setHasMore(false)
					.build());
		}
		final long companyId = head.getCompanyId();

		Integer boardId = request.hasBoardId() ? request.getBoardId() : null;
		String postTitle = request.hasPostTitle() ? request.getPostTitle() : null;
		Integer lastPostId = request.hasLastPostId() ? request.getLastPostId() : null;
		int size = request.getSize();

		AdminCommunityProtos.ExportPostListResponse.Builder responseBuilder = AdminCommunityProtos.ExportPostListResponse.newBuilder();
		Connection dbConn = null;
		List<Integer> postIds = null;
		try {
			dbConn = hikariDataSource.getConnection();
			postIds = CommunityDB.getBoardPostIdListByLastId(dbConn, companyId, boardId, postTitle, lastPostId, size + 1, CommunityUtil.ADMIN_POST_STATE_LIST);

		} catch (SQLException e) {
			throw new RuntimeException("获取帖子id列表失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		if (postIds.size() > size) {
			responseBuilder.setHasMore(true);
			postIds = postIds.subList(0, size);
		} else {
			responseBuilder.setHasMore(false);
		}

		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, postIds, CommunityUtil.ADMIN_POST_STATE_LIST);

		//添加依赖的版块
		Set<Integer> boardIds = new TreeSet<Integer>();
		for (CommunityProtos.Post post : postMap.values()) {
			if (null != post) {
				boardIds.add(post.getBoardId());
			}
		}
		responseBuilder.addAllRefBoard(CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, boardIds));

		// 给post赋comment_count,like_count的值
		responseBuilder.addAllPost(this.setPostExtValue(companyId, postMap, postIds));

		logger.info("export PostList end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(responseBuilder.build());
	}

	@Override
	public ListenableFuture<DeletePostResponse> deletePost(AdminHead head, DeletePostRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeletePostResponse.newBuilder()
					.setResult(DeletePostResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		List<Integer> postIdList = request.getPostIdList();
		
		if (postIdList.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.DeletePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.DeletePostResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("帖子id不能为空！")
					.build());
		}
		
		Map<Integer, CommunityProtos.Post> postMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, request.getPostIdList(), CommunityUtil.ADMIN_POST_STATE_LIST);

		if (postMap.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.DeletePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.DeletePostResponse.Result.SUCC)
					.build());
		}

		Set<Integer> boardIds = new TreeSet<Integer>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			boardIds = CommunityDB.getBoardIdsByPostIds(dbConn, companyId, postIdList);
			CommunityDB.updatePostStateDelete(dbConn, companyId, postIdList);
		} catch (SQLException e) {
			throw new RuntimeException("删除帖子失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delPost(jedis, companyId, postIdList);
			CommunityCache.delPostExt(jedis, companyId, postIdList);
			CommunityCache.delBoardExt(jedis, companyId, boardIds);
			CommunityCache.delBoardHotPostList(jedis, companyId, boardIds);
		} finally {
			jedis.close();
		}

		logger.info("delete Post end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.DeletePostResponse.newBuilder()
				.setResult(AdminCommunityProtos.DeletePostResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<MigratePostResponse> migratePost(AdminHead head, MigratePostRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(MigratePostResponse.newBuilder()
					.setResult(MigratePostResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		List<Integer> postIdList = request.getPostIdList();
		int boardId = request.getBoardId();

		if (postIdList.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.MigratePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.MigratePostResponse.Result.FAIL_POST_INVALID)
					.setFailText("帖子ID列表不能为空！")
					.build());
		}
		if (null == CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId))) {
			return Futures.immediateFuture(AdminCommunityProtos.MigratePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.MigratePostResponse.Result.FAIL_BOARD_NOT_EXIST)
					.setFailText("该板块不存在！")
					.build());
		}
		List<CommunityProtos.Board> boardList = CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId));
		if (boardList == null || boardList.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.MigratePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.MigratePostResponse.Result.FAIL_BOARD_NOT_EXIST)
					.setFailText("板块ID不存在！")
					.build());
		}
		
		Map<Integer, CommunityProtos.Post> postInfoMap = CommunityUtil.doGetPost(jedisPool,
				hikariDataSource,
				companyId,
				request.getPostIdList(),
				CommunityUtil.ADMIN_POST_STATE_LIST);
		if (postInfoMap.isEmpty()) {
			return Futures.immediateFuture(AdminCommunityProtos.MigratePostResponse.newBuilder()
					.setResult(AdminCommunityProtos.MigratePostResponse.Result.FAIL_POST_INVALID)
					.setFailText("帖子ID不存在！")
					.build());
		}

		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.updatePostBoard(dbConn, companyId, postIdList, boardId);
		} catch (SQLException e) {
			throw new RuntimeException("迁移帖子失败！");
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Set<Integer> boardIds = new TreeSet<Integer>();

		for (CommunityProtos.Post post : postInfoMap.values()) {
			boardIds.add(post.getBoardId());
		}
		boardIds.add(request.getBoardId());

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delPost(jedis, companyId, postIdList);
			CommunityCache.delPostExt(jedis, companyId, postIdList);
			CommunityCache.delBoardExt(jedis, companyId, boardIds);
		} finally {
			jedis.close();
		}

		logger.info("migrate Post end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.MigratePostResponse.newBuilder()
				.setResult(AdminCommunityProtos.MigratePostResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetCommentListResponse> getCommentList(AdminHead head, GetCommentListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetCommentListResponse.newBuilder()
					.setTotalSize(0)
					.setFilteredSize(0)
					.build());
		}
		final long companyId = head.getCompanyId();
		final int postId = request.getPostId();
		final Integer start = request.hasStart() ? request.getStart() : null;
		
		Connection dbConn = null;
		DataPage<CommunityProtos.Comment>  commentDataPage = null;
		try {
			dbConn = hikariDataSource.getConnection();
			commentDataPage = CommunityDB.getPostCommentListByStart(dbConn, companyId, request.getPostId(), start, request.getLength(), CommunityUtil.ADMIN_COMMENT_STATE_LIST);		
		} catch (SQLException e) {
			throw new RuntimeException("获取评论信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		//评论没加缓存，所以不用获取缓存

		return Futures.immediateFuture(AdminCommunityProtos.GetCommentListResponse.newBuilder()
				.setTotalSize(commentDataPage.totalSize())
				.setFilteredSize(commentDataPage.filteredSize())
				.addAllComment(this.setCommentExtValue(postId, companyId, dbConn, commentDataPage.dataList()))
				.build());
	}

	private List<CommunityProtos.Comment> setCommentExtValue(int postId,
			final long companyId, Connection dbConn, List<CommunityProtos.Comment> commentList) {
		
		List<Integer> commentIdList = new ArrayList<Integer>();
		for(CommunityProtos.Comment comment : commentList){
			if(comment!=null){
				commentIdList.add(comment.getCommentId());
			}		
		}
		
		Map<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>> postCommentExtMap = new HashMap<Integer, Map<Integer, CommunityDAOProtos.PostCommentExt>>();
		try {
			dbConn = this.hikariDataSource.getConnection();
			postCommentExtMap = CommunityDB.getPostCommentExt(dbConn, companyId, Collections.singletonMap(postId, commentIdList));
		} catch (SQLException e) {
			throw new RuntimeException("db fail", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		List<CommunityProtos.Comment> resultCommentList = new ArrayList<CommunityProtos.Comment>();
		CommunityProtos.Comment.Builder tmpComment = CommunityProtos.Comment.newBuilder();
		for(CommunityProtos.Comment comment : commentList){
			tmpComment.clear();
			tmpComment.mergeFrom(comment);
			Map<Integer,CommunityDAOProtos.PostCommentExt> postCommentExt = postCommentExtMap.get(postId);
			int likeCount = postCommentExt != null && postCommentExt.get(comment.getCommentId()) != null ? postCommentExt.get(comment.getCommentId())
					.getLikeCount() : 0;
			tmpComment.setLikeCount(likeCount);
			resultCommentList.add(tmpComment.build());
		}
		return resultCommentList;
	}

	@Override
	public ListenableFuture<DeleteCommentResponse> deleteComment(AdminHead head, DeleteCommentRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteCommentResponse.newBuilder()
					.setResult(DeleteCommentResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		int postId = request.getPostId();
		if (postId < 0) {
			return Futures.immediateFuture(AdminCommunityProtos.DeleteCommentResponse.newBuilder()
					.setResult(AdminCommunityProtos.DeleteCommentResponse.Result.FAIL_UNKNOWN)
					.build());
		}
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.updateCommentStateDelete(dbConn, companyId, request.getPostId(), request.getCommentId());
		} catch (SQLException e) {
			throw new RuntimeException("删除评论信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = this.jedisPool.getResource();
		try {
			CommunityCache.delPostExt(jedis, companyId, Collections.singleton(request.getPostId()));
		} finally {
			jedis.close();
		}

		logger.info("delete CommentList  end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.DeleteCommentResponse.newBuilder()
				.setResult(AdminCommunityProtos.DeleteCommentResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<SetStickyPostResponse> setStickyPost(AdminHead head, SetStickyPostRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(SetStickyPostResponse.newBuilder()
					.setResult(SetStickyPostResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		CommunityProtos.Post post = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, Collections.singleton(request.getPostId()),
				CommunityUtil.ADMIN_POST_STATE_LIST).get(request.getPostId());

		if (null == post) {
			return Futures.immediateFuture(AdminCommunityProtos.SetStickyPostResponse.newBuilder()
					.setResult(AdminCommunityProtos.SetStickyPostResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("该帖子不存在！")
					.build());
		}
		
		boolean isSticky = request.getIsSticky();
		if (isSticky) {
			CommunityDAOProtos.BoardExt boardExt = CommunityUtil.doGetBoardExt(jedisPool, hikariDataSource, companyId, Collections.singleton(post.getBoardId()))
					.get(post.getBoardId());
			int stickyPostCont = 0;
			for (CommunityDAOProtos.PostListIndex postListIndex : boardExt.getStickyPostIndexList()) {
				if (postListIndex.getIsSticky() && CommunityUtil.ADMIN_POST_STATE_LIST.contains(postListIndex.getState())) {
					stickyPostCont++;
				}
			}
			if (stickyPostCont >= CommunityUtil.MAX_BOARD_STICKY_POST_LIST_SIZE) {
				return Futures.immediateFuture(AdminCommunityProtos.SetStickyPostResponse.newBuilder()
						.setResult(AdminCommunityProtos.SetStickyPostResponse.Result.FAIL_STICKY_POST_COUNT_OUT_OF_RANGE)
						.setFailText(new StringBuilder("每个板块下置顶贴不能超过").append(CommunityUtil.MAX_BOARD_STICKY_POST_LIST_SIZE).append("个！").toString())
						.build());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.updatePostSticky(dbConn, companyId, Collections.singleton(request.getPostId()), request.getIsSticky(), (int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("删除帖子失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delPost(jedis, companyId, Collections.singleton(post.getPostId()));
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(post.getBoardId()));
		} finally {
			jedis.close();
		}

		logger.info("set Sticky Post end, adminId:" + head.getSession().getAdminId());

		return Futures.immediateFuture(AdminCommunityProtos.SetStickyPostResponse.newBuilder()
				.setResult(AdminCommunityProtos.SetStickyPostResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<RecommendPostResponse> recommendPost(AdminHead head, RecommendPostRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(RecommendPostResponse.newBuilder()
					.setResult(RecommendPostResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();

		int postId = request.getPostId();
		boolean isRecommend = request.getIsRecommend();
		
		CommunityProtos.Post post = CommunityUtil.doGetPost(jedisPool,
				hikariDataSource,
				companyId,
				Collections.singleton(request.getPostId()),
				CommunityUtil.ADMIN_POST_STATE_LIST).get(postId);

		if (null == post) {
			return Futures.immediateFuture(AdminCommunityProtos.RecommendPostResponse.newBuilder()
					.setResult(AdminCommunityProtos.RecommendPostResponse.Result.FAIL_POST_NOT_EXIST)
					.setFailText("该帖子不存在！")
					.build());
		}
		
		if(isRecommend){
			final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(jedisPool, hikariDataSource, companyId);
			List<Integer> recommendPostIdList = new ArrayList<Integer>();
			for (CommunityDAOProtos.RecommendPostListIndex recommendPostIndex : communityInfo.getRecommendPostIndexList()) {
				if (recommendPostIdList != null) {
					recommendPostIdList.add(recommendPostIndex.getPostId());
				}
			}

			Map<Integer, CommunityProtos.Post> recommendPostMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, recommendPostIdList, CommunityUtil.ADMIN_POST_STATE_LIST);

			if (recommendPostMap.size() >= CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE) {
				return Futures.immediateFuture(AdminCommunityProtos.RecommendPostResponse.newBuilder()
						.setResult(AdminCommunityProtos.RecommendPostResponse.Result.FAIL_RECOMMENDED_POST_COUNT_OUT_OF_RANGE)
						.setFailText(new StringBuilder("推荐贴最大数量不能超过").append(CommunityUtil.MAX_BOARD_STICKY_POST_LIST_SIZE).append("个！").toString())
						.build());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.updatePostRecommended(dbConn, companyId, Collections.singleton(postId), isRecommend, (int) (System.currentTimeMillis() / 1000L));
		} catch (SQLException e) {
			throw new RuntimeException("db failed！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delPost(jedis, companyId, Collections.singleton(post.getPostId()));
			CommunityCache.delCommunityInfo(jedis, Collections.singleton(companyId));
		} finally {
			jedis.close();
		}
		
		logger.info("recommend Post end, adminId:" + head.getSession().getAdminId());
		
		return Futures.immediateFuture(AdminCommunityProtos.RecommendPostResponse.newBuilder()
				.setResult(AdminCommunityProtos.RecommendPostResponse.Result.SUCC)
				.build());
	}

	@Override
	public ListenableFuture<GetRecommendPostResponse> getRecommendPost(AdminHead head, EmptyRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetRecommendPostResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();

		final CommunityDAOProtos.CommunityInfo communityInfo = CommunityUtil.doGetCommunityInfo(jedisPool, hikariDataSource, companyId);
		List<Integer> recommendPostIdList = new ArrayList<Integer>();
		for (CommunityDAOProtos.RecommendPostListIndex recommendPostIndex : communityInfo.getRecommendPostIndexList()) {
			if (recommendPostIdList != null) {
				recommendPostIdList.add(recommendPostIndex.getPostId());
			}
		}

		Map<Integer, CommunityProtos.Post> recommendPostMap = CommunityUtil.doGetPost(jedisPool, hikariDataSource, companyId, recommendPostIdList, CommunityUtil.ADMIN_POST_STATE_LIST);

		AdminCommunityProtos.GetRecommendPostResponse.Builder responseBuilder = AdminCommunityProtos.GetRecommendPostResponse.newBuilder();
		//添加依赖的版块
		Set<Integer> boardIds = new TreeSet<Integer>();
		for (CommunityProtos.Post post : recommendPostMap.values()) {
			if (null != post) {
				boardIds.add(post.getBoardId());
			}
		}
		responseBuilder.addAllRefBoard(CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, boardIds));

		// 给post赋comment_count,like_count的值

		List<CommunityProtos.Post> recommendPostList = this.setPostExtValue(companyId, recommendPostMap, recommendPostIdList);
		
		if (recommendPostList.size() > CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE) {
			recommendPostList = recommendPostList.subList(0, CommunityUtil.MAX_RECOMMENDED_POST_LIST_SIZE);
		}
		responseBuilder.addAllPost(recommendPostList);
				
		return Futures.immediateFuture(responseBuilder.build());
	}
	
	/**
	 * 给response设置comment_count,like_count值,标签列表
	 * 
	 * @param jedisPool
	 * @param hikariDataSource
	 * @param postMap
	 * @param postIds
	 * @param responseBuilder
	 */
	private List<CommunityProtos.Post> setPostExtValue(long companyId, Map<Integer, Post> postMap, List<Integer> postIds) {
		
		Map<Integer, CommunityDAOProtos.PostExt> postExtMap = CommunityUtil.doGetPostExt(jedisPool, hikariDataSource, companyId, postMap.keySet());
		List<CommunityProtos.Post> posts=new ArrayList<CommunityProtos.Post>();
		CommunityProtos.Post.Builder tmpPostBuilder = CommunityProtos.Post.newBuilder();
		for (Integer postId : postIds) {
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
					if (CommunityUtil.ADMIN_COMMENT_STATE_LIST.contains(cnt.getState())) {
						commentCount += cnt.getCount();
					}
				}

				tmpPostBuilder.setCommentCount(commentCount);
				tmpPostBuilder.setLikeCount(postExt.getLikeCount());
				tmpPostBuilder.addAllTag(postExt.getTagList());
			}

			posts.add(tmpPostBuilder.build());
		}
		return posts;
	}

	private static final CommunityDAOProtos.PostCommentListIndex POST_COMMENT_LIST_INDEX_END = 
			CommunityDAOProtos.PostCommentListIndex.newBuilder()
			.setCommentId(0)
			.build();
	
	@Override
	public ListenableFuture<ExportCommentListResponse> exportCommentList(AdminHead head, ExportCommentListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ExportCommentListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		final long companyId = head.getCompanyId();
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
			return Futures.immediateFuture(ExportCommentListResponse.newBuilder()
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

		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		
		return Futures.immediateFuture(AdminCommunityProtos.ExportCommentListResponse.newBuilder()
				.setHasMore(hasMore)
				.setOffsetIndex(nextOffsetIndex)
				.addAllComment(this.setCommentExtValue(postId, companyId, dbConn, commentList))
				.setRefPost(post)
				.build());
	}

	@Override
	public ListenableFuture<CreateBoardTagResponse> createBoardTag(AdminHead head, CreateBoardTagRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateBoardTagResponse.newBuilder()
					.setResult(CreateBoardTagResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int boardId = request.getBoardId();
		final List<String> tagList = request.getTagList();
		
		List<CommunityProtos.Board> boardList = CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId));
		if (boardList == null || boardList.isEmpty()) {
			return Futures.immediateFuture(CreateBoardTagResponse.newBuilder()
					.setResult(CreateBoardTagResponse.Result.FAIL_BOARD_NOT_EXIST)
					.setFailText("该版块不存在!")
					.build());
		} 
		for (String tag : tagList) {
			if (tag.length() > 191) {
				return Futures.immediateFuture(CreateBoardTagResponse.newBuilder()
						.setResult(CreateBoardTagResponse.Result.FAIL_TAG_INVALID)
						.setFailText("标签长度超出限制！")
						.build());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.insertBoardTag(dbConn, companyId, boardId, tagList);
		} catch (SQLException e) {
			throw new RuntimeException("创建标签失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(boardId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(CreateBoardTagResponse.newBuilder().setResult(CreateBoardTagResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<DeleteBoardTagResponse> deleteBoardTag(AdminHead head, DeleteBoardTagRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(DeleteBoardTagResponse.newBuilder()
					.setResult(DeleteBoardTagResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final int boardId = request.getBoardId();
		final List<String> tagList = request.getTagList();
		
		List<CommunityProtos.Board> boardList = CommunityUtil.doGetBoard(jedisPool, hikariDataSource, companyId, Collections.singleton(boardId));
		if (boardList == null || boardList.isEmpty()) {
			return Futures.immediateFuture(DeleteBoardTagResponse.newBuilder()
					.setResult(DeleteBoardTagResponse.Result.FAIL_BOARD_NOT_EXIST)
					.setFailText("该版块不存在!")
					.build());
		} 
		for (String tag : tagList) {
			if (tag.length() > 191) {
				return Futures.immediateFuture(DeleteBoardTagResponse.newBuilder()
						.setResult(DeleteBoardTagResponse.Result.FAIL_TAG_INVALID)
						.setFailText("标签长度超出限制！")
						.build());
			}
		}
		
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			CommunityDB.deleteBoardTag(dbConn, companyId, boardId, tagList);
		} catch (SQLException e) {
			throw new RuntimeException("删除标签失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}

		Jedis jedis = jedisPool.getResource();
		try {
			CommunityCache.delBoardExt(jedis, companyId, Collections.singleton(boardId));
		} finally {
			jedis.close();
		}
		
		return Futures.immediateFuture(DeleteBoardTagResponse.newBuilder().setResult(DeleteBoardTagResponse.Result.SUCC).build());
	}

	@Override
	public ListenableFuture<GetBoardTagResponse> getBoardTag(AdminHead head, GetBoardTagRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(GetBoardTagResponse.newBuilder().build());
		}
		final long companyId = head.getCompanyId();
		final int boardId = request.getBoardId();
		
		List<String> tagList = new ArrayList<String>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			tagList = CommunityDB.getBoardTag(dbConn, companyId, boardId);
		} catch (SQLException e) {
			throw new RuntimeException("获取版块下标签失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		return Futures.immediateFuture(GetBoardTagResponse.newBuilder().addAllTag(tagList).build());
	}

	@Override
	public ListenableFuture<CreateCommentResponse> createComment(AdminHead head, CreateCommentRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreateCommentResponse.newBuilder()
					.setResult(CreateCommentResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long createUserId = request.getCreateUserId();
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
		commentBuilder.setCreateUserId(createUserId);
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
		
		if (post.getCreateUserId() != 0) {
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
		
		if (replyComment != null && replyComment.getCreateUserId() != 0 && replyComment.getCreateUserId() != post.getCreateUserId()) {
			pushRequestBuilder
					.addPushPacket(
							PushProtos.PushPacket.newBuilder()
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
	public ListenableFuture<ExportPostLikeListResponse> exportPostLikeList(AdminHead head, ExportPostLikeListRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(ExportPostLikeListResponse.newBuilder()
					.setHasMore(false)
					.setOffsetIndex(ByteString.EMPTY)
					.build());
		}
		final long companyId = head.getCompanyId();
		final int postId = request.getPostId();
		final int size = request.getSize();
		final CommunityProtos.PostLike currentOffsetIndex;

		if (request.hasOffsetIndex() && !request.getOffsetIndex().isEmpty()) {
			CommunityProtos.PostLike tmp = null;
			try {
				tmp = CommunityProtos.PostLike.parseFrom(request.getOffsetIndex());
			} catch (InvalidProtocolBufferException e) {
				tmp = null;
			}
			currentOffsetIndex = tmp;
		} else {
			currentOffsetIndex = null;
		}
		
		List<CommunityProtos.PostLike> postLikeList = new ArrayList<CommunityProtos.PostLike>();
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			postLikeList = CommunityDB.getPostLikeList(dbConn, companyId, postId, size + 1, currentOffsetIndex);
		} catch (SQLException e) {
			throw new RuntimeException("获取点赞信息失败！", e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		ExportPostLikeListResponse.Builder response = ExportPostLikeListResponse.newBuilder();
		if (postLikeList.size() > size) {
			response.addAllPostLike(postLikeList.subList(0, size));
			response.setHasMore(true);
			response.setOffsetIndex(postLikeList.get(size - 1).toByteString());
		} else {
			response.addAllPostLike(postLikeList);
			response.setHasMore(false);
			if (postLikeList.isEmpty()) {
				response.setOffsetIndex(ByteString.EMPTY);
			} else {
				response.setOffsetIndex(postLikeList.get(postLikeList.size() - 1).toByteString());
			}
		}
		
		CommunityProtos.Post post = CommunityUtil.doGetPost(this.jedisPool,
				this.hikariDataSource,
				companyId,
				Collections.singleton(postId),
				CommunityUtil.USER_POST_STATE_LIST).get(postId);
		
		response.setRefPost(post);
		
		return Futures.immediateFuture(response.build());
	}

	@Override
	public ListenableFuture<CreatePostResponse> createPost(AdminHead head, CreatePostRequest request) {
		if (!head.hasCompanyId()) {
			return Futures.immediateFuture(CreatePostResponse.newBuilder()
					.setResult(CreatePostResponse.Result.FAIL_UNKNOWN)
					.setFailText("company_id 参数未填")
					.build());
		}
		final long companyId = head.getCompanyId();
		final long createUserId = request.getCreateUserId();
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
			AdminUserProtos.GetAbilityTagUserIdResponse response = null;
			try {
				response = this.adminUserService
						.getAbilityTagUserId(head, AdminUserProtos.GetAbilityTagUserIdRequest.newBuilder().addAllTagName(tagList).build()).get();
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
}
