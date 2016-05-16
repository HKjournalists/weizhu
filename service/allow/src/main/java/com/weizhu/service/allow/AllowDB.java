package com.weizhu.service.allow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.protobuf.InvalidProtocolBufferException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.AllowProtos;

public class AllowDB {
	
	private static final ProtobufMapper<AllowProtos.Model> MODEL_MAPPER = ProtobufMapper
			.createMapper(AllowProtos.Model.getDefaultInstance(), 
					"model_id", 
					"model_name",
					"default_action",
					"create_admin_id",
					"create_time");
	
	public static Map<Integer, AllowDAOProtos.ModelRule> getModelRule(Connection conn, long companyId, Collection<Integer> modelIds) throws SQLException {
		if (modelIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		String modelIdStr = DBUtil.COMMA_JOINER.join(modelIds);
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT rule_id, model_id, rule_data FROM weizhu_allow_rule WHERE company_id = ").append(companyId);
		sql.append(" AND model_id IN (").append(modelIdStr);
		sql.append(") ORDER BY model_id, rule_id ASC; ");
		sql.append("SELECT * FROM weizhu_allow_model WHERE company_id = ").append(companyId);
		sql.append(" AND model_id IN (").append(modelIdStr).append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			
			stmt.execute(sql.toString());
			rs = stmt.getResultSet();
			
			Map<Integer, LinkedList<AllowProtos.Rule>> ruleListMap = new TreeMap<Integer, LinkedList<AllowProtos.Rule>>();
			AllowProtos.Rule.Builder ruleBuilder = AllowProtos.Rule.newBuilder();
			while (rs.next()) {
				try {
					int modelId = rs.getInt("model_id");
					AllowProtos.Rule rule = AllowProtos.Rule.parseFrom(rs.getBytes("rule_data"));
					
					LinkedList<AllowProtos.Rule> list = ruleListMap.get(modelId);
					if (list == null) {
						list = new LinkedList<AllowProtos.Rule>();
						ruleListMap.put(modelId, list);
					}
					
					int ruleId = rs.getInt("rule_id");
					ruleBuilder.clear();
					ruleBuilder.mergeFrom(rule).setRuleId(ruleId);
					
					list.add(ruleBuilder.build());
				} catch (InvalidProtocolBufferException e) {
					// ignore
					// TODO log error!
				}
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Integer, AllowDAOProtos.ModelRule> modelRuleMap = new TreeMap<Integer, AllowDAOProtos.ModelRule>();
			AllowDAOProtos.ModelRule.Builder tmpModelRuleBuilder = AllowDAOProtos.ModelRule.newBuilder();
			AllowProtos.Model.Builder tmpModelBuilder = AllowProtos.Model.newBuilder();
			
			while (rs.next()) {
				tmpModelRuleBuilder.clear();
				tmpModelBuilder.clear();
				
				AllowProtos.Model model = MODEL_MAPPER.mapToItem(rs, tmpModelBuilder).build();
				tmpModelRuleBuilder.setModel(model);
				
				String ruleIdListStr = rs.getString("rule_order_str");
				LinkedList<AllowProtos.Rule> ruleList = ruleListMap.remove(model.getModelId());
				if (ruleList != null) {
					
					if (ruleIdListStr != null) {
						List<String> ruleIdStrList = DBUtil.COMMA_SPLITTER.splitToList(ruleIdListStr);
						for (String ruleIdStr : ruleIdStrList) {
							Integer ruleId;
							try {
								ruleId = Integer.parseInt(ruleIdStr);
							} catch (NumberFormatException e) {
								continue;
							}
							
							Iterator<AllowProtos.Rule> it = ruleList.iterator();
							while (it.hasNext()) {
								final AllowProtos.Rule rule = it.next();
								
								if (rule.getRuleId() == ruleId.intValue()) {
									tmpModelRuleBuilder.addRule(rule);
									it.remove();
									break;
								}
							}
						}
					}
					
					tmpModelRuleBuilder.addAllRule(ruleList);
				}
				
				modelRuleMap.put(model.getModelId(), tmpModelRuleBuilder.build());
			}
			
			return modelRuleMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static int insertModel(Connection conn, long companyId, AllowProtos.Model model) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_allow_model (company_id, model_id, model_name, default_action, create_admin_id, create_time, rule_order_str) VALUES (?, NULL, ?, ?, ?, ?, ''); ", Statement.RETURN_GENERATED_KEYS);
			
			DBUtil.set(pstmt, 1, true, companyId);
			DBUtil.set(pstmt, 2, model.hasModelName(), model.getModelName());
			DBUtil.set(pstmt, 3, model.hasDefaultAction(), model.getDefaultAction());
			DBUtil.set(pstmt, 4, model.hasCreateAdminId(), model.getCreateAdminId());
			DBUtil.set(pstmt, 5, model.hasCreateTime(), model.getCreateTime());
			
			pstmt.executeUpdate();
			
			rs = pstmt.getGeneratedKeys();
			
			if (!rs.next()) {
				throw new RuntimeException("insertModel fail!");
			}
			
			return rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static List<Integer> insertRule(Connection conn, long companyId, int modelId, List<AllowProtos.Rule> ruleList) throws SQLException {
		if (ruleList.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_allow_rule (company_id, rule_id, model_id, rule_name, `action`, rule_type, rule_data) VALUES (?, NULL, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			
			for (AllowProtos.Rule rule : ruleList) {
				DBUtil.set(pstmt, 1, true, companyId);
				DBUtil.set(pstmt, 2, true, modelId);
				DBUtil.set(pstmt, 3, rule.hasRuleName(), rule.getRuleName());
				DBUtil.set(pstmt, 4, rule.hasAction(), rule.getAction());
				DBUtil.set(pstmt, 5, true, rule.getRuleTypeCase());
				DBUtil.set(pstmt, 6, true, rule.toByteString());
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> ruleIdList = new ArrayList<Integer>(ruleList.size());
			while (rs.next()) {
				ruleIdList.add(rs.getInt(1));
			}
			
			if (ruleList.size() != ruleIdList.size()) {
				throw new RuntimeException("insert rule fail!");
			}
			
			return ruleIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static DataPage<Integer> getModelIdPage(Connection conn, long companyId, int start, int length, String keyword) throws SQLException {
		if (length == 0) {
			return new DataPage<Integer>(Collections.<Integer>emptyList(), 0, 0);
		}
		
		StringBuilder keywordSQL = new StringBuilder();
		if (!keyword.isEmpty()) {
			keywordSQL.append(" AND model_name LIKE '%");
			keywordSQL.append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(keyword));
			keywordSQL.append("%' ");
		}
		
		StringBuilder sql = new StringBuilder("SELECT COUNT(1) AS total FROM weizhu_allow_model WHERE company_id = ");
		sql.append(companyId);
		sql.append(keywordSQL);
		sql.append("; ");
		sql.append("SELECT model_id FROM weizhu_allow_model WHERE company_id = ");
		sql.append(companyId);
		sql.append(keywordSQL);
		sql.append(" ORDER BY model_id DESC LIMIT ").append(start).append(",").append(length).append("; ");
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
			
			rs = stmt.getResultSet();
			while (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt("total");
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Integer> modelIdList = new ArrayList<Integer>();
			while (rs.next()) {
				modelIdList.add(rs.getInt("model_id"));
			}
			
			return new DataPage<Integer>(modelIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void deleteModel(Connection conn, long companyId, Collection<Integer> modelIds) throws SQLException {
		if (modelIds.isEmpty()) {
			return ;
		}
		
		String modelIdStr = DBUtil.COMMA_JOINER.join(modelIds);
		
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_allow_model WHERE company_id = ");
		sql.append(companyId);
		sql.append(" AND model_id IN (");
		sql.append(modelIdStr);
		sql.append("); ");
		sql.append("DELETE FROM weizhu_allow_rule WHERE company_id = ");
		sql.append(companyId);
		sql.append(" AND model_id IN (");
		sql.append(modelIdStr);
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void deleteRule(Connection conn, long companyId, int modelId, Collection<Integer> ruleIds) throws SQLException {
		if (ruleIds.isEmpty()) {
			return ;
		}
		
		StringBuilder sql = new StringBuilder("DELETE FROM weizhu_allow_rule WHERE company_id = ");
		sql.append(companyId);
		sql.append(" AND rule_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(ruleIds));
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static void updateModelName(Connection conn, long companyId, int modelId, String modelName, AllowProtos.Action action) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_model SET model_name = ?, default_action = ? WHERE company_id = ? AND model_id = ?; ");
			pstmt.setString(1, modelName);
			pstmt.setString(2, action.name());
			pstmt.setLong(3, companyId);
			pstmt.setInt(4, modelId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateModelRuleOrder(Connection conn, long companyId, int modelId, List<Integer> ruleIdList) throws SQLException {
		String ruleIdOrderStr = DBUtil.COMMA_JOINER.join(ruleIdList);
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_model SET rule_order_str = ? WHERE company_id = ? AND model_id = ?; ");
			pstmt.setString(1, ruleIdOrderStr);
			pstmt.setLong(2, companyId);
			pstmt.setInt(3, modelId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateUserRule(Connection conn, long companyId, int ruleId, String ruleName, AllowProtos.Action ruleAction, List<Long> userIdList) throws SQLException {
		AllowProtos.UserRule userRule = AllowProtos.UserRule.newBuilder()
				.addAllUserId(userIdList)
				.build();
		
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(ruleId)
				.setRuleName(ruleName)
				.setAction(ruleAction)
				.setUserRule(userRule)
				.build();
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_rule SET rule_type = 'USER_RULE', rule_name = ?, action = ?, rule_data = ? WHERE company_id = ? AND rule_id = ?; ");
			pstmt.setString(1, ruleName);
			pstmt.setString(2, ruleAction.name());
			pstmt.setBytes(3, rule.toByteArray());
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, ruleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
    public static void updateTeamRule(Connection conn, long companyId, int ruleId, String ruleName, AllowProtos.Action ruleAction, List<Integer> teamIdList) throws SQLException {
    	AllowProtos.TeamRule teamRule = AllowProtos.TeamRule.newBuilder()
				.addAllTeamId(teamIdList)
				.build();
		
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(ruleId)
				.setRuleName(ruleName)
				.setAction(ruleAction)
				.setTeamRule(teamRule)
				.build();
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_rule SET rule_type= 'TEAM_RULE', rule_name = ?, action = ?, rule_data = ? WHERE company_id = ? AND rule_id = ?; ");
			pstmt.setString(1, ruleName);
			pstmt.setString(2, ruleAction.name());
			pstmt.setBytes(3, rule.toByteArray());
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, ruleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updatePositionRule(Connection conn, long companyId, int ruleId, String ruleName, AllowProtos.Action ruleAction, List<Integer> positionIdList) throws SQLException {
		AllowProtos.PositionRule positionRule = AllowProtos.PositionRule.newBuilder()
				.addAllPositionId(positionIdList)
				.build();
		
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(ruleId)
				.setRuleName(ruleName)
				.setAction(ruleAction)
				.setPositionRule(positionRule)
				.build();
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_rule SET rule_type = 'POSITION_RULE', rule_name = ?, action = ?, rule_data = ? WHERE company_id = ? AND rule_id = ?; ");
			pstmt.setString(1, ruleName);
			pstmt.setString(2, ruleAction.name());
			pstmt.setBytes(3, rule.toByteArray());
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, ruleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateLevelRule(Connection conn, long companyId, int ruleId, String ruleName, AllowProtos.Action ruleAction, List<Integer> levelIdList) throws SQLException {
		AllowProtos.LevelRule levelRule = AllowProtos.LevelRule.newBuilder()
				.addAllLevelId(levelIdList)
				.build();
		
		AllowProtos.Rule rule = AllowProtos.Rule.newBuilder()
				.setRuleId(ruleId)
				.setRuleName(ruleName)
				.setAction(ruleAction)
				.setLevelRule(levelRule)
				.build();
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_allow_rule SET rule_type = 'LEVEL_RULE', rule_name = ?, action = ?, rule_data = ? WHERE company_id = ? AND rule_id = ?; ");
			pstmt.setString(1, ruleName);
			pstmt.setString(2, ruleAction.name());
			pstmt.setBytes(3, rule.toByteArray());
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, ruleId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static Map<Integer, AllowProtos.Model> getModel(Connection conn, long companyId, Collection<Integer> modelIds) throws SQLException {
		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_allow_model WHERE company_id = ");
		sql.append(companyId);
		sql.append(" AND model_id IN (");
		sql.append(DBUtil.COMMA_JOINER.join(modelIds));
		sql.append("); ");
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
		
			Map<Integer, AllowProtos.Model> modelMap = new HashMap<Integer, AllowProtos.Model>();
			
			AllowProtos.Model.Builder tmpModelBuilder = AllowProtos.Model.newBuilder();
			while (rs.next()) {
				int modelId = rs.getInt("model_id");
				tmpModelBuilder.clear();
				AllowProtos.Model model = MODEL_MAPPER.mapToItem(rs, tmpModelBuilder).build();
				
				modelMap.put(modelId, model);
			}
			return modelMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
}
