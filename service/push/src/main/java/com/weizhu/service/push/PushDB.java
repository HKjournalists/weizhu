package com.weizhu.service.push;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.weizhu.common.db.DBUtil;

public class PushDB {

	public static Map<Long, Long> getPushSeq(Connection conn, long companyId, Set<Long> userIdSet0) throws SQLException {
		if (userIdSet0.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Set<Long> userIdSet = new HashSet<Long>(userIdSet0);
		Map<Long, Long> resultMap = new HashMap<Long, Long>(userIdSet.size());
		
		// 循环获取，保证必须获取到所有用户的 pushSeq。
		// 插入后再获取 可以保证并发安全
		while (!userIdSet.isEmpty()) {
			Statement stmt = null;
			ResultSet rs = null;
			try {
				StringBuilder sql = new StringBuilder("SELECT user_id, push_seq FROM weizhu_push_seq WHERE company_id = ");
				sql.append(companyId).append(" AND user_id IN (");
				DBUtil.COMMA_JOINER.appendTo(sql, userIdSet);
				sql.append("); ");
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql.toString());
				
				while (rs.next()) {
					long userId = rs.getLong("user_id");
					long pushSeq = rs.getLong("push_seq");
					resultMap.put(userId, pushSeq);
					userIdSet.remove(userId);
				}
				
				if (!userIdSet.isEmpty()) {
					DBUtil.closeQuietly(stmt);
					stmt = null;
					
					sql = new StringBuilder("INSERT IGNORE INTO weizhu_push_seq (company_id, user_id, push_seq) VALUES ");
					Iterator<Long> it = userIdSet.iterator();
					sql.append("(").append(companyId).append(", ").append(it.next()).append(", 0)");
					while (it.hasNext()) {
						sql.append(", (").append(companyId).append(", ").append(it.next()).append(", 0)");
					}
					sql.append("; ");
					
					stmt = conn.createStatement();
					stmt.executeUpdate(sql.toString());
				}
			} finally {
				DBUtil.closeQuietly(rs);
				DBUtil.closeQuietly(stmt);
			}
		}
		return resultMap;
	}
	
	public static void updatePushSeq(Connection conn, long companyId, Map<Long, Long> pushSeqMap) throws SQLException {
		if (pushSeqMap.isEmpty()) {
			return ;
		}
		
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_push_seq SET push_seq = ? WHERE company_id = ? AND user_id = ? AND push_seq < ?; ");
			
			for (Entry<Long, Long> entry : pushSeqMap.entrySet()) {
				long userId = entry.getKey();
				long pushSeq = entry.getValue();
				pstmt.setLong(1, pushSeq);
				pstmt.setLong(2, companyId);
				pstmt.setLong(3, userId);
				pstmt.setLong(4, pushSeq);
				
				pstmt.addBatch();
			}

			pstmt.executeBatch();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
