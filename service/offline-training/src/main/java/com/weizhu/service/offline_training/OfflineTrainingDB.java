package com.weizhu.service.offline_training;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.OfflineTrainingProtos;

public class OfflineTrainingDB {

	private static final ProtobufMapper<OfflineTrainingProtos.Train> TRAIN_MAPPER = 
			ProtobufMapper.createMapper(OfflineTrainingProtos.Train.getDefaultInstance(),
					"train_id",
					"train_name",
					"image_name",
					"start_time",
					"end_time",
					"apply_enable",
					"apply_start_time",
					"apply_end_time",
					"apply_user_count",
					"apply_is_notify",
					"train_address",
					"lecturer_name",
					"check_in_start_time",
					"check_in_end_time",
					"arrangement_text",
					"describe_text",
					"allow_model_id",
					"state",
					"create_admin_id",
					"create_time",
					"update_admin_id",
					"update_time");
	
	public static Map<Integer, OfflineTrainingProtos.Train> getTrain(Connection conn, long companyId, 
			Collection<Integer> trainIds,
			@Nullable Collection<OfflineTrainingProtos.State> states
			) throws SQLException {
		if (trainIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		String trainIdStr = DBUtil.COMMA_JOINER.join(trainIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_train_lecturer_user WHERE company_id = ").append(companyId).append(" AND train_id IN (").append(trainIdStr).append(") ORDER BY train_id, user_id; ");
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_train_discover_item WHERE company_id = ").append(companyId).append(" AND train_id IN (").append(trainIdStr).append(") ORDER BY train_id, item_id; ");
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_train WHERE company_id = ").append(companyId).append(" AND train_id IN (").append(trainIdStr).append(")");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Integer, List<Long>> lecturerUserIdMap = new TreeMap<Integer, List<Long>>();
			while(rs.next()) {
				Integer trainId = rs.getInt("train_id");
				
				List<Long> userIdList = lecturerUserIdMap.get(trainId);
				if (userIdList == null) {
					userIdList = new ArrayList<Long>();
					lecturerUserIdMap.put(trainId, userIdList);
				}
				userIdList.add(rs.getLong("user_id"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, List<Long>> discoverItemIdMap = new TreeMap<Integer, List<Long>>();
			while(rs.next()) {
				Integer trainId = rs.getInt("train_id");
				
				List<Long> itemIdList = discoverItemIdMap.get(trainId);
				if (itemIdList == null) {
					itemIdList = new ArrayList<Long>();
					discoverItemIdMap.put(trainId, itemIdList);
				}
				itemIdList.add(rs.getLong("item_id"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, OfflineTrainingProtos.Train> trainMap = new TreeMap<Integer, OfflineTrainingProtos.Train>();
			
			OfflineTrainingProtos.Train.Builder tmpBuilder = OfflineTrainingProtos.Train.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				TRAIN_MAPPER.mapToItem(rs, tmpBuilder);
				
				List<Long> userIdList = lecturerUserIdMap.get(tmpBuilder.getTrainId());
				if (userIdList != null) {
					tmpBuilder.addAllLecturerUserId(userIdList);
				}
				
				List<Long> itemIdList = discoverItemIdMap.get(tmpBuilder.getTrainId());
				if (itemIdList != null) {
					tmpBuilder.addAllDiscoverItemId(itemIdList);
				}
				trainMap.put(tmpBuilder.getTrainId(), tmpBuilder.build());
			}
			
			return trainMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getTrainIdPage(Connection conn, long companyId, 
			int start, 
			int length,
			@Nullable Collection<OfflineTrainingProtos.State> totalStates
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		if (totalStates != null && totalStates.isEmpty()) {
			return new DataPage<Integer>(Collections.<Integer> emptyList(), 0, 0);
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ").append(companyId);
		if (totalStates != null) {
			whereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(whereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			whereBuilder.append("')");
		}
		final String where = whereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT train_id FROM weizhu_offline_training_train ");
		sqlBuilder.append(where).append(" ORDER BY train_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_offline_training_train ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<Integer> trainIdList = new ArrayList<Integer>();
			while (rs.next()) {
				trainIdList.add(rs.getInt("train_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<Integer>(trainIdList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<Integer> getTrainIdPage(Connection conn, long companyId, 
			int start, 
			int length,
			@Nullable Integer startTime,
			@Nullable Integer endTime,
			@Nullable Long createAdminId,
			@Nullable OfflineTrainingProtos.State state,
			@Nullable String trainName,
			@Nullable Collection<OfflineTrainingProtos.State> totalStates
			) throws SQLException {
		if (startTime == null && endTime == null && createAdminId == null && state == null && trainName == null) {
			return getTrainIdPage(conn, companyId, start, length, totalStates);
		}
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		if (totalStates != null && totalStates.isEmpty()) {
			return new DataPage<Integer>(Collections.<Integer>emptyList(), 0, 0);
		}
		
		StringBuilder totalWhereBuilder = new StringBuilder();
		totalWhereBuilder.append("WHERE company_id = ").append(companyId);
		if (totalStates != null) {
			totalWhereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(totalWhereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			totalWhereBuilder.append("')");
		}
		final String totalWhere = totalWhereBuilder.toString();
		
		StringBuilder filterWhereBuilder = new StringBuilder();
		filterWhereBuilder.append("WHERE company_id = ").append(companyId);
		if (startTime != null) {
			filterWhereBuilder.append(" AND end_time > ").append(startTime);
		}
		if (endTime != null) {
			filterWhereBuilder.append(" AND start_time < ").append(endTime);
		}
		if (createAdminId != null) {
			filterWhereBuilder.append(" AND create_admin_id = ").append(createAdminId);
		}
		if (state != null) {
			filterWhereBuilder.append(" AND state = '");
			filterWhereBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name())).append("'");
		}
		if (trainName != null) {
			filterWhereBuilder.append(" AND train_name LIKE '%");
			filterWhereBuilder.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(trainName)).append("%'");
		}
		if (totalStates != null){
			filterWhereBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(filterWhereBuilder, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			filterWhereBuilder.append("')");
		}
		final String filterWhere = filterWhereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT train_id FROM weizhu_offline_training_train ");
		sqlBuilder.append(filterWhere).append(" ORDER BY train_id DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS filtered_size FROM weizhu_offline_training_train ");
		sqlBuilder.append(filterWhere).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_offline_training_train ");
		sqlBuilder.append(totalWhere).append("; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<Integer> trainIdList = new ArrayList<Integer>();
			while (rs.next()) {
				trainIdList.add(rs.getInt("train_id"));
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int filteredSize;
			if (rs.next()) {
				filteredSize = rs.getInt("filtered_size");
			} else {
				throw new RuntimeException("cannot get filtered size");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<Integer>(trainIdList, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static OfflineTrainingDAOProtos.TrainIndexList getOpenTrainIndexList(Connection conn, long companyId, 
			int now, int size, 
			@Nullable OfflineTrainingDAOProtos.TrainIndex offsetIndex, 
			@Nullable Collection<OfflineTrainingProtos.State> states
			) throws SQLException {
		if (size <= 0 || (states != null && states.isEmpty())) {
			return OfflineTrainingDAOProtos.TrainIndexList.getDefaultInstance();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT train_id, start_time, end_time FROM weizhu_offline_training_train WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND end_time > ").append(now);
		if (offsetIndex != null) {
			sqlBuilder.append(" AND (start_time > ");
			sqlBuilder.append(offsetIndex.getStartTime()).append(" OR (start_time = ");
			sqlBuilder.append(offsetIndex.getStartTime()).append(" AND train_id > ");
			sqlBuilder.append(offsetIndex.getTrainId()).append("))");
		}
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append(" ORDER BY start_time ASC, train_id ASC LIMIT ");
		sqlBuilder.append(size).append("; ");
		
		sqlBuilder.append("SELECT MIN(end_time) AS recent_end_time FROM weizhu_offline_training_train WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND end_time > ").append(now);
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			OfflineTrainingDAOProtos.TrainIndexList.Builder listBuilder = OfflineTrainingDAOProtos.TrainIndexList.newBuilder();
			OfflineTrainingDAOProtos.TrainIndex.Builder tmpIndexBuilder = OfflineTrainingDAOProtos.TrainIndex.newBuilder();
			while (rs.next()) {
				tmpIndexBuilder.clear();
				
				tmpIndexBuilder.setTrainId(rs.getInt("train_id"));
				tmpIndexBuilder.setStartTime(rs.getInt("start_time"));
				tmpIndexBuilder.setEndTime(rs.getInt("end_time"));
				listBuilder.addTrainIndex(tmpIndexBuilder.build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			// 获取最近结束的培训时间, 设置为此列表的失效时间
			if (rs.next()) {
				listBuilder.setExpiredTime(rs.getInt("recent_end_time"));
			}
			
			return listBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static OfflineTrainingDAOProtos.TrainIndexList getClosedTrainIndexList(Connection conn, long companyId, 
			int now, int size, 
			@Nullable OfflineTrainingDAOProtos.TrainIndex offsetIndex, 
			@Nullable Collection<OfflineTrainingProtos.State> states
			) throws SQLException {
		if (size <= 0 || (states != null && states.isEmpty())) {
			return OfflineTrainingDAOProtos.TrainIndexList.getDefaultInstance();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT train_id, start_time, end_time FROM weizhu_offline_training_train WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND end_time <= ").append(now);
		if (offsetIndex != null) {
			sqlBuilder.append(" AND (end_time < ");
			sqlBuilder.append(offsetIndex.getEndTime()).append(" OR (end_time = ");
			sqlBuilder.append(offsetIndex.getEndTime()).append(" AND train_id < ");
			sqlBuilder.append(offsetIndex.getTrainId()).append("))");
		}
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append(" ORDER BY end_time DESC, train_id DESC LIMIT ");
		sqlBuilder.append(size).append("; ");
		
		sqlBuilder.append("SELECT MIN(end_time) AS recent_end_time FROM weizhu_offline_training_train WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND end_time > ").append(now);
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			OfflineTrainingDAOProtos.TrainIndexList.Builder listBuilder = OfflineTrainingDAOProtos.TrainIndexList.newBuilder();
			OfflineTrainingDAOProtos.TrainIndex.Builder tmpIndexBuilder = OfflineTrainingDAOProtos.TrainIndex.newBuilder();
			while (rs.next()) {
				tmpIndexBuilder.clear();
				
				tmpIndexBuilder.setTrainId(rs.getInt("train_id"));
				tmpIndexBuilder.setStartTime(rs.getInt("start_time"));
				tmpIndexBuilder.setEndTime(rs.getInt("end_time"));
				listBuilder.addTrainIndex(tmpIndexBuilder.build());
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			// 获取最近结束的培训时间, 设置为此列表的失效时间
			if (rs.next()) {
				listBuilder.setExpiredTime(rs.getInt("recent_end_time"));
			}
			
			return listBuilder.build();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, List<Integer>> getTrainIdListByNotApplyNotify(Connection conn, @Nullable Collection<OfflineTrainingProtos.State> states) throws SQLException {
		if (states != null && states.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT company_id, train_id FROM weizhu_offline_training_train WHERE apply_enable = 1 AND apply_is_notify = 0");
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append(" ORDER BY company_id, train_id; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<Integer>> resultMap = new TreeMap<Long, List<Integer>>();
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				int trainId = rs.getInt("train_id");
				
				List<Integer> trainIdList = resultMap.get(companyId);
				if (trainIdList == null) {
					trainIdList = new ArrayList<Integer>();
					resultMap.put(companyId, trainIdList);
				}
				trainIdList.add(trainId);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Long, List<Integer>> getTrainIdListByStartTime(Connection conn,
			int startStartTime, int endStartTime, 
			@Nullable Collection<OfflineTrainingProtos.State> states
			) throws SQLException {
		if ((startStartTime >= endStartTime) || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT company_id, train_id FROM weizhu_offline_training_train WHERE start_time >= ");
		sqlBuilder.append(startStartTime).append(" AND start_time < ").append(endStartTime);
		if (states != null) {
			sqlBuilder.append(" AND state IN ('");
			DBUtil.QUOTE_COMMA_JOINER.appendTo(sqlBuilder, Iterables.transform(Iterables.transform(states, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
			sqlBuilder.append("')");
		}
		sqlBuilder.append(" ORDER BY company_id, train_id; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<Integer>> resultMap = new TreeMap<Long, List<Integer>>();
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				int trainId = rs.getInt("train_id");
				
				List<Integer> trainIdList = resultMap.get(companyId);
				if (trainIdList == null) {
					trainIdList = new ArrayList<Integer>();
					resultMap.put(companyId, trainIdList);
				}
				trainIdList.add(trainId);
			}
			return resultMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertTrain(Connection conn, long companyId, OfflineTrainingProtos.Train train) throws SQLException {
		int trainId;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_offline_training_train (company_id, train_id, "
					+ "train_name, image_name, start_time, end_time, "
					+ "apply_enable, apply_start_time, apply_end_time, apply_user_count, apply_is_notify, "
					+ "train_address, lecturer_name, check_in_start_time, check_in_end_time, "
					+ "arrangement_text, describe_text, allow_model_id, "
					+ "state, create_time, create_admin_id) VALUES (?, NULL, "
					+ "?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, "
					+ "?, ?, ?, "
					+ "?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			DBUtil.set(pstmt, 1, companyId);
			
			DBUtil.set(pstmt, 2, train.hasTrainName(), train.getTrainName());
			DBUtil.set(pstmt, 3, train.hasImageName(), train.getImageName());
			DBUtil.set(pstmt, 4, train.hasStartTime(), train.getStartTime());
			DBUtil.set(pstmt, 5, train.hasEndTime(), train.getEndTime());
			
			DBUtil.set(pstmt, 6, train.hasApplyEnable(), train.getApplyEnable());
			DBUtil.set(pstmt, 7, train.hasApplyStartTime(), train.getApplyStartTime());
			DBUtil.set(pstmt, 8, train.hasApplyEndTime(), train.getApplyEndTime());
			DBUtil.set(pstmt, 9, train.hasApplyUserCount(), train.getApplyUserCount());
			DBUtil.set(pstmt, 10, train.hasApplyIsNotify(), train.getApplyIsNotify());
			
			DBUtil.set(pstmt, 11, train.hasTrainAddress(), train.getTrainAddress());
			DBUtil.set(pstmt, 12, train.hasLecturerName(), train.getLecturerName());
			DBUtil.set(pstmt, 13, train.hasCheckInStartTime(), train.getCheckInStartTime());
			DBUtil.set(pstmt, 14, train.hasCheckInEndTime(), train.getCheckInEndTime());
			
			DBUtil.set(pstmt, 15, train.hasArrangementText(), train.getArrangementText());
			DBUtil.set(pstmt, 16, train.hasDescribeText(), train.getDescribeText());
			DBUtil.set(pstmt, 17, train.hasAllowModelId(), train.getAllowModelId());
			
			DBUtil.set(pstmt, 18, train.hasState(), train.getState());
			DBUtil.set(pstmt, 19, train.hasCreateTime(), train.getCreateTime());
			DBUtil.set(pstmt, 20, train.hasCreateAdminId(), train.getCreateAdminId());
			
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				trainId = rs.getInt(1);
			} else {
				throw new RuntimeException("cannot get generate train id");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isLecturerUserIdFirst = true;
		for (Long userId : train.getLecturerUserIdList()) {
			if (isLecturerUserIdFirst) {
				isLecturerUserIdFirst = false;
				sqlBuilder.append("INSERT IGNORE INTO weizhu_offline_training_train_lecturer_user (company_id, train_id, user_id) VALUES ");
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", ");
			sqlBuilder.append(trainId).append(", ");
			sqlBuilder.append(userId).append(") ");
		}
		if (!isLecturerUserIdFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDiscoverItemIdFirst = true;
		for (Long itemId : train.getDiscoverItemIdList()) {
			if (isDiscoverItemIdFirst) {
				isDiscoverItemIdFirst = false;
				sqlBuilder.append("INSERT IGNORE INTO weizhu_offline_training_train_discover_item (company_id, train_id, item_id) VALUES ");
			} else {
				sqlBuilder.append(", ");
			}
			sqlBuilder.append("(");
			sqlBuilder.append(companyId).append(", ");
			sqlBuilder.append(trainId).append(", ");
			sqlBuilder.append(itemId).append(") ");
		}
		if (!isDiscoverItemIdFirst) {
			sqlBuilder.append("; ");
		}
		
		if (sqlBuilder.length() > 0) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
		return trainId;
	}
	
	public static void updateTrain(Connection conn, long companyId, 
			OfflineTrainingProtos.Train oldTrain, 
			OfflineTrainingProtos.Train newTrain
			) throws SQLException {
		if (oldTrain.getTrainId() != newTrain.getTrainId()) {
			throw new RuntimeException("invalid train id");
		}
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_offline_training_train SET "
					+ "train_name = ?, image_name = ?, start_time = ?, end_time = ?, "
					+ "apply_enable = ?, apply_start_time = ?, apply_end_time = ?, apply_user_count = ?, apply_is_notify = ?, "
					+ "train_address = ?, lecturer_name = ?, check_in_start_time = ?, check_in_end_time = ?, "
					+ "arrangement_text = ?, describe_text = ?, allow_model_id = ?, "
					+ "state = ?, update_time = ?, update_admin_id = ? "
					+ "WHERE company_id = ? AND train_id = ?; ");
			
			DBUtil.set(pstmt, 1, newTrain.hasTrainName(), newTrain.getTrainName());
			DBUtil.set(pstmt, 2, newTrain.hasImageName(), newTrain.getImageName());
			DBUtil.set(pstmt, 3, newTrain.hasStartTime(), newTrain.getStartTime());
			DBUtil.set(pstmt, 4, newTrain.hasEndTime(), newTrain.getEndTime());
			
			DBUtil.set(pstmt, 5, newTrain.hasApplyEnable(), newTrain.getApplyEnable());
			DBUtil.set(pstmt, 6, newTrain.hasApplyStartTime(), newTrain.getApplyStartTime());
			DBUtil.set(pstmt, 7, newTrain.hasApplyEndTime(), newTrain.getApplyEndTime());
			DBUtil.set(pstmt, 8, newTrain.hasApplyUserCount(), newTrain.getApplyUserCount());
			DBUtil.set(pstmt, 9, newTrain.hasApplyIsNotify(), newTrain.getApplyIsNotify());
			
			DBUtil.set(pstmt, 10, newTrain.hasTrainAddress(), newTrain.getTrainAddress());
			DBUtil.set(pstmt, 11, newTrain.hasLecturerName(), newTrain.getLecturerName());
			DBUtil.set(pstmt, 12, newTrain.hasCheckInStartTime(), newTrain.getCheckInStartTime());
			DBUtil.set(pstmt, 13, newTrain.hasCheckInEndTime(), newTrain.getCheckInEndTime());
			
			DBUtil.set(pstmt, 14, newTrain.hasArrangementText(), newTrain.getArrangementText());
			DBUtil.set(pstmt, 15, newTrain.hasDescribeText(), newTrain.getDescribeText());
			DBUtil.set(pstmt, 16, newTrain.hasAllowModelId(), newTrain.getAllowModelId());
			
			DBUtil.set(pstmt, 17, newTrain.hasState(), newTrain.getState());
			DBUtil.set(pstmt, 18, newTrain.hasUpdateTime(), newTrain.getUpdateTime());
			DBUtil.set(pstmt, 19, newTrain.hasUpdateAdminId(), newTrain.getUpdateAdminId());
			
			DBUtil.set(pstmt, 20, companyId);
			DBUtil.set(pstmt, 21, newTrain.getTrainId());
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isInsertLectureUserIdFirst = true;
		for (Long newUserId : newTrain.getLecturerUserIdList()) {
			if (!oldTrain.getLecturerUserIdList().contains(newUserId)) {
				if (isInsertLectureUserIdFirst) {
					isInsertLectureUserIdFirst = false;
					sqlBuilder.append("INSERT IGNORE INTO weizhu_offline_training_train_lecturer_user (company_id, train_id, user_id) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(newTrain.getTrainId()).append(", ");
				sqlBuilder.append(newUserId).append(")");
			}
		}
		if (!isInsertLectureUserIdFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDeleteLectureUserIdFirst = true;
		for (Long oldUserId : oldTrain.getLecturerUserIdList()) {
			if (!newTrain.getLecturerUserIdList().contains(oldUserId)) {
				if (isDeleteLectureUserIdFirst) {
					isDeleteLectureUserIdFirst = false;
					sqlBuilder.append("DELETE FROM weizhu_offline_training_train_lecturer_user WHERE company_id = ").append(companyId).append(" AND train_id = ").append(oldTrain.getTrainId()).append(" AND user_id IN (");
				} else {
					sqlBuilder.append(", ");
				}
				sqlBuilder.append(oldUserId);
			}
		}
		if (!isDeleteLectureUserIdFirst) {
			sqlBuilder.append("); ");
		}
		
		boolean isInsertDiscoverItemIdFirst = true;
		for (Long newItemId : newTrain.getDiscoverItemIdList()) {
			if (!oldTrain.getDiscoverItemIdList().contains(newItemId)) {
				if (isInsertDiscoverItemIdFirst) {
					isInsertDiscoverItemIdFirst = false;
					sqlBuilder.append("INSERT IGNORE INTO weizhu_offline_training_train_discover_item (company_id, train_id, item_id) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(newTrain.getTrainId()).append(", ");
				sqlBuilder.append(newItemId).append(")");
			}
		}
		if (!isInsertDiscoverItemIdFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDeleteDiscoverItemIdFirst = true;
		for (Long oldItemId : oldTrain.getDiscoverItemIdList()) {
			if (!newTrain.getDiscoverItemIdList().contains(oldItemId)) {
				if (isDeleteDiscoverItemIdFirst) {
					isDeleteDiscoverItemIdFirst = false;
					sqlBuilder.append("DELETE FROM weizhu_offline_training_train_discover_item WHERE company_id = ").append(companyId).append(" AND train_id = ").append(oldTrain.getTrainId()).append(" AND item_id IN (");
				} else {
					sqlBuilder.append(", ");
				}
				sqlBuilder.append(oldItemId);
			}
		}
		if (!isDeleteDiscoverItemIdFirst) {
			sqlBuilder.append("); ");
		}
		
		if (sqlBuilder.length() > 0) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static void updateTrainState(Connection conn, long companyId, 
			Collection<Integer> trainIds, OfflineTrainingProtos.State newState
			) throws SQLException {
		if (trainIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_offline_training_train SET state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(newState.name())).append("' WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, trainIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static boolean updateTrainApplyIsNotify(Connection conn, long companyId, int trainId, boolean applyEnable, int applyStartTime, OfflineTrainingProtos.State state) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_offline_training_train SET apply_is_notify = 1 WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id = ");
		sqlBuilder.append(trainId).append(" AND apply_enable = ");
		sqlBuilder.append(applyEnable ? 1 : 0).append(" AND apply_start_time = ");
		sqlBuilder.append(applyStartTime).append(" AND apply_is_notify = 0 AND state = '");
		sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name())).append("'; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			return stmt.executeUpdate(sql) > 0;
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void addDiscoverItemTrain(Connection conn, long companyId, 
			int trainId, 
			List<Long> oldItemIdList, 
			List<Long> newItemIdList
			) throws SQLException {
		
		StringBuilder sqlBuilder = new StringBuilder();
		
		boolean isInsertDiscoverItemIdFirst = true;
		for (Long newItemId : newItemIdList) {
			if (!oldItemIdList.contains(newItemId)) {
				if (isInsertDiscoverItemIdFirst) {
					isInsertDiscoverItemIdFirst = false;
					sqlBuilder.append("INSERT IGNORE INTO weizhu_offline_training_train_discover_item (company_id, train_id, item_id) VALUES ");
				} else {
					sqlBuilder.append(", ");
				}
				
				sqlBuilder.append("(");
				sqlBuilder.append(companyId).append(", ");
				sqlBuilder.append(trainId).append(", ");
				sqlBuilder.append(newItemId).append(")");
			}
		}
		if (!isInsertDiscoverItemIdFirst) {
			sqlBuilder.append("; ");
		}
		
		boolean isDeleteDiscoverItemIdFirst = true;
		for (Long oldItemId : oldItemIdList) {
			if (!newItemIdList.contains(oldItemId)) {
				if (isDeleteDiscoverItemIdFirst) {
					isDeleteDiscoverItemIdFirst = false;
					sqlBuilder.append("DELETE FROM weizhu_offline_training_train_discover_item WHERE company_id = ").append(companyId).append(" AND train_id = ").append(trainId).append(" AND item_id IN (");
				} else {
					sqlBuilder.append(", ");
				}
				sqlBuilder.append(oldItemId);
			}
		}
		if (!isDeleteDiscoverItemIdFirst) {
			sqlBuilder.append("); ");
		}
		
		if (sqlBuilder.length() > 0) {
			final String sql = sqlBuilder.toString();
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
		}
	}
	
	public static Map<Integer, OfflineTrainingProtos.TrainCount> getTrainCount(Connection conn, long companyId, 
			Collection<Integer> trainIds
			) throws SQLException {
		if (trainIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String trainIdStr = DBUtil.COMMA_JOINER.join(trainIds);
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT train_id, COUNT(user_id) AS user_apply_count FROM weizhu_offline_training_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id IN (").append(trainIdStr).append(") AND is_apply = 1 GROUP BY train_id; ");
		sqlBuilder.append("SELECT train_id, COUNT(user_id) AS user_check_in_count FROM weizhu_offline_training_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id IN (").append(trainIdStr).append(") AND is_check_in = 1 GROUP BY train_id; ");
		sqlBuilder.append("SELECT train_id, COUNT(user_id) AS user_leave_count FROM weizhu_offline_training_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id IN (").append(trainIdStr).append(") AND is_leave = 1 GROUP BY train_id; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();
			
			Map<Integer, OfflineTrainingProtos.TrainCount.Builder> builderMap = new TreeMap<Integer, OfflineTrainingProtos.TrainCount.Builder>();
			for (Integer trainId : trainIds) {
				OfflineTrainingProtos.TrainCount.Builder builder = OfflineTrainingProtos.TrainCount.newBuilder();
				builder.setTrainId(trainId);
				builder.setUserAllowCount(0);
				builder.setUserApplyCount(0);
				builder.setUserCheckInCount(0);
				builder.setUserLeaveCount(0);
				builderMap.put(trainId, builder);
			}
			
			while (rs.next()) {
				OfflineTrainingProtos.TrainCount.Builder b = builderMap.get(rs.getInt("train_id"));
				if (b != null) {
					b.setUserApplyCount(rs.getInt("user_apply_count"));
				}
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			while (rs.next()) {
				OfflineTrainingProtos.TrainCount.Builder b = builderMap.get(rs.getInt("train_id"));
				if (b != null) {
					b.setUserCheckInCount(rs.getInt("user_check_in_count"));
				}
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			while (rs.next()) {
				OfflineTrainingProtos.TrainCount.Builder b = builderMap.get(rs.getInt("train_id"));
				if (b != null) {
					b.setUserLeaveCount(rs.getInt("user_leave_count"));
				}
			}
			
			Map<Integer, OfflineTrainingProtos.TrainCount> trainCountMap = new TreeMap<Integer, OfflineTrainingProtos.TrainCount>();
			for (Entry<Integer, OfflineTrainingProtos.TrainCount.Builder> entry : builderMap.entrySet()) {
				trainCountMap.put(entry.getKey(), entry.getValue().build());
			}
			return trainCountMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<OfflineTrainingProtos.TrainUser> TRAIN_USER_MAPPER = 
			ProtobufMapper.createMapper(OfflineTrainingProtos.TrainUser.getDefaultInstance(),
					"train_id",
					"user_id",
					"is_apply",
					"apply_time",
					"is_check_in",
					"check_in_time",
					"is_leave",
					"leave_time",
					"leave_reason",
					"update_time");
	
	public static Map<Integer, OfflineTrainingProtos.TrainUser> getTrainUser(Connection conn, long companyId, 
			long userId, 
			Collection<Integer> trainIds
			) throws SQLException {
		if (trainIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND user_id = ").append(userId).append(" AND train_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sqlBuilder, trainIds);
		sqlBuilder.append("); ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, OfflineTrainingProtos.TrainUser> userMap = new TreeMap<Integer, OfflineTrainingProtos.TrainUser>();
			OfflineTrainingProtos.TrainUser.Builder tmpBuilder = OfflineTrainingProtos.TrainUser.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				
				TRAIN_USER_MAPPER.mapToItem(rs, tmpBuilder);
				userMap.put(tmpBuilder.getTrainId(), tmpBuilder.build());
			}
			return userMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<OfflineTrainingProtos.TrainUser> getTrainUserPage(Connection conn, long companyId, 
			int trainId, 
			int start, 
			int length
			) throws SQLException {
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder whereBuilder = new StringBuilder();
		whereBuilder.append("WHERE company_id = ").append(companyId).append(" AND train_id = ").append(trainId);
		final String where = whereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_user ");
		sqlBuilder.append(where).append(" ORDER BY update_time DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_offline_training_user ");
		sqlBuilder.append(where).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<OfflineTrainingProtos.TrainUser> trainUserList = new ArrayList<OfflineTrainingProtos.TrainUser>();
			OfflineTrainingProtos.TrainUser.Builder tmpBuilder = OfflineTrainingProtos.TrainUser.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				trainUserList.add(TRAIN_USER_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<OfflineTrainingProtos.TrainUser>(trainUserList, totalSize, totalSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static DataPage<OfflineTrainingProtos.TrainUser> getTrainUserPage(Connection conn, long companyId, 
			int trainId, 
			int start,
			int length,
			@Nullable Boolean isCheckIn,
			@Nullable Boolean isLeave
			) throws SQLException {
		if (isCheckIn == null && isLeave == null) {
			return getTrainUserPage(conn, companyId, trainId, start, length);
		}
		if (start < 0) {
			start = 0;
		}
		if (length < 0) {
			length = 0;
		}
		
		StringBuilder totalWhereBuilder = new StringBuilder();
		totalWhereBuilder.append("WHERE company_id = ").append(companyId).append(" AND train_id = ").append(trainId);
		final String totalWhere = totalWhereBuilder.toString();
		
		StringBuilder filterWhereBuilder = new StringBuilder();
		filterWhereBuilder.append("WHERE company_id = ").append(companyId).append(" AND train_id = ").append(trainId);
		if (isCheckIn != null) {
			filterWhereBuilder.append(" AND is_check_in = ").append(isCheckIn ? 1 : 0);
		}
		if (isLeave != null) {
			filterWhereBuilder.append(" AND is_leave = ").append(isLeave ? 1 : 0);
		}
		final String filterWhere = filterWhereBuilder.toString();
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_offline_training_user ");
		sqlBuilder.append(filterWhere).append(" ORDER BY update_time DESC LIMIT ");
		sqlBuilder.append(start).append(", ");
		sqlBuilder.append(length).append("; ");
		sqlBuilder.append("SELECT count(*) AS filtered_size FROM weizhu_offline_training_user ");
		sqlBuilder.append(filterWhere).append("; ");
		sqlBuilder.append("SELECT count(*) AS total_size FROM weizhu_offline_training_user ");
		sqlBuilder.append(totalWhere).append("; ");

		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			rs = stmt.getResultSet();

			List<OfflineTrainingProtos.TrainUser> trainUserList = new ArrayList<OfflineTrainingProtos.TrainUser>();
			OfflineTrainingProtos.TrainUser.Builder tmpBuilder = OfflineTrainingProtos.TrainUser.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				trainUserList.add(TRAIN_USER_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int filteredSize;
			if (rs.next()) {
				filteredSize = rs.getInt("filtered_size");
			} else {
				throw new RuntimeException("cannot get filtered size");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			stmt.getMoreResults();
			rs = stmt.getResultSet();

			final int totalSize;
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			} else {
				throw new RuntimeException("cannot get total size");
			}

			return new DataPage<OfflineTrainingProtos.TrainUser>(trainUserList, totalSize, filteredSize);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Set<Long> getTrainApplyUserIdSet(Connection conn, long companyId, int trainId) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT user_id FROM weizhu_offline_training_user WHERE company_id = ");
		sqlBuilder.append(companyId).append(" AND train_id = ");
		sqlBuilder.append(trainId).append(" AND is_apply = 1 ORDER BY user_id; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);

			Set<Long> userIdSet = new TreeSet<Long>();
			while (rs.next()) {
				userIdSet.add(rs.getLong("user_id"));
			}

			return userIdSet;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateTrainUserApply(Connection conn, long companyId, 
			int trainId, long userId, boolean isApply, int applyTime, int updateTime
			) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_offline_training_user (company_id, train_id, user_id, is_apply, apply_time, is_check_in, is_leave, update_time) VALUES (");
		sqlBuilder.append(companyId).append(", ");
		sqlBuilder.append(trainId).append(", ");
		sqlBuilder.append(userId).append(", ");
		sqlBuilder.append(isApply ? 1 : 0).append(", ");
		sqlBuilder.append(isApply ? applyTime : "NULL").append(", 0, 0, ");
		sqlBuilder.append(updateTime).append(") ON DUPLICATE KEY UPDATE is_apply = ");
		sqlBuilder.append(isApply ? 1 : 0).append(", apply_time = ");
		sqlBuilder.append(isApply ? applyTime : "NULL").append(", update_time = ");
		sqlBuilder.append(updateTime).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateTrainUserCheckIn(Connection conn, long companyId, 
			int trainId, long userId, boolean isCheckIn, int checkInTime, int updateTime
			) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_offline_training_user (company_id, train_id, user_id, is_apply, is_check_in, check_in_time, is_leave, update_time) VALUES (");
		sqlBuilder.append(companyId).append(", ");
		sqlBuilder.append(trainId).append(", ");
		sqlBuilder.append(userId).append(", 0, ");
		sqlBuilder.append(isCheckIn ? 1 : 0).append(", ");
		sqlBuilder.append(isCheckIn ? checkInTime : "NULL").append(", 0, ");
		sqlBuilder.append(updateTime).append(") ON DUPLICATE KEY UPDATE is_check_in = ");
		sqlBuilder.append(isCheckIn ? 1 : 0).append(", check_in_time = ");
		sqlBuilder.append(isCheckIn ? checkInTime : "NULL").append(", update_time = ");
		sqlBuilder.append(updateTime).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateTrainUserLeave(Connection conn, long companyId, 
			int trainId, long userId, boolean isLeave, int leaveTime, @Nullable String leaveReason, int updateTime
			) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("INSERT INTO weizhu_offline_training_user (company_id, train_id, user_id, is_apply, is_check_in, is_leave, leave_time, leave_reason, update_time) VALUES (");
		sqlBuilder.append(companyId).append(", ");
		sqlBuilder.append(trainId).append(", ");
		sqlBuilder.append(userId).append(", 0, 0, ");
		sqlBuilder.append(isLeave ? 1 : 0).append(", ");
		sqlBuilder.append(isLeave ? leaveTime : "NULL").append(", ");
		sqlBuilder.append(isLeave && leaveReason != null ? "'" + DBUtil.SQL_STRING_ESCAPER.escape(leaveReason) + "'" : "NULL").append(", ");
		sqlBuilder.append(updateTime).append(") ON DUPLICATE KEY UPDATE is_leave = ");
		sqlBuilder.append(isLeave ? 1 : 0).append(", leave_time = ");
		sqlBuilder.append(isLeave ? leaveTime : "NULL").append(", leave_reason = ");
		sqlBuilder.append(isLeave && leaveReason != null ? "'" + DBUtil.SQL_STRING_ESCAPER.escape(leaveReason) + "'" : "NULL").append(", update_time = ");
		sqlBuilder.append(updateTime).append("; ");
		
		final String sql = sqlBuilder.toString();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
}
