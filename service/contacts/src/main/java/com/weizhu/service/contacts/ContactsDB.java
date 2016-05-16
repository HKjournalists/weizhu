package com.weizhu.service.contacts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.ContactsProtos;

public class ContactsDB {

	public static int insertCustomer(Connection conn, ContactsProtos.Customer customer) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT IFNULL(MAX(customer_id), 0) + 1 as customer_id FROM weizhu_customer WHERE user_id = ?; ");
			pstmt.setLong(1, customer.getUserId());
			
			rs = pstmt.executeQuery();
			
			if (!rs.next()) {
				throw new RuntimeException("read db fail!");
			}
			
			int customerId = rs.getInt("customer_id");
			
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
			
			pstmt = conn.prepareStatement("INSERT INTO weizhu_customer (user_id, customer_id, customer_name, mobile_no, is_star, company, position, department, address, email, wechat, qq, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			pstmt.setLong(1, customer.getUserId());
			pstmt.setInt(2, customerId);
			pstmt.setString(3, customer.getCustomerName());
			pstmt.setString(4, customer.getMobileNo());
			pstmt.setBoolean(5, customer.getIsStar());
			if (customer.hasCompany()) {
				pstmt.setString(6, customer.getCompany());
			} else {
				pstmt.setNull(6, java.sql.Types.VARCHAR);
			}
			if (customer.hasPosition()) {
				pstmt.setString(7, customer.getPosition());
			} else {
				pstmt.setNull(7, java.sql.Types.VARCHAR);
			}
			if (customer.hasDepartment()) {
				pstmt.setString(8, customer.getDepartment());
			} else {
				pstmt.setNull(8, java.sql.Types.VARCHAR);
			}
			if (customer.hasAddress()) {
				pstmt.setString(9, customer.getAddress());
			} else {
				pstmt.setNull(9, java.sql.Types.VARCHAR);
			}
			if (customer.hasEmail()) {
				pstmt.setString(10, customer.getEmail());
			} else {
				pstmt.setNull(10, java.sql.Types.VARCHAR);
			}
			if (customer.hasWechat()) {
				pstmt.setString(11, customer.getWechat());
			} else {
				pstmt.setNull(11, java.sql.Types.VARCHAR);
			}
			if (customer.hasQq()) {
				pstmt.setLong(12, customer.getQq());
			} else {
				pstmt.setNull(12, java.sql.Types.BIGINT);
			}
			if (customer.hasRemark()) {
				pstmt.setString(13, customer.getRemark());
			} else {
				pstmt.setNull(13, java.sql.Types.VARCHAR);
			}
			
			if (pstmt.executeUpdate() <= 0) {
				throw new RuntimeException("read db fail!");
			}
			
			return customerId;
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	private static final String UPDATE_CUSTOMER_SQL = "UPDATE weizhu_customer SET "
			+ "customer_name = ?, "
			+ "mobile_no = ?, "
			+ "is_star = ?, "
			+ "company = ?, "
			+ "position = ?, "
			+ "department = ?, "
			+ "address = ?, "
			+ "email = ?, "
			+ "wechat = ?, "
			+ "qq = ?, "
			+ "remark = ? "
			+ "WHERE user_id = ? AND customer_id = ?; ";
	
	public static boolean updateCustomer(Connection conn, ContactsProtos.Customer customer) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(UPDATE_CUSTOMER_SQL);
			
			pstmt.setString(1, customer.getCustomerName());
			pstmt.setString(2, customer.getMobileNo());
			pstmt.setBoolean(3, customer.getIsStar());
			if (customer.hasCompany()) {
				pstmt.setString(4, customer.getCompany());
			} else {
				pstmt.setNull(4, java.sql.Types.VARCHAR);
			}
			if (customer.hasPosition()) {
				pstmt.setString(5, customer.getPosition());
			} else {
				pstmt.setNull(5, java.sql.Types.VARCHAR);
			}
			if (customer.hasDepartment()) {
				pstmt.setString(6, customer.getDepartment());
			} else {
				pstmt.setNull(6, java.sql.Types.VARCHAR);
			}
			if (customer.hasAddress()) {
				pstmt.setString(7, customer.getAddress());
			} else {
				pstmt.setNull(7, java.sql.Types.VARCHAR);
			}
			if (customer.hasEmail()) {
				pstmt.setString(8, customer.getEmail());
			} else {
				pstmt.setNull(8, java.sql.Types.VARCHAR);
			}
			if (customer.hasWechat()) {
				pstmt.setString(9, customer.getWechat());
			} else {
				pstmt.setNull(9, java.sql.Types.VARCHAR);
			}
			if (customer.hasQq()) {
				pstmt.setLong(10, customer.getQq());
			} else {
				pstmt.setNull(10, java.sql.Types.BIGINT);
			}
			if (customer.hasRemark()) {
				pstmt.setString(11, customer.getRemark());
			} else {
				pstmt.setNull(11, java.sql.Types.VARCHAR);
			}
			
			pstmt.setLong(12, customer.getUserId());
			pstmt.setInt(13, customer.getCustomerId());
			
			return pstmt.executeUpdate() > 0;

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteCustomer(Connection conn, long userId, List<Integer> customerIdList) throws SQLException {
		if (customerIdList.isEmpty()) {
			return;
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM weizhu_customer WHERE user_id = ").append(userId).append(" AND customer_id IN (");
		DBUtil.COMMA_JOINER.appendTo(sql, customerIdList);
		sql.append("); ");
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql.toString());
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}

	private static final ProtobufMapper<ContactsProtos.Customer> CUSTOMER_MAPPER = ProtobufMapper.createMapper(
			ContactsProtos.Customer.getDefaultInstance(), 
			"user_id",
			"customer_id",
			"customer_name",
			"mobile_no",
			"is_star",
			"company",
			"position",
			"department",
			"address",
			"email",
			"wechat",
			"qq",
			"remark"
			);
	
	public static List<ContactsProtos.Customer> getCustomerList(Connection conn, long userId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT * FROM weizhu_customer WHERE user_id = ?; ");
			pstmt.setLong(1, userId);
			
			rs = pstmt.executeQuery();
			
			return CUSTOMER_MAPPER.mapToList(rs);
			
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
}
