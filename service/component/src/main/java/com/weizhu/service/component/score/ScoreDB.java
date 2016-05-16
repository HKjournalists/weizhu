package com.weizhu.service.component.score;

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
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentProtos.Score;
import com.weizhu.proto.ComponentProtos.ScoreUser;

public class ScoreDB {

	//这里定义将proto文件中定义的字段名和数据库中定义的字段名关联起来，所以如果proto文件中有的字段但是数据库中没有对应的表的不能在这里定义
		private static final ProtobufMapper<ComponentProtos.Score> SCORE_MAPPER = ProtobufMapper.createMapper(ComponentProtos.Score.getDefaultInstance(),
				"score_id",
				"score_name",
				"image_name",
				"type",
				"result_view",
				"start_time",
				"end_time",
				"allow_model_id",
				"state",
				"create_admin_id",
				"create_time",
				"update_admin_id",
				"update_time");
		private static final ProtobufMapper<ComponentProtos.ScoreUser> SCORE_USER_MAPPER = ProtobufMapper.createMapper(ComponentProtos.ScoreUser.getDefaultInstance(),
				"score_id",
				"user_id",
				"score_value",
				"score_time");
		
		/**
		 * 根据批量ID批量获取打分
		 * @param conn
		 * @param companyId
		 * @param scoreIds
		 * @return
		 * @throws SQLException
		 */
		public static Map<Integer,ComponentProtos.Score> getScoreById(Connection conn,  long companyId, Collection<Integer> scoreIds) throws SQLException{
			//若传入的scoreIds是空的话则返回一个空的map
			if (scoreIds.isEmpty()) {
				return Collections.emptyMap();
			}
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM weizhu_component_score WHERE company_id = ").append(companyId).append(" AND score_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sql, scoreIds);
			sql.append("); ");
			
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				Map<Integer,ComponentProtos.Score> resultMap = new TreeMap<Integer,ComponentProtos.Score>();
				ComponentProtos.Score.Builder tmpBuilder = ComponentProtos.Score.newBuilder();
				while(rs.next()){
					tmpBuilder.clear();
					
					ComponentProtos.Score score = SCORE_MAPPER.mapToItem(rs, tmpBuilder).build();
					resultMap.put(score.getScoreId(), score);
				}
				
				return resultMap;
			}finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		
		/**
		 * 根据打分ID获取该打分的详情
		 * @param conn
		 * @param companyId
		 * @param scoreId
		 * @param size          每页数目
		 * @param lastScoreUser 上一页最后一个
		 * @return
		 * @throws SQLException
		 */
		public static List<ComponentProtos.ScoreUser> getScoreUserList (
				Connection conn, long companyId, 
				int scoreId, 
				int size,
				@Nullable ComponentProtos.ScoreUser lastScoreUser
				) throws SQLException {
			if (size <= 0) {
				return Collections.emptyList();
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM weizhu_component_score_user WHERE company_id = ").append(companyId).append(" AND score_id = ").append(scoreId);
			if (lastScoreUser != null) {
				sql.append(" AND (score_time < ").append(lastScoreUser.getScoreTime());
				sql.append(" OR (score_time = ").append(lastScoreUser.getScoreTime());
				sql.append(" AND user_id < ").append(lastScoreUser.getUserId());
				sql.append("))");
			}
			sql.append(" ORDER BY score_time DESC, user_id DESC LIMIT ").append(size).append("; ");
			
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				List<ComponentProtos.ScoreUser> resultBuilderList = new ArrayList<ScoreUser>();
				ComponentProtos.ScoreUser.Builder tmpBulider = ComponentProtos.ScoreUser.newBuilder();
				while(rs.next()){
					tmpBulider.clear();
					ComponentProtos.ScoreUser scoreUser = SCORE_USER_MAPPER.mapToItem(rs, tmpBulider).build();
					resultBuilderList.add(scoreUser);
				}
				
				return resultBuilderList;
			} finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
			
		}
		
		/**
		 * 获取打分的统计信息
		 * @param conn
		 * @param companyId
		 * @param scoreIds
		 * @return
		 * @throws SQLException
		 */
		public static Map<Integer,ComponentProtos.ScoreCount> getScoreCount(
				Connection conn,  
				long companyId, 
				Collection<Integer> scoreIds
				) throws SQLException{
			if(scoreIds.isEmpty()) {
				return Collections.emptyMap();
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT score_id,COUNT(user_id) AS user_count,SUM(score_value) AS total_score FROM weizhu_component_score_user WHERE company_id = ").append(companyId);
			sql.append(" AND score_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sql, scoreIds);
			sql.append(") ");
			sql.append(" GROUP BY score_id; ");
			
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				Map<Integer,ComponentProtos.ScoreCount> resultMap = new TreeMap<Integer,ComponentProtos.ScoreCount>();
				ComponentProtos.ScoreCount.Builder tmpBuilder = ComponentProtos.ScoreCount.newBuilder();
				while(rs.next()){
					tmpBuilder.clear();
					ComponentProtos.ScoreCount scoreCount = tmpBuilder
															.setScoreId(rs.getInt("score_id"))
															.setUserCount(rs.getInt("user_count"))
															.setTotalScore(rs.getInt("total_score")).build();
					resultMap.put(scoreCount.getScoreId(), scoreCount);
				}
				return resultMap;
			}finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		
		/**
		 * 获取某个用户一批打分的打分情况
		 * @param conn
		 * @param companyId
		 * @param userId
		 * @param scoreIds
		 * @return
		 * @throws SQLException
		 */
		public static Map<Integer, ComponentProtos.ScoreUser> getScoreUser(
				Connection conn, long companyId, 
				long userId, 
				Collection<Integer> scoreIds
				) throws SQLException{
			if(scoreIds.isEmpty()){
				return Collections.emptyMap();
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM weizhu_component_score_user WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId).append(" AND score_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sql, scoreIds);
			sql.append(");"); 
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				Map<Integer,ComponentProtos.ScoreUser> resultMap = new TreeMap<Integer,ComponentProtos.ScoreUser>();
				ComponentProtos.ScoreUser.Builder tmpBuilder = ComponentProtos.ScoreUser.newBuilder();
				while(rs.next()){
					tmpBuilder.clear();
					ComponentProtos.ScoreUser scoreUser = SCORE_USER_MAPPER.mapToItem(rs, tmpBuilder).build();
					resultMap.put(scoreUser.getScoreId(), scoreUser);
				}
				
				return resultMap;
			}finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		
		/**
		 * 获取某个用户的所有打分
		 * @param conn
		 * @param companyId
		 * @param userId
		 * @param size
		 * @return
		 * @throws SQLException
		 */
		public static List<ComponentProtos.ScoreUser> getUserScoreList(
				Connection conn, 
				long companyId, 
				long userId, 
				int size,
				@Nullable ComponentProtos.ScoreUser lastScoreUser
				) throws SQLException{
			if(size<= 0){
				return Collections.emptyList();
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM weizhu_component_score_user WHERE company_id = ").append(companyId).append(" AND user_id = ").append(userId);
			if (lastScoreUser != null) {
				sql.append(" AND (score_time < ").append(lastScoreUser.getScoreTime());
				sql.append(" OR (score_time = ").append(lastScoreUser.getScoreTime());
				sql.append(" AND score_id < ").append(lastScoreUser.getScoreId());
				sql.append("))");
			}
			sql.append(" ORDER BY score_time DESC, score_id DESC LIMIT ").append(size).append("; ");
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				List<ComponentProtos.ScoreUser> resultList = new ArrayList<ComponentProtos.ScoreUser>();
				ComponentProtos.ScoreUser.Builder tmpBuilder = ComponentProtos.ScoreUser.newBuilder();
				while(rs.next()){
					tmpBuilder.clear();
					ComponentProtos.ScoreUser scoreUser = SCORE_USER_MAPPER.mapToItem(rs, tmpBuilder).build();
					resultList.add(scoreUser);
				}
				
				return resultList;
			}finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		
		/**
		 * 插入打分详情
		 * @param conn
		 * @param companyId
		 * @param scoreUser
		 * @return
		 * @throws SQLException
		 */
		public static int insertScoreUser(Connection conn, long companyId, ComponentProtos.ScoreUser scoreUser) throws SQLException{
			PreparedStatement pstmt = null;
			
			try {
				pstmt = conn.prepareStatement("INSERT INTO weizhu_component_score_user (company_id,score_id,user_id,score_value,score_time) VALUES (?,?,?,?,?);");
				pstmt.setLong(1, companyId);
				DBUtil.set(pstmt, 2, scoreUser.getScoreId());
				DBUtil.set(pstmt, 3, scoreUser.getUserId());
				DBUtil.set(pstmt, 4, scoreUser.getScoreValue());
				DBUtil.set(pstmt, 5, scoreUser.getScoreTime());
				
				int result = pstmt.executeUpdate();
				if(result<=0){
					throw new RuntimeException("insert fail");
				}
				return result;
			} finally{
				DBUtil.closeQuietly(pstmt);
			}
		}
		
		/**
		 * 查询所有的打分
		 * @param conn
		 * @param companyId
		 * @param start
		 * @param length
		 * @return
		 * @throws SQLException
		 */
		public static DataPage<Integer> getScoreIdList(
				Connection conn, 
				long companyId, 
				int start, 
				int length,
				@Nullable ComponentProtos.State state,
	        	@Nullable String scoreNameKeyword,
				@Nullable Collection<ComponentProtos.State> totalStates  //这个还是有必要的，管理员 看不到被删除的记录，但是boss可以看到所以这两者的states不一样 获取的totalSize也不一样
				) throws SQLException {
			if (start < 0) {
				start = 0;
			}
			if (length < 0) {
				length = 0;
			}
			//若状态集合为空的话则返回空的list
			if (totalStates != null && totalStates.isEmpty()) {
				return new DataPage<Integer>(Collections.<Integer>emptyList(), 0, 0);
			}
			
			StringBuilder filterWhere = new StringBuilder();
			// 判断作为过滤条件的state是否为null
			if(state != null){
				filterWhere.append(" AND state = '").append(DBUtil.SQL_STRING_ESCAPER.escape(state.name())).append("'");
			}
			// 判断作为过滤条件的关键字是否为null
			if(scoreNameKeyword != null){
				filterWhere.append(" AND score_name LIKE '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(scoreNameKeyword)).append("%'");
			}
			
			StringBuilder totalWhere = new StringBuilder();
			if (totalStates != null){
				totalWhere.append(" AND state IN ('");
				DBUtil.QUOTE_COMMA_JOINER.appendTo(totalWhere, Iterables.transform(Iterables.transform(totalStates, Functions.toStringFunction()), DBUtil.SQL_STRING_ESCAPER.asFunction()));
				totalWhere.append("')");
			}
			
			filterWhere.append(totalWhere);
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT score_id FROM weizhu_component_score WHERE company_id = ").append(companyId);
			sql.append(filterWhere);
			sql.append(" ORDER BY create_time DESC, score_id DESC LIMIT ").append(start).append(", ").append(length).append("; ");
			sql.append("SELECT COUNT(score_id) AS total_size FROM weizhu_component_score WHERE company_id = ").append(companyId);
			sql.append(totalWhere);
			sql.append("; ");
			sql.append("SELECT COUNT(score_id) AS filtered_size FROM weizhu_component_score WHERE company_id = ").append(companyId);
			sql.append(filterWhere).append("; ");
			
			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = conn.createStatement();
				stmt.execute(sql.toString());
				rs = stmt.getResultSet();
				
				List<Integer> resultList = new ArrayList<Integer>();
				while(rs.next()){
					resultList.add(rs.getInt("score_id"));
				}
				
				DBUtil.closeQuietly(rs);
				rs = null;
				
				stmt.getMoreResults();
				rs = stmt.getResultSet();
				
				if (!rs.next()) {
					throw new RuntimeException("get item id page total size fail");
				}
				int totalSize = rs.getInt("total_size");
				
				DBUtil.closeQuietly(rs);
				rs = null;
				
				stmt.getMoreResults();
				rs = stmt.getResultSet();
				if (!rs.next()) {
					throw new RuntimeException("get item id page filtered size fail");
				}
				int filteredSize = rs.getInt("filtered_size");
				
				return new DataPage<Integer>(resultList, totalSize, filteredSize);
			} finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		
		private static final String INSERT_SCORE_SQL = "INSERT INTO weizhu_component_score ("
				+"company_id,score_name,image_name,type,result_view,start_time,"
				+"end_time,allow_model_id,state,create_admin_id,create_time) VALUES ("
				+"?,?,?,?,?,?,"
				+"?,?,?,?,?);";
		
		/**
		 * 插入打分活动
		 * @param conn
		 * @param companyId
		 * @param scoreList
		 * @return
		 * @throws SQLException
		 */
		public static List<Integer> insertScore(Connection conn, long companyId, List<ComponentProtos.Score> scoreList) throws SQLException{
			if(scoreList.isEmpty()){
				return Collections.emptyList();
			}
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				pstmt = conn.prepareStatement(INSERT_SCORE_SQL, Statement.RETURN_GENERATED_KEYS);
				for(Score score : scoreList){
					DBUtil.set(pstmt, 1,  companyId);
					DBUtil.set(pstmt, 2, score.getScoreName());
					DBUtil.set(pstmt, 3, score.hasImageName(), score.getImageName());
					DBUtil.set(pstmt, 4, score.getType());
					DBUtil.set(pstmt, 5, score.getResultView());
					DBUtil.set(pstmt, 6, score.hasStartTime(), score.getStartTime());
					DBUtil.set(pstmt, 7, score.hasEndTime(), score.getEndTime());
					DBUtil.set(pstmt, 8, score.hasAllowModelId(), score.getAllowModelId());
					DBUtil.set(pstmt, 9, score.hasState(), score.getState());
					DBUtil.set(pstmt, 10, score.hasCreateAdminId(), score.getCreateAdminId());
					DBUtil.set(pstmt, 11, score.hasCreateTime(), score.getCreateTime());
					
					pstmt.addBatch();
				}
				
				pstmt.executeBatch();
				rs = pstmt.getGeneratedKeys();
				
				List<Integer> scoreIdList = new ArrayList<Integer>();
				while (rs.next()) {
					scoreIdList.add(rs.getInt(1));
				}
				
				if (scoreList.size() != scoreIdList.size()) {
					throw new RuntimeException("insert item base fail");
				}
				
				return scoreIdList;
			} finally{
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(pstmt);
			}
		}
		
		private static final String UPDATE_SCORE_SQL = "UPDATE weizhu_component_score SET "
				+" score_name = ?, image_name = ?, result_view = ?, start_time = ?, "
				+" end_time = ?, allow_model_id = ?, update_admin_id = ?,update_time = ?"
				+" WHERE company_id = ? AND score_id = ?; ";
		
		/**
		 * 更新打分
		 * @param conn
		 * @param companyId
		 * @param score
		 * @throws SQLException
		 */
		public static void updateScore(Connection conn, long companyId,ComponentProtos.Score score ) throws SQLException {
			PreparedStatement pstmt = null;
			try {
				pstmt = conn.prepareStatement(UPDATE_SCORE_SQL);
				DBUtil.set(pstmt,1,score.getScoreName());
				DBUtil.set(pstmt,2,score.hasImageName(),score.getImageName());
				DBUtil.set(pstmt,3,score.hasResultView(),score.getResultView());
				DBUtil.set(pstmt,4,score.hasStartTime(),score.getStartTime());
				DBUtil.set(pstmt,5,score.hasEndTime(),score.getEndTime());
				DBUtil.set(pstmt,6,score.hasAllowModelId(),score.getAllowModelId());
				DBUtil.set(pstmt,7,score.hasUpdateAdminId(),score.getUpdateAdminId());
				DBUtil.set(pstmt,8,score.hasUpdateTime(),score.getUpdateTime());
				DBUtil.set(pstmt,9,companyId);
				DBUtil.set(pstmt,10,score.getScoreId());
				
				pstmt.executeUpdate();
			}finally {
				DBUtil.closeQuietly(pstmt);
			}
		}
		
		/**
		 * 更新打分的状态
		 * @param conn
		 * @param companyId
		 * @param scoreIds
		 * @param state
		 * @throws SQLException
		 */
		public static void updateScoreState(Connection conn, long companyId,Collection<Integer> scoreIds,ComponentProtos.State state) throws SQLException {
			if (scoreIds.isEmpty()) {
				return;
			}
			
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE weizhu_component_score SET state = '");
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name())).append("' WHERE company_id = ").append(companyId);
			sql.append(" AND score_id IN (");
			DBUtil.COMMA_JOINER.appendTo(sql, scoreIds);
			sql.append("); ");
			Statement st = null;
			try {
				st = conn.createStatement();
				st.executeUpdate(sql.toString());
			} finally {
				DBUtil.closeQuietly(st);
			}
		}
}
