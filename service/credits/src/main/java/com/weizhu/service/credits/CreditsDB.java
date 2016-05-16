package com.weizhu.service.credits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AdminCreditsProtos;
import com.weizhu.proto.AdminCreditsProtos.CreditsOperation;
import com.weizhu.proto.AdminCreditsProtos.GetCreditsLogResponse;
import com.weizhu.proto.AdminCreditsProtos.UserCreditsDelta;
import com.weizhu.proto.CreditsProtos;

public class CreditsDB {
	
	private static final ProtobufMapper<GetCreditsLogResponse.CreditsLog> CREDITS_LOG_MAPPER = 
			ProtobufMapper.createMapper(GetCreditsLogResponse.CreditsLog.getDefaultInstance(), 
					"credits_delta",
					"desc",
					"create_time",
					"create_admin");
	private static final ProtobufMapper<CreditsProtos.Credits> CREDITS_MAPPER = 
			ProtobufMapper.createMapper(CreditsProtos.Credits.getDefaultInstance(),
					"user_id",
					"credits");
	private static final ProtobufMapper<CreditsProtos.CreditsOrder> CREDITS_ORDER_MAPPER = 
			ProtobufMapper.createMapper(CreditsProtos.CreditsOrder.getDefaultInstance(),
					"order_id",
					"user_id",
					"type",
					"credits_delta",
					"desc",
					"state",
					"create_time",
					"create_admin");
	private static final ProtobufMapper<AdminCreditsProtos.CreditsOperation> CREDITS_ORDER_OPERATION =
			ProtobufMapper.createMapper(AdminCreditsProtos.CreditsOperation.getDefaultInstance(),
					"operation_id",
					"desc",
					"create_time",
					"create_admin");

	/**
	 * 获取公司所剩余的积分
	 * @param conn
	 * @param companyId
	 * @return
	 * @throws SQLException
	 */
	public static long getCredits(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT credits FROM weizhu_credits WHERE company_id = ?; ");
			pstmt.setLong(1, companyId);
			
			rs = pstmt.executeQuery();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get credits, companyId=" + companyId);
			}
			
			return rs.getLong("credits");
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新公司积分
	 * @param conn
	 * @param companyId
	 * @param credits
	 * @throws SQLException
	 */
	public static void updateCredits(Connection conn, long companyId, long credits) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_credits SET credits = ? WHERE company_id = ?; ");
			pstmt.setLong(1, credits);
			pstmt.setLong(2, companyId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新积分日志
	 * @param conn
	 * @param companyId    公司id
	 * @param creditsDelta 改变积分
	 * @param desc         描述
	 * @param adminId      管理员id
	 * @param now          创建时间
	 * @throws SQLException
	 */
	public static void insertCreditsLog(Connection conn, long companyId, 
			long creditsDelta, @Nullable String desc, 
			long adminId, int now) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_credits_log (company_id, credits_delta, `desc`, create_admin, create_time) VALUES (?, ?, ?, ?, ?); ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, creditsDelta);
			DBUtil.set(pstmt, 3, desc);
			DBUtil.set(pstmt, 4, adminId);
			DBUtil.set(pstmt, 5, now);
		
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取积分日志
	 * @param conn
	 * @param companyId
	 * @param start
	 * @param length
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<GetCreditsLogResponse.CreditsLog> getCreditsLog(Connection conn, long companyId, int start, int length) throws SQLException {
		if (length == 0) {
			return new DataPage<GetCreditsLogResponse.CreditsLog>(Lists.newArrayList(), 0, 0);
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT count(1) AS total FROM weizhu_credits_log WHERE company_id = ?; SELECT * FROM weizhu_credits_log WHERE company_id = ? ORDER BY log_id LIMIT ?, ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setLong(2, companyId);
			pstmt.setInt(3, start);
			pstmt.setInt(4, length);
			
			pstmt.execute();
			rs = pstmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total num");
			}
			
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			GetCreditsLogResponse.CreditsLog.Builder creditsLogBuilder = GetCreditsLogResponse.CreditsLog.newBuilder();
			List<GetCreditsLogResponse.CreditsLog> creditsLogList = new ArrayList<GetCreditsLogResponse.CreditsLog>();
			while (rs.next()) {
				creditsLogBuilder.clear();
				
				CREDITS_LOG_MAPPER.mapToItem(rs, creditsLogBuilder);
				
				rs.getString("desc");
				if (rs.wasNull()) {
					creditsLogBuilder.setDesc("");
				}
				
				creditsLogList.add(creditsLogBuilder.build());
			}
			
			return new DataPage<GetCreditsLogResponse.CreditsLog>(creditsLogList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取用户积分
	 * @param conn
	 * @param companyId
	 * @param userIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Long, CreditsProtos.Credits> getUserCredits(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_credits_user WHERE company_id = ");
		sql.append(companyId).append(" AND user_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(userIds)).append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			CreditsProtos.Credits.Builder creditsBuilder = CreditsProtos.Credits.newBuilder();
			Map<Long, CreditsProtos.Credits> creditsMap = new HashMap<Long, CreditsProtos.Credits>();
			while (rs.next()) {
				creditsBuilder.clear();
				
				CREDITS_MAPPER.mapToItem(rs, creditsBuilder);
				
				long userId = rs.getLong("user_id");
				
				creditsMap.put(userId, creditsBuilder.build());
			}
			
			return creditsMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 创建订单（用户创建订单，管理员创建订单（修改用户的积分））
	 * @param conn             数据库连接
	 * @param companyId        公司id
	 * @param creditsOrderList 订单列表
	 * @param now              创建订单的时间
	 * @param adminId          创建订单的管理员id（微助 admin_id = 0）
	 * @param orderNum         第三方订单序号
	 * @return List<Integer>   order_id 列表  
	 * @throws SQLException
	 */
	public static List<Integer> insertCreditsOrder(Connection conn, long companyId, Collection<CreditsProtos.CreditsOrder> creditsOrders, int now, long adminId, @Nullable String orderNum, @Nullable Integer operationId, @Nullable String param) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_credits_order (company_id, user_id, type, credits_delta, `desc`, state, create_time, create_admin, order_num, operation_id, params) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			for (CreditsProtos.CreditsOrder creditsOrder : creditsOrders) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, creditsOrder.getUserId());
				DBUtil.set(pstmt, 3, creditsOrder.getType().name());
				DBUtil.set(pstmt, 4, creditsOrder.getCreditsDelta());
				DBUtil.set(pstmt, 5, creditsOrder.getDesc());
				DBUtil.set(pstmt, 6, creditsOrder.getState().name());
				DBUtil.set(pstmt, 7, now);
				DBUtil.set(pstmt, 8, adminId);
				DBUtil.set(pstmt, 9, orderNum);
				DBUtil.set(pstmt, 10, operationId);
				DBUtil.set(pstmt, 11, param);
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> orderIdList = new ArrayList<Integer>();
			while (rs.next()) {
				orderIdList.add(rs.getInt(1));
			}
			
			return orderIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新用户积分
	 * @param conn
	 * @param companyId
	 * @param userCredits 用户积分
	 * @throws SQLException
	 */
	public static void updateUserCredits(Connection conn, long companyId, Collection<CreditsProtos.Credits> userCredits) throws SQLException {
		if (userCredits.isEmpty()) {
			return;
		}
		
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_credits_user (company_id, user_id, credits) VALUES (?, ?, ?);");
			for (CreditsProtos.Credits credits : userCredits) {
				pstmt.setLong(1, companyId);
				pstmt.setLong(2, credits.getUserId());
				pstmt.setLong(3, credits.getCredits());
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 清空用户积分
	 * @param conn
	 * @param companyId
	 * @param userIds
	 * @throws SQLException
	 */
	public static void clearUserCredits(Connection conn, long companyId, Collection<Long> userIds) throws SQLException {
		if (userIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_credits_user WHERE company_id = ").append(companyId);
		sql.append(" AND user_id IN (").append(DBUtil.COMMA_JOINER.join(userIds)).append("); ");

		Statement stmt = null;
		
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		
	}
	
	/**
	 * 根据Duiba订单获取用户订单
	 * @param conn
	 * @param orderNums Duiba订单编号
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, CreditsProtos.CreditsOrder> getCreditsOrderByOrderNum(Connection conn, Collection<String> orderNums) throws SQLException {
		if (orderNums.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_credits_order WHERE order_num IN (");
		DBUtil.QUOTE_COMMA_JOINER.appendTo(sql, Iterables.transform(orderNums, DBUtil.SQL_STRING_ESCAPER.asFunction()));
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder();
			Map<String, CreditsProtos.CreditsOrder> creditsOrderMap = new HashMap<String, CreditsProtos.CreditsOrder>();
			while (rs.next()) {
				creditsOrderBuilder.clear();
				
				CREDITS_ORDER_MAPPER.mapToItem(rs, creditsOrderBuilder);
				
				String orderNum = rs.getString("order_num");
				
				creditsOrderMap.put(orderNum, creditsOrderBuilder.build());
			}
			
			return creditsOrderMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取订单详情(客户端)
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param isExpense   是否是消费订单
	 * @param lastOrderId 上一条订单id
	 * @param lastTime    上一条订单创建时间
	 * @param size        获取数量+1，用来判断是否还有更多
	 * @return
	 * @throws SQLException
	 */
	public static List<CreditsProtos.CreditsOrder> getCreditsOrder(Connection conn, long companyId, long userId, 
			boolean isExpense, 
			@Nullable Integer lastOrderId, @Nullable Integer lastTime, 
			int size) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_credits_order WHERE company_id = ").append(companyId);
		sql.append(" AND user_id = ").append(userId);
		
		if (lastOrderId != null && lastTime != null) {
			sql.append(" AND order_id < ").append(lastOrderId);
			sql.append(" AND create_time <= ").append(lastTime);
		}
		
		if (isExpense) {
			sql.append(" AND type = 'EXPENSE'");
		}
		
		sql.append(" ORDER BY order_id DESC LIMIT ").append(size);
		sql.append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder();
			List<CreditsProtos.CreditsOrder> creditsOrderList = new ArrayList<CreditsProtos.CreditsOrder>();
			while (rs.next()) {
				creditsOrderBuilder.clear();
				
				CREDITS_ORDER_MAPPER.mapToItem(rs, creditsOrderBuilder);
				
				if (isExpense) {
					creditsOrderBuilder.setCreditsDelta(0 - rs.getLong("credits_delta"));
				}

				creditsOrderList.add(creditsOrderBuilder.build());
			}
			
			return creditsOrderList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取订单详情(服务端)
	 * @param conn
	 * @param companyId
	 * @param userIds
	 * @param isExpense 是否是消费订单
	 * @param startTime 查询开始时间
	 * @param endTime   查询结束时间
	 * @param start     请求开始数量
	 * @param length    请求数量
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<CreditsProtos.CreditsOrder> getCreditsOrderPage(Connection conn, long companyId, Collection<Long> userIds, 
			boolean isExpense, 
			@Nullable Integer startTime, @Nullable Integer endTime,
			int start, int length) throws SQLException {
		
		StringBuilder condition = new StringBuilder();
		condition.append(" company_id = ").append(companyId);
		
		if (startTime != null) {
			condition.append(" AND time >= ").append(startTime);
		}
		if (endTime != null) {
			condition.append(" AND time < ").append(endTime);
		}
		if (isExpense) {
			condition.append(" AND type = 'EXPENSE'");
		}
		if (!userIds.isEmpty()) {
			condition.append(" AND user_id IN (").append(DBUtil.COMMA_JOINER.join(userIds)).append(")");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) AS total FROM weizhu_credits_order WHERE");
		sql.append(condition);
		sql.append("; ");
		sql.append("SELECT * FROM weizhu_credits_order WHERE");
		sql.append(condition);
		sql.append(" ORDER BY order_id DESC LIMIT ").append(start).append(", ").append(length);
		sql.append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total");
			}
			
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			CreditsProtos.CreditsOrder.Builder creditsOrderBuilder = CreditsProtos.CreditsOrder.newBuilder();
			List<CreditsProtos.CreditsOrder> creditsOrderList = new ArrayList<CreditsProtos.CreditsOrder>();
			while (rs.next()) {
				creditsOrderBuilder.clear();
				
				CREDITS_ORDER_MAPPER.mapToItem(rs, creditsOrderBuilder);
				
				if (isExpense) {
					creditsOrderBuilder.setCreditsDelta(0 - rs.getLong("credits_delta"));
				}

				creditsOrderList.add(creditsOrderBuilder.build());
			}
			
			return new DataPage<CreditsProtos.CreditsOrder>(creditsOrderList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 更新订单状态
	 * @param conn
	 * @param companyId
	 * @param orderNum  Duiba订单号
	 * @param state     订单状态
	 * @throws SQLException
	 */
	public static void updateCreditsOrder(Connection conn, long companyId, String orderNum, CreditsProtos.CreditsOrder.State state) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_credits_order SET state = ? WHERE order_num = ?; ");
			pstmt.setString(1, state.name());
			pstmt.setString(2, orderNum);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取用户积分（服务端）
	 * @param conn
	 * @param companyId
	 * @param userIds 
	 * @param start   开始请求标志
	 * @param length  请求数量
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<CreditsProtos.Credits> getCreditsPage(Connection conn, long companyId, Collection<Long> userIds, int start, int length) throws SQLException {
		StringBuilder condition = null;
		if (!userIds.isEmpty()) {
			condition = new StringBuilder();
			condition.append(" AND user_id IN (").append(DBUtil.COMMA_JOINER.join(userIds)).append(") ");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(1) AS total FROM weizhu_credits_user WHERE company_id = ").append(companyId);
		if (condition != null) {
			sql.append(condition);
		}
		sql.append("; ");
		sql.append("SELECT * FROM weizhu_credits_user WHERE company_id = ").append(companyId);
		if (condition != null) {
			sql.append(condition);
		}
		sql.append(" LIMIT ");
		sql.append(start).append(", ");
		sql.append(length).append("; ");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total num");
			}
			
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			CreditsProtos.Credits.Builder creditsBuilder = CreditsProtos.Credits.newBuilder();
			List<CreditsProtos.Credits> creditsList = new ArrayList<CreditsProtos.Credits>();
			while (rs.next()) {
				creditsBuilder.clear();
				
				CREDITS_MAPPER.mapToItem(rs, creditsBuilder);
				
				creditsList.add(creditsBuilder.build());
			}
			
			return new DataPage<CreditsProtos.Credits>(creditsList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取积分规则
	 * @param conn
	 * @param companyId
	 * @return
	 * @throws SQLException
	 */
	public static String getCreditsRule(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT rule FROM weizhu_credits_rule WHERE company_id = ?; ");
			pstmt.setLong(1, companyId);
			
			rs = pstmt.executeQuery();
			
			String creditsRule = "";
			if (rs.next()) {
				creditsRule = rs.getString("rule");
			}
			
			return creditsRule;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新积分规则
	 * @param conn
	 * @param companyId
	 * @param creditsRule 积分规则
	 * @throws SQLException
	 */
	public static void updateCreditsRule(Connection conn, long companyId, String creditsRule) throws SQLException {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_credits_rule (company_id, rule) VALUES (?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setString(2, creditsRule);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取管理员发放订单记录
	 * @param conn
	 * @param companyId
	 * @param start
	 * @param length
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<CreditsOperation> getCreditsOperation(Connection conn, long companyId, int start, int length) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_credits_operation; SELECT * FROM weizhu_credits_operation WHERE company_id = ? ORDER BY create_time DESC LIMIT ?, ?; ");
			pstmt.setLong(1, companyId);
			pstmt.setInt(2, start);
			pstmt.setInt(3, length);
			
			pstmt.execute();
			rs = pstmt.getResultSet();
			
			if (!rs.next()) {
				throw new RuntimeException("cannot get total");
			}
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			pstmt.getMoreResults();
			rs = pstmt.getResultSet();
			
			CreditsOperation.Builder operationBuilder = CreditsOperation.newBuilder();
			List<CreditsOperation> CreditsOperationList = new ArrayList<CreditsOperation>();
			while (rs.next()) {
				operationBuilder.clear();
				
				CREDITS_ORDER_OPERATION.mapToItem(rs, operationBuilder);
				
				CreditsOperationList.add(operationBuilder.build());
			}
			
			return new DataPage<CreditsOperation>(CreditsOperationList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 根据操作id，获取用户订单
	 * @param conn
	 * @param companyId
	 * @param operationIds 
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, List<UserCreditsDelta>> getUserCreditsDelta(Connection conn, long companyId, Collection<Integer> operationIds) throws SQLException {
		if (operationIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Statement stmt = null;
		ResultSet rs = null;
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT operation_id, user_id, credits_delta FROM weizhu_credits_order WHERE company_id = ").append(companyId);
		sql.append(" AND operation_id IN (").append(DBUtil.COMMA_JOINER.join(operationIds));
		sql.append("); ");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			
			Map<Integer, List<UserCreditsDelta>> userCreditsDeltaMap = new HashMap<Integer, List<UserCreditsDelta>>();
			List<UserCreditsDelta> userCreditsDeltaList = null;
			UserCreditsDelta.Builder creditsDeltaBuilder = UserCreditsDelta.newBuilder();
			while (rs.next()) {
				int operationId = rs.getInt("operation_id");
				
				userCreditsDeltaList = userCreditsDeltaMap.get(operationId);
				if (userCreditsDeltaList == null) {
					userCreditsDeltaList = new ArrayList<UserCreditsDelta>();
				}
				
				creditsDeltaBuilder.clear();
				
				userCreditsDeltaList.add(creditsDeltaBuilder
						.setCreditsDelta(rs.getLong("credits_delta"))
						.setUserId(rs.getLong("user_id"))
						.build());
				
				userCreditsDeltaMap.put(operationId, userCreditsDeltaList);
			}
			
			return userCreditsDeltaMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 创建操作员订单发放记录
	 * @param conn
	 * @param companyId
	 * @param desc         描述
	 * @param creditsTotal 总和
	 * @param now          创建时间
	 * @param adminId      创建管理员id
	 * @return
	 * @throws SQLException
	 */
	public static int insertCreditsOperation(Connection conn, long companyId, String desc, long creditsTotal, int now, long adminId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_credits_operation (company_id, `desc`, create_time, create_admin) VALUES (?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			pstmt.setLong(1, companyId);
			pstmt.setString(2, desc);
			pstmt.setInt(3, now);
			pstmt.setLong(4, adminId);
			
			pstmt.execute();
			
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("cannot get the key");
			}
			
			return rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取消费积分总和
	 * @param conn
	 * @param companyId
	 * @return
	 * @throws SQLException
	 */
	public static int getExpenseCredits(Connection conn, long companyId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = conn.prepareStatement("SELECT SUM(credits_delta) AS credits FROM weizhu_credits_order WHERE company_id = ? AND type = 'EXPENSE' AND state = 'SUCCESS'; ");
			pstmt.setLong(1, companyId);
			
			rs = pstmt.executeQuery();
			
			int credits = 0;
			if (rs.next()) {
				credits = rs.getInt("credits");
			}
			
			return credits;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
