package com.weizhu.service.im;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.IMProtos;

public class IMDB {
	
	public static Long getP2PLatestMsgSeqIfExist(Connection conn, long companyId, long userIdMost, long userIdLeast) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_most = ? AND user_id_least = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userIdMost);
			pstmt.setLong(3, userIdLeast);
			
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return rs.getLong("latest_msg_seq");
			} else {
				return null;
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static long getP2PLatestMsgSeq(Connection conn, long companyId, long userIdMost, long userIdLeast) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_im_p2p (company_id, user_id_most, user_id_least, latest_msg_seq, latest_msg_time) VALUES (?, ?, ?, 0, 0); SELECT latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_most = ? AND user_id_least = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userIdMost);
			pstmt.setLong(3, userIdLeast);
			pstmt.setLong(4, companyId);
			pstmt.setLong(5, userIdMost);
			pstmt.setLong(6, userIdLeast);
			
			pstmt.execute();
			pstmt.getMoreResults();
			
			rs = pstmt.getResultSet();
			
			if (rs.next()) {
				return rs.getLong("latest_msg_seq");
			} else {
				throw new RuntimeException("cannot get latest_msg_seq");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final String INSERT_P2P_MESSAGE_SQL = 
			"INSERT INTO weizhu_im_p2p_msg (company_id, user_id_most, user_id_least, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, discover_item_item_id, video_name, video_type, video_size, video_time, video_image_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); "
			+ "UPDATE weizhu_im_p2p SET latest_msg_seq = ?, latest_msg_time = ? WHERE company_id = ? AND user_id_most = ? AND user_id_least = ? AND latest_msg_seq < ?; ";

	public static void insertP2PMessage(Connection conn, long companyId, long userIdMost, long userIdLeast, IMProtos.InstantMessage msg) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(INSERT_P2P_MESSAGE_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userIdMost);
			pstmt.setLong(3, userIdLeast);
			pstmt.setLong(4, msg.getMsgSeq());
			pstmt.setInt(5, msg.getMsgTime());
			pstmt.setLong(6, msg.getFromUserId());
			
			DBUtil.set(pstmt, 7, msg.hasText(), msg.getText().getContent());
			DBUtil.set(pstmt, 8, msg.hasVoice(), msg.getVoice().getData());
			DBUtil.set(pstmt, 9, msg.hasVoice(), msg.getVoice().getDuration());
			DBUtil.set(pstmt, 10, msg.hasImage(), msg.getImage().getName());
			DBUtil.set(pstmt, 11, msg.hasUser(), msg.getUser().getUserId());
			DBUtil.set(pstmt, 12, msg.hasDiscoverItem(), msg.getDiscoverItem().getItemId());
			
			DBUtil.set(pstmt, 13, msg.hasVideo(), msg.getVideo().getName());
			DBUtil.set(pstmt, 14, msg.hasVideo(), msg.getVideo().getType());
			DBUtil.set(pstmt, 15, msg.hasVideo(), msg.getVideo().getSize());
			DBUtil.set(pstmt, 16, msg.hasVideo(), msg.getVideo().getTime());
			DBUtil.set(pstmt, 17, msg.hasVideo(), msg.getVideo().getImageName());
			
			pstmt.setLong(18, msg.getMsgSeq());
			pstmt.setInt(19, msg.getMsgTime());
			pstmt.setLong(20, companyId);
			pstmt.setLong(21, userIdMost);
			pstmt.setLong(22, userIdLeast);
			pstmt.setLong(23, msg.getMsgSeq());
			
			pstmt.execute();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final ProtobufMapper<IMProtos.InstantMessage> INSTANT_MESSAGE_MAPPER = 
			ProtobufMapper.createMapper(IMProtos.InstantMessage.getDefaultInstance(), 
					"msg_seq", 
					"msg_time", 
					"from_user_id", 
					"text.content", 
					"voice.data", 
					"voice.duration",
					"image.name",
					"user.user_id",
					"discover_item.item_id",
					"video.name",
					"video.type",
					"video.size",
					"video.time",
					"video.image_name"
					);
	
	public static List<IMProtos.InstantMessage> getP2PMessage(Connection conn, 
			long companyId, long userIdMost, long userIdLeast, 
			@Nullable Long msgSeqBegin, @Nullable Long msgSeqEnd, int size) throws SQLException {
		if (size <= 0 || (msgSeqBegin != null && msgSeqEnd != null && msgSeqBegin - msgSeqEnd - 1 <= 0)) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT msg_seq, msg_time, from_user_id, text_content as `text.content`, voice_data as `voice.data`, voice_duration as `voice.duration`, image_name as `image.name`, user_user_id as `user.user_id`, discover_item_item_id as `discover_item.item_id`, video_name as `video.name`, video_type as `video.type`, video_size as `video.size`, video_time as `video.time`, video_image_name as `video.image_name` FROM weizhu_im_p2p_msg ");
		sqlBuilder.append("WHERE company_id = ").append(companyId).append(" AND user_id_most = ").append(userIdMost).append(" AND user_id_least = ").append(userIdLeast);
		if (msgSeqBegin != null) {
			sqlBuilder.append(" AND msg_seq < ").append(msgSeqBegin);
		}
		if (msgSeqEnd != null) {
			sqlBuilder.append(" AND msg_seq > ").append(msgSeqEnd);
		}
		sqlBuilder.append(" ORDER BY msg_seq DESC LIMIT ").append(size);
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<IMProtos.InstantMessage> resultList = new ArrayList<IMProtos.InstantMessage>();
			IMProtos.InstantMessage.Builder tmpBuilder = IMProtos.InstantMessage.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				resultList.add(INSTANT_MESSAGE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
			
			return resultList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final Comparator<IMProtos.P2PChat> P2P_CHAT_CMP = new Comparator<IMProtos.P2PChat>() {

		@Override
		public int compare(IMProtos.P2PChat o1, IMProtos.P2PChat o2) {
			if (o1.getLatestMsg().getMsgTime() > o2.getLatestMsg().getMsgTime()) {
				return -1;
			} else if (o1.getLatestMsg().getMsgTime() < o2.getLatestMsg().getMsgTime()) {
				return 1;
			} else if (o1.getUserId() > o2.getUserId()) {
				return -1;
			} else if (o1.getUserId() < o2.getUserId()) {
				return 1;
			} else {
				return 0;
			}
		}
		
	};
	
	private static final String GET_P2P_CHAT_LIST_SQL = 
			"SELECT "
			+ "A.user_id_most as user_id_most, "
			+ "A.user_id_least as user_id_least, "
			+ "B.msg_seq as `msg_seq`, "
			+ "B.msg_time as `msg_time`, "
			+ "B.from_user_id as `from_user_id`, "
			+ "B.text_content as `text.content`, "
			+ "B.voice_data as `voice.data`, "
			+ "B.voice_duration as `voice.duration`, "
			+ "B.image_name as `image.name`, "
			+ "B.user_user_id as `user.user_id`, "
			+ "B.discover_item_item_id as `discover_item.item_id`, "
			+ "B.video_name as `video.name`, "
			+ "B.video_type as `video.type`, "
			+ "B.video_size as `video.size`, "
			+ "B.video_time as `video.time`, "
			+ "B.video_image_name as `video.image_name` "
			+ "FROM ("
			+ "(SELECT company_id, user_id_most, user_id_least, latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_most = ? ORDER BY latest_msg_time DESC, user_id_least DESC LIMIT ?) "
			+ "UNION ALL "
			+ "(SELECT company_id, user_id_most, user_id_least, latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_least = ? AND user_id_most != ? ORDER BY latest_msg_time DESC, user_id_most DESC LIMIT ?) "
			+ ") A INNER JOIN weizhu_im_p2p_msg B "
			+ "ON A.company_id = B.company_id AND A.user_id_most = B.user_id_most AND A.user_id_least = B.user_id_least AND A.latest_msg_seq = B.msg_seq; "
			;
	
	public static List<IMProtos.P2PChat> getP2PChatList(Connection conn, long companyId, long userId, int chatSize) throws SQLException {
		if (chatSize <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_P2P_CHAT_LIST_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, chatSize);
			
			pstmt.setLong(4, companyId);
			pstmt.setLong(5, userId);
			pstmt.setLong(6, userId);
			pstmt.setInt(7, chatSize);
			
			rs = pstmt.executeQuery();
			
			List<IMProtos.P2PChat> list = new ArrayList<IMProtos.P2PChat>(chatSize * 2);
			
			IMProtos.P2PChat.Builder chatBuilder = IMProtos.P2PChat.newBuilder();
			IMProtos.InstantMessage.Builder msgBuilder = IMProtos.InstantMessage.newBuilder();
			while (rs.next()) {
				chatBuilder.clear();
				msgBuilder.clear();
				
				long userIdMost = rs.getLong("user_id_most");
				long userIdLeast = rs.getLong("user_id_least");
				IMProtos.InstantMessage msg = INSTANT_MESSAGE_MAPPER.mapToItem(rs, msgBuilder).build();
				
				list.add(chatBuilder
						.setUserId(userIdMost == userId ? userIdLeast : userIdMost)
						.setLatestMsg(msg)
						.build());
			}
			
			Collections.sort(list, P2P_CHAT_CMP);
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final String GET_P2P_CHAT_LIST_LAST_SQL = 
			"SELECT "
			+ "A.user_id_most as user_id_most, "
			+ "A.user_id_least as user_id_least, "
			+ "B.msg_seq as `msg_seq`, "
			+ "B.msg_time as `msg_time`, "
			+ "B.from_user_id as `from_user_id`, "
			+ "B.text_content as `text.content`, "
			+ "B.voice_data as `voice.data`, "
			+ "B.voice_duration as `voice.duration`, "
			+ "B.image_name as `image.name`, "
			+ "B.user_user_id as `user.user_id` "
			+ "B.discover_item_item_id as `discover_item.item_id`, "
			+ "B.video_name as `video.name`, "
			+ "B.video_type as `video.type`, "
			+ "B.video_size as `video.size`, "
			+ "B.video_time as `video.time`, "
			+ "B.video_image_name as `video.image_name` "
			+ "FROM ("
			+ "(SELECT company_id, user_id_most, user_id_least, latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_most = ? AND ((latest_msg_time = ? AND user_id_least < ?) OR (latest_msg_time < ?)) ORDER BY latest_msg_time DESC, user_id_least DESC LIMIT ?) "
			+ "UNION ALL "
			+ "(SELECT company_id, user_id_most, user_id_least, latest_msg_seq FROM weizhu_im_p2p WHERE company_id = ? AND user_id_least = ? AND user_id_most != ? AND ((latest_msg_time = ? AND user_id_most < ?) OR (latest_msg_time < ?)) ORDER BY latest_msg_time DESC, user_id_most DESC LIMIT ?) "
			+ ") A INNER JOIN weizhu_im_p2p_msg B "
			+ "ON A.company_id = B.company_id AND A.user_id_most = B.user_id_most AND A.user_id_least = B.user_id_least AND A.latest_msg_seq = B.msg_seq; "
			;
	
	public static List<IMProtos.P2PChat> getP2PChatList(Connection conn, long companyId, long userId, long lastUserId, int lastMsgTime, int chatSize) throws SQLException {
		if (chatSize <= 0) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_P2P_CHAT_LIST_LAST_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, userId);
			pstmt.setInt(3, lastMsgTime);
			pstmt.setLong(4, lastUserId);
			pstmt.setInt(5, lastMsgTime);
			pstmt.setInt(6, chatSize);
			
			pstmt.setLong(7, companyId);
			pstmt.setLong(8, userId);
			pstmt.setLong(9, userId);
			pstmt.setInt(10, lastMsgTime);
			pstmt.setLong(11, lastUserId);
			pstmt.setInt(12, lastMsgTime);
			pstmt.setInt(13, chatSize);
			
			rs = pstmt.executeQuery();
			
			List<IMProtos.P2PChat> list = new ArrayList<IMProtos.P2PChat>(chatSize * 2);
			
			IMProtos.P2PChat.Builder chatBuilder = IMProtos.P2PChat.newBuilder();
			IMProtos.InstantMessage.Builder msgBuilder = IMProtos.InstantMessage.newBuilder();
			while (rs.next()) {
				chatBuilder.clear();
				msgBuilder.clear();
				
				long userIdMost = rs.getLong("user_id_most");
				long userIdLeast = rs.getLong("user_id_least");
				IMProtos.InstantMessage msg = INSTANT_MESSAGE_MAPPER.mapToItem(rs, msgBuilder).build();
				
				list.add(chatBuilder
						.setUserId(userIdMost == userId ? userIdLeast : userIdMost)
						.setLatestMsg(msg)
						.build());
			}
			
			Collections.sort(list, P2P_CHAT_CMP);
			
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Long, IMProtos.GroupChat> getGroupChatById(Connection conn, long companyId, Collection<Long> groupIds) throws SQLException {
		if (groupIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String groupIdStr = DBUtil.COMMA_JOINER.join(groupIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT group_id, user_id, msg_seq FROM weizhu_im_group_member WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND group_id IN (").append(groupIdStr).append(") AND is_join = 1 ORDER BY group_id ASC, msg_seq ASC; ");
		sqlBuilder.append("SELECT group_id, group_name FROM weizhu_im_group WHERE company_id = ").append(companyId);
		sqlBuilder.append(" AND group_id IN (").append(groupIdStr).append("); ");
		
		final String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			
			Map<Long, List<IMProtos.GroupChat.Member>> groupMemberMap = new TreeMap<Long, List<IMProtos.GroupChat.Member>>();
			
			IMProtos.GroupChat.Member.Builder tmpMemberBuilder = IMProtos.GroupChat.Member.newBuilder();
			while (rs.next()) {
				long groupId = rs.getLong("group_id");
				IMProtos.GroupChat.Member member = tmpMemberBuilder.clear()
						.setUserId(rs.getLong("user_id"))
						.setJoinMsgSeq(rs.getLong("msg_seq"))
						.build();
				
				DBUtil.addMapArrayList(groupMemberMap, groupId, member);
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, IMProtos.GroupChat> groupChatMap = new TreeMap<Long, IMProtos.GroupChat>();
			IMProtos.GroupChat.Builder tmpGroupChatBuilder = IMProtos.GroupChat.newBuilder();
			while (rs.next()) {
				tmpGroupChatBuilder.clear();
				
				Long groupId = rs.getLong("group_id");
				tmpGroupChatBuilder.setGroupId(groupId);
				tmpGroupChatBuilder.setGroupName(rs.getString("group_name"));
				List<IMProtos.GroupChat.Member> memberList = groupMemberMap.get(groupId);
				if (memberList != null) {
					tmpGroupChatBuilder.addAllMember(memberList);
				}
				groupChatMap.put(groupId, tmpGroupChatBuilder.build());
			}
			return groupChatMap;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final String GET_GROUP_CHAT_ID_LIST_SQL = 
			"SELECT group_id, group_name, latest_msg_seq "
			+ "FROM weizhu_im_group "
			+ "WHERE company_id = ? AND group_id IN (SELECT group_id FROM weizhu_im_group_member WHERE company_id = ? AND user_id = ? AND is_join = 1) "
			+ "ORDER BY latest_msg_time DESC LIMIT ?; ";
	
	public static List<IMProtos.GroupChat> getGroupChatList(Connection conn, long companyId, long userId, int chatSize) throws SQLException {
		if (chatSize <= 0) {
			return Collections.emptyList();
		}
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(GET_GROUP_CHAT_ID_LIST_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, companyId);
			pstmt.setLong(3, userId);
			pstmt.setInt(4, chatSize);
			
			rs = pstmt.executeQuery();
			
			Map<Long, IMProtos.GroupChat.Builder> groupChatBuilderMap = new LinkedHashMap<Long, IMProtos.GroupChat.Builder>(chatSize);
			Map<Long, Long> groupChatLatestMsgSeqMap = new HashMap<Long, Long>(chatSize);
			while (rs.next()) {
				long groupId = rs.getLong("group_id");
				String groupName = rs.getString("group_name");
				long latestMsgSeq = rs.getLong("latest_msg_seq");
				
				groupChatBuilderMap.put(groupId, IMProtos.GroupChat.newBuilder()
						.setGroupId(groupId)
						.setGroupName(groupName));
				
				if (latestMsgSeq > 0) {
					groupChatLatestMsgSeqMap.put(groupId, latestMsgSeq);
				}
			}
			
			if (groupChatBuilderMap.isEmpty()) {
				return Collections.emptyList();
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			DBUtil.closeQuietly(pstmt);
			pstmt = null;
			
			StringBuilder sqlBuilder = new StringBuilder();
			// 获取 member
			sqlBuilder.append("SELECT group_id, user_id, msg_seq FROM weizhu_im_group_member WHERE company_id = ").append(companyId).append(" AND group_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sqlBuilder, groupChatBuilderMap.keySet());
			sqlBuilder.append(") AND is_join = 1 ORDER BY group_id ASC, msg_seq ASC; ");
			
			if (!groupChatLatestMsgSeqMap.isEmpty()) {
				// 获取最新消息
				sqlBuilder.append("SELECT group_id, msg_seq, msg_time, from_user_id, text_content as `text.content`, voice_data as `voice.data`, voice_duration as `voice.duration`, image_name as `image.name`, user_user_id as `user.user_id`, ");
				sqlBuilder.append("group_group_name, group_join_user_id, group_leave_user_id, discover_item_item_id as `discover_item.item_id`, video_name as `video.name`, video_type as `video.type`, video_size as `video.size`, video_time as `video.time`, video_image_name as `video.image_name` FROM weizhu_im_group_msg ");
				sqlBuilder.append("WHERE (company_id, group_id, msg_seq) IN (");
				boolean isFirst = true;
				for (Map.Entry<Long, Long> entry : groupChatLatestMsgSeqMap.entrySet()) {
					if (isFirst) {
						isFirst = false;
					} else {
						sqlBuilder.append(", ");
					}
					sqlBuilder.append("(").append(companyId).append(", ").append(entry.getKey()).append(", ").append(entry.getValue()).append(")");
				}
				sqlBuilder.append("); ");
			}
			
			final String sql = sqlBuilder.toString();
			
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			
			IMProtos.GroupChat.Member.Builder tmpMemberBuilder = IMProtos.GroupChat.Member.newBuilder();
			while (rs.next()) {
				long groupId = rs.getLong("group_id");
				IMProtos.GroupChat.Builder builder = groupChatBuilderMap.get(groupId);
				if (builder != null) {
					builder.addMember(tmpMemberBuilder.clear()
							.setUserId(rs.getLong("user_id"))
							.setJoinMsgSeq(rs.getLong("msg_seq"))
							.build());
				}
			}
			
			if (!groupChatLatestMsgSeqMap.isEmpty()) {
				DBUtil.closeQuietly(rs);
				rs = null;
				
				stmt.getMoreResults();
				rs = stmt.getResultSet();
				
				IMProtos.InstantMessage.Builder tmpBuilder = IMProtos.InstantMessage.newBuilder();
				IMProtos.InstantMessage.Group.Builder tmpGroupBuilder = IMProtos.InstantMessage.Group.newBuilder();
				while (rs.next()) {
					tmpBuilder.clear();
					INSTANT_MESSAGE_MAPPER.mapToItem(rs, tmpBuilder);
					
					tmpGroupBuilder.clear();
					String groupName = rs.getString("group_group_name");
					if (groupName != null) {
						tmpGroupBuilder.setGroupName(groupName);
					}
					String groupJoinUserIdStr = rs.getString("group_join_user_id");
					if (groupJoinUserIdStr != null) {
						Iterable<String> iterable = DBUtil.COMMA_SPLITTER.split(groupJoinUserIdStr);
						for (String userIdStr : iterable) {
							try {
								tmpGroupBuilder.addJoinUserId(Long.parseLong(userIdStr));
							} catch (NumberFormatException e) {
								// ignore
							}
						}
					}
					String groupLeaveUserIdStr = rs.getString("group_leave_user_id");
					if (groupLeaveUserIdStr != null) {
						Iterable<String> iterable = DBUtil.COMMA_SPLITTER.split(groupLeaveUserIdStr);
						for (String userIdStr : iterable) {
							try {
								tmpGroupBuilder.addLeaveUserId(Long.parseLong(userIdStr));
							} catch (NumberFormatException e) {
								// ignore
							}
						}
					}
					
					if (groupJoinUserIdStr != null && groupLeaveUserIdStr != null && tmpGroupBuilder.isInitialized()) {
						tmpBuilder.setGroup(tmpGroupBuilder.build());
					}
					
					long groupId = rs.getLong("group_id");
					IMProtos.GroupChat.Builder builder = groupChatBuilderMap.get(groupId);
					if (builder != null) {
						builder.setLatestMsg(tmpBuilder.build());
					}
				}
			}
			
			List<IMProtos.GroupChat> list = new ArrayList<IMProtos.GroupChat>(groupChatBuilderMap.size());
			for (IMProtos.GroupChat.Builder builder : groupChatBuilderMap.values()) {
				list.add(builder.build());
			}
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static long insertGroupChat(Connection conn, long companyId, String groupName) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_im_group (company_id, group_id, group_name, latest_msg_seq, latest_msg_time) VALUES (?, NULL, ?, 0, 0); ", Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, groupName);
			
			pstmt.executeUpdate();
			
			rs = pstmt.getGeneratedKeys();
			
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				throw new RuntimeException("cannot insert group chat");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static long getGroupLatestMsgSeq(Connection conn, long companyId, long groupId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT latest_msg_seq FROM weizhu_im_group WHERE company_id = ? AND group_id = ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, groupId);
 
			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				return rs.getLong("latest_msg_seq");
			} else {
				throw new RuntimeException("cannot get latest_msg_seq");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static boolean updateGroupName(Connection conn, long companyId, long groupId, String groupName) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_im_group SET group_name = ? WHERE company_id = ? AND group_id = ?; ");
			pstmt.setString(1, groupName);
			pstmt.setLong(2, companyId);
			pstmt.setLong(3, groupId);
			
			return pstmt.executeUpdate() > 0;
		} finally {
			DBUtil.closeQuietly(pstmt);
		}	
	}
	
	public static void updateGroupMember(Connection conn, long companyId, long groupId, 
			Collection<Long> joinUserIds, Collection<Long> leaveUserIds, long msgSeq) throws SQLException {
		if (joinUserIds.isEmpty() && leaveUserIds.isEmpty()) {
			return;
		}
		PreparedStatement pstmt = null;
		try {
			
			if (!joinUserIds.isEmpty()) {
				pstmt = conn.prepareStatement("INSERT IGNORE INTO weizhu_im_group_member (company_id, group_id, user_id, msg_seq, is_join) VALUES (?, ?, ?, ?, ?); ");
				
				for (Long joinUserId : joinUserIds) {
					pstmt.setLong(1, companyId);
					pstmt.setLong(2, groupId);
					pstmt.setLong(3, joinUserId);
					pstmt.setLong(4, msgSeq);
					pstmt.setBoolean(5, true);
					pstmt.addBatch();
				}
				
				pstmt.executeBatch();
				
				DBUtil.closeQuietly(pstmt);
				pstmt = null;
			}
			
			pstmt = conn.prepareStatement("UPDATE weizhu_im_group_member SET msg_seq = ?, is_join = ? WHERE company_id = ? AND group_id = ? AND user_id = ? AND msg_seq < ?; ");
		
			for (Long joinUserId : joinUserIds) {
				pstmt.setLong(1, msgSeq);
				pstmt.setBoolean(2, true);
				pstmt.setLong(3, companyId);
				pstmt.setLong(4, groupId);
				pstmt.setLong(5, joinUserId);
				pstmt.setLong(6, msgSeq);
				pstmt.addBatch();
			}
			
			for (Long leaveUserId : leaveUserIds) {
				pstmt.setLong(1, msgSeq);
				pstmt.setBoolean(2, false);
				pstmt.setLong(3, companyId);
				pstmt.setLong(4, groupId);
				pstmt.setLong(5, leaveUserId);
				pstmt.setLong(6, msgSeq);
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final String INSERT_GROUP_MESSAGE_SQL = 
			"INSERT INTO weizhu_im_group_msg (company_id, group_id, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, "
			+ "group_group_name, group_join_user_id, group_leave_user_id, discover_item_item_id, video_name, video_type, video_size, video_time, video_image_name) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); "
			+ "UPDATE weizhu_im_group SET latest_msg_seq = ?, latest_msg_time = ? WHERE company_id = ? AND group_id = ? AND latest_msg_seq < ?; ";
	
	public static boolean insertGroupMessage(Connection conn, long companyId, long groupId, IMProtos.InstantMessage msg) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(INSERT_GROUP_MESSAGE_SQL);
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, groupId);
			pstmt.setLong(3, msg.getMsgSeq());
			pstmt.setInt(4, msg.getMsgTime());
			pstmt.setLong(5, msg.getFromUserId());
			
			DBUtil.set(pstmt, 6, msg.hasText(), msg.getText().getContent());
			DBUtil.set(pstmt, 7, msg.hasVoice(), msg.getVoice().getData());
			DBUtil.set(pstmt, 8, msg.hasVoice(), msg.getVoice().getDuration());
			DBUtil.set(pstmt, 9, msg.hasImage(), msg.getImage().getName());
			DBUtil.set(pstmt, 10, msg.hasUser(), msg.getUser().getUserId());

			// for group
			DBUtil.set(pstmt, 11, msg.hasGroup() && msg.getGroup().hasGroupName(), 
					msg.getGroup().getGroupName());
			DBUtil.set(pstmt, 12, msg.hasGroup(), 
					msg.getGroup().getJoinUserIdCount() <= 0 ? "" : 
						DBUtil.COMMA_JOINER.join(msg.getGroup().getJoinUserIdList()));
			DBUtil.set(pstmt, 13, msg.hasGroup(), 
					msg.getGroup().getLeaveUserIdCount() <= 0 ? "" : 
						DBUtil.COMMA_JOINER.join(msg.getGroup().getLeaveUserIdList()));
			
			DBUtil.set(pstmt, 14, msg.hasDiscoverItem(), msg.getDiscoverItem().getItemId());
			
			DBUtil.set(pstmt, 15, msg.hasVideo(), msg.getVideo().getName());
			DBUtil.set(pstmt, 16, msg.hasVideo(), msg.getVideo().getType());
			DBUtil.set(pstmt, 17, msg.hasVideo(), msg.getVideo().getSize());
			DBUtil.set(pstmt, 18, msg.hasVideo(), msg.getVideo().getTime());
			DBUtil.set(pstmt, 19, msg.hasVideo(), msg.getVideo().getImageName());
			
			pstmt.setLong(20, msg.getMsgSeq());
			pstmt.setInt(21, msg.getMsgTime());
			pstmt.setLong(22, companyId);
			pstmt.setLong(23, groupId);
			pstmt.setLong(24, msg.getMsgSeq());
			
			pstmt.execute();
			
			return pstmt.getUpdateCount() > 0;
			
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<IMProtos.InstantMessage> getGroupMessage(Connection conn, long companyId, long groupId, 
			@Nullable Long msgSeqBegin, @Nullable Long msgSeqEnd, int size) throws SQLException {
		if (size <= 0 || (msgSeqBegin != null && msgSeqEnd != null && msgSeqBegin - msgSeqEnd - 1 <= 0)) {
			return Collections.emptyList();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT msg_seq, msg_time, from_user_id, text_content as `text.content`, voice_data as `voice.data`, voice_duration as `voice.duration`, image_name as `image.name`, user_user_id as `user.user_id`, ");
		sql.append("group_group_name, group_join_user_id, group_leave_user_id, discover_item_item_id as `discover_item.item_id`, video_name as `video.name`, video_type as `video.type`, video_size as `video.size`, video_time as `video.time`, video_image_name as `video.image_name` FROM weizhu_im_group_msg ");
		sql.append("WHERE company_id = ").append(companyId).append(" AND group_id = ").append(groupId);
		if (msgSeqBegin != null) {
			sql.append(" AND msg_seq < ").append(msgSeqBegin);
		}
		if (msgSeqEnd != null) {
			sql.append(" AND msg_seq > ").append(msgSeqEnd);
		}
		sql.append(" ORDER BY msg_seq DESC LIMIT ").append(size);
		sql.append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			List<IMProtos.InstantMessage> list = new ArrayList<IMProtos.InstantMessage>(size);
			
			IMProtos.InstantMessage.Builder tmpBuilder = IMProtos.InstantMessage.newBuilder();
			IMProtos.InstantMessage.Group.Builder tmpGroupBuilder = IMProtos.InstantMessage.Group.newBuilder();
			
			while (rs.next()) {
				tmpBuilder.clear();
				INSTANT_MESSAGE_MAPPER.mapToItem(rs, tmpBuilder);
				
				tmpGroupBuilder.clear();
				String groupName = rs.getString("group_group_name");
				if (groupName != null) {
					tmpGroupBuilder.setGroupName(groupName);
				}
				String groupJoinUserIdStr = rs.getString("group_join_user_id");
				if (groupJoinUserIdStr != null) {
					Iterable<String> iterable = DBUtil.COMMA_SPLITTER.split(groupJoinUserIdStr);
					for (String userIdStr : iterable) {
						try {
							tmpGroupBuilder.addJoinUserId(Long.parseLong(userIdStr));
						} catch (NumberFormatException e) {
							// ignore
						}
					}
				}
				String groupLeaveUserIdStr = rs.getString("group_leave_user_id");
				if (groupLeaveUserIdStr != null) {
					Iterable<String> iterable = DBUtil.COMMA_SPLITTER.split(groupLeaveUserIdStr);
					for (String userIdStr : iterable) {
						try {
							tmpGroupBuilder.addLeaveUserId(Long.parseLong(userIdStr));
						} catch (NumberFormatException e) {
							// ignore
						}
					}
				}
				
				if (groupJoinUserIdStr != null && groupLeaveUserIdStr != null && tmpGroupBuilder.isInitialized()) {
					tmpBuilder.setGroup(tmpGroupBuilder.build());
				}
				
				list.add(tmpBuilder.build());
			}
			
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
}
