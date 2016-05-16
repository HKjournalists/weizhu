package com.weizhu.service.tools.productcolock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.Gender;
import com.weizhu.service.tools.ToolsDAOProtos;

public class ToolsProductclockDB {
	
	private static final ProtobufMapper<ProductclockProtos.Customer> CUSTOMER_MAPPER = 
			ProtobufMapper.createMapper(ProductclockProtos.Customer.getDefaultInstance(),
			  "customer_id",
			  "customer_name",
			  "mobile_no",
			  "gender",
			  "birthday_solar",
			  "birthday_lunar",
			  "wedding_solar",
			  "wedding_lunar",
			  "address",
			  "remark",
			  "belong_user",
			  "is_remind",
			  "days_ago_remind",
			  "state",
			  "create_admin",
			  "create_user",
			  "create_time",
			  "update_admin",
			  "update_user",
			  "update_time");
	
	private static final ProtobufMapper<ProductclockProtos.Product> PRODUCT_MAPPER = 
			ProtobufMapper.createMapper(ProductclockProtos.Product.getDefaultInstance(),
			  "product_id",
			  "product_name",
			  "product_desc",
			  "image_name",
			  "default_remind_day",
			  "state",
			  "create_admin",
			  "create_time",
			  "update_admin",
			  "update_time");
	
	public static Map<Integer, ProductclockProtos.Customer> getCustomerById(Connection conn, long companyId,
			Collection<Integer> customerIds) throws SQLException {
		if (customerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_tools_productclock_customer WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("customer_id IN (").append(DBUtil.COMMA_JOINER.join(customerIds)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, ProductclockProtos.Customer> customerMap = Maps.newHashMap();
			ProductclockProtos.Customer.Builder customerBuilder = ProductclockProtos.Customer.newBuilder();
			while (rs.next()) {
				customerBuilder.clear();
				
				CUSTOMER_MAPPER.mapToItem(rs, customerBuilder);
				int customerId = rs.getInt("customer_id");
				
				customerMap.put(customerId, customerBuilder.build());
			}
			
			return customerMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	public static Map<Integer, ProductclockProtos.Product> getProductById(Connection conn, long companyId,
			Collection<Integer> productIds) throws SQLException {
		if (productIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_tools_productclock_product WHERE company_id = ").append(companyId).append(" AND state = 'NORMAL' AND ");
		sqlBuilder.append("product_id IN (").append(DBUtil.COMMA_JOINER.join(productIds)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, ProductclockProtos.Product> productMap = Maps.newConcurrentMap();
			ProductclockProtos.Product.Builder productBuilder = ProductclockProtos.Product.newBuilder();
			while (rs.next()) {
				productBuilder.clear();
				
				PRODUCT_MAPPER.mapToItem(rs, productBuilder);
				int productId = rs.getInt("product_id");
				
				productMap.put(productId, productBuilder.build());
			}
			
			return productMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 根据顾客所属销售获取顾客
	 * @param conn
	 * @param companyId
	 * @param userId
	 * @param hasProduct
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getCustomerIdByUserId(Connection conn, long companyId, long userId,
			@Nullable Boolean hasProduct) throws SQLException {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT customer_id FROM weizhu_tools_productclock_customer c WHERE c.company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("c.belong_user = ").append(userId).append(" AND ");
		if (hasProduct != null) {
			if (hasProduct) {
				sqlBuilder.append("EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			} else {
				sqlBuilder.append("NOT EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			}
		}
		sqlBuilder.append("state = 'NORMAL' ORDER BY customer_id DESC; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Integer> list = Lists.newArrayList();
			while (rs.next()) {
				list.add(rs.getInt("customer_id"));
			}
			
			return list;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取顾客（客户端）
	 * @param conn
	 * @param companyId 
	 * @param userId
	 * @param lastCustomerId
	 * @param lastCreateTime
	 * @param size
	 * @param hasProduct
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getCustomerIdList(Connection conn, long companyId, long userId,
			@Nullable Integer lastCustomerId, @Nullable Integer lastCreateTime, 
			int size, @Nullable Boolean hasProduct) throws SQLException {
		if (size < 1) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT customer_id FROM weizhu_tools_productclock_customer c WHERE company_id = ").append(companyId).append(" AND ");
		if (hasProduct != null) {
			if (hasProduct) {
				sqlBuilder.append("EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			} else {
				sqlBuilder.append("NOT EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			}
		}
		if (lastCustomerId != null && lastCreateTime != null) {
			sqlBuilder.append("(c.create_time < ").append(lastCreateTime);
			sqlBuilder.append(" OR (c.create_time = ").append(lastCreateTime).append(" AND c.customer_id < ").append(lastCustomerId).append(")) AND ");
		}
		sqlBuilder.append("belong_user = ").append(userId).append(" AND state = 'NORMAL' ORDER BY c.create_time DESC, c.customer_id ASC LIMIT ").append(size).append("; ");
		
		String sql = sqlBuilder.toString();

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Integer> customerIdList = Lists.newArrayList();
			while (rs.next()) {
				customerIdList.add(rs.getInt("customer_id"));
			}
			
			return customerIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取顾客（服务端）
	 * @param conn
	 * @param companyId
	 * @param size
	 * @param length
	 * @param salorId
	 * @param hasProduct
	 * @param customerName
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<Integer> getCustomerIdPage(Connection conn, long companyId, 
			int size, int length, 
			List<Long> salorIdList, @Nullable Boolean hasProduct, @Nullable String customerName) throws SQLException {
		if (length < 1) {
			return new DataPage<Integer>(Collections.emptyList(), 0, 0);
		}
		
		StringBuilder condition = new StringBuilder();
		if (hasProduct != null) {
			if (hasProduct) {
				condition.append("EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			} else {
				condition.append("NOT EXISTS(SELECT 1 FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE c.customer_id = cp.customer_id AND p.state = 'NORMAL' AND cp.product_id = p.product_id) AND ");
			}
		}
		if (!salorIdList.isEmpty()) {
			condition.append("c.belong_user IN (").append(DBUtil.COMMA_JOINER.join(salorIdList)).append(") AND ");
		}
		if (customerName != null) {
			condition.append("c.customer_name LIKE '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(customerName)).append("%' AND ");
		}
		
		StringBuilder sqlBuilder = new StringBuilder();	
		sqlBuilder.append("SELECT COUNT(1) AS total FROM weizhu_tools_productclock_customer c WHERE c.company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append(condition);
		sqlBuilder.append("state = 'NORMAL'; ");
		sqlBuilder.append("SELECT customer_id FROM weizhu_tools_productclock_customer c WHERE c.company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append(condition);
		sqlBuilder.append("state = 'NORMAL' ORDER BY c.create_time DESC, c.customer_id ASC LIMIT ").append(size).append(", ").append(length).append("; ");
		
		String sql = sqlBuilder.toString();

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt("total");
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			List<Integer> customerIdList = Lists.newArrayList();
			while (rs.next()) {
				customerIdList.add(rs.getInt("customer_id"));
			}
			
			return new DataPage<Integer>(customerIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 添加顾客
	 * @param conn
	 * @param companyId
	 * @param customers
	 * @param createUserId
	 * @param createTime
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> insertCustomer(Connection conn, long companyId, 
			Collection<ProductclockProtos.Customer> customers,
			long createUserId, int createTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tools_productclock_customer (company_id, customer_name, mobile_no, gender, "
					+ "birthday_solar, birthday_lunar, wedding_solar, wedding_lunar, "
					+ "address, remark, is_remind, days_ago_remind, belong_user, "
					+ "state, create_admin, create_user, create_time) VALUES (?, ?, ?, ?,"
					+ "?, ?, ?, ?,"
					+ "?, ?, ?, ?, ?,"
					+ "?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			for (ProductclockProtos.Customer customer : customers) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, customer.getCustomerName());
				DBUtil.set(pstmt, 3, customer.hasMobileNo() ? customer.getMobileNo() : null);
				DBUtil.set(pstmt, 4, customer.hasGender() ? customer.getGender().name() : Gender.MALE.name());
				DBUtil.set(pstmt, 5, customer.hasBirthdaySolar() ? customer.getBirthdaySolar() : null);
				DBUtil.set(pstmt, 6, customer.hasBirthdayLunar() ? customer.getBirthdayLunar() : null);
				DBUtil.set(pstmt, 7, customer.hasWeddingSolar() ? customer.getWeddingSolar() : null);
				DBUtil.set(pstmt, 8, customer.hasWeddingLunar() ? customer.getWeddingLunar() : null);
				DBUtil.set(pstmt, 9, customer.hasAddress() ? customer.getAddress() : null);
				DBUtil.set(pstmt, 10, customer.hasRemark() ? customer.getRemark() : null);
				DBUtil.set(pstmt, 11, customer.hasIsRemind() ? customer.getIsRemind() : false);
				DBUtil.set(pstmt, 12, customer.hasDaysAgoRemind() ? customer.getDaysAgoRemind() : 1);
				DBUtil.set(pstmt, 13, customer.hasCreateUser() ? customer.getCreateUser() : null);
				DBUtil.set(pstmt, 14, ProductclockProtos.State.NORMAL);
				DBUtil.set(pstmt, 15, customer.hasCreateAdmin() ? customer.getCreateAdmin() : null);
				DBUtil.set(pstmt, 16, customer.hasCreateUser() ? customer.getCreateUser() : null);
				DBUtil.set(pstmt, 17, createTime);
				
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> customerIdList = Lists.newArrayList();
			while (rs.next()) {
				customerIdList.add(rs.getInt(1));
			}
			
			return customerIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新顾客信息
	 * @param conn
	 * @param companyId
	 * @param customerId
	 * @param customerName
	 * @param mobileNo
	 * @param gender
	 * @param birthdaySolar
	 * @param birthdayLunar
	 * @param weddingSolar
	 * @param weddingLunar
	 * @param address
	 * @param remark
	 * @param isRemind
	 * @param daysAgoRemind
	 * @param updateUserId
	 * @param updateTime
	 * @throws SQLException
	 */
	public static void updateCustomer(Connection conn, long companyId, int customerId,
			String customerName, @Nullable String mobileNo, Gender gender,
			@Nullable Integer birthdaySolar, @Nullable Integer birthdayLunar, @Nullable Integer weddingSolar, @Nullable Integer weddingLunar,
			@Nullable String address, @Nullable String remark, boolean isRemind, int daysAgoRemind,
			long updateUserId, int updateTime) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_tools_productclock_customer SET customer_name = ?, mobile_no = ?, gender = ?, "
					+ "birthday_solar = ?, birthday_lunar = ?, wedding_solar = ?, wedding_lunar = ?, "
					+ "address = ?, remark = ?, is_remind = ?, days_ago_remind = ?, "
					+ "update_user = ?, update_time = ? WHERE company_id = ? AND customer_id = ?; ");
			DBUtil.set(pstmt, 1, customerName);
			DBUtil.set(pstmt, 2, mobileNo);
			DBUtil.set(pstmt, 3, gender.name());
			DBUtil.set(pstmt, 4, birthdaySolar);
			DBUtil.set(pstmt, 5, birthdayLunar);
			DBUtil.set(pstmt, 6, weddingSolar);
			DBUtil.set(pstmt, 7, weddingLunar);
			DBUtil.set(pstmt, 8, address);
			DBUtil.set(pstmt, 9, remark);
			DBUtil.set(pstmt, 10, isRemind);
			DBUtil.set(pstmt, 11, daysAgoRemind);
			DBUtil.set(pstmt, 12, updateUserId);
			DBUtil.set(pstmt, 13, updateTime);
			DBUtil.set(pstmt, 14, companyId);
			DBUtil.set(pstmt, 15, customerId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}		
	}
	
	/**
	 * 更新顾客所属销售
	 * @param conn
	 * @param companyId
	 * @param customerIds
	 * @param salerId
	 * @throws SQLException
	 */
	public static void updateCustomerBelongSaler(Connection conn, long companyId,
			Collection<Integer> customerIds, long salerId) throws SQLException {
		if (customerIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_tools_productclock_customer SET belong_user = ").append(salerId).append(" ");
		sqlBuilder.append("WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("customer_id IN (").append(DBUtil.COMMA_JOINER.join(customerIds)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 删除顾客
	 * @param conn
	 * @param companyId
	 * @param customerIds
	 * @throws SQLException
	 */
	public static void deleteCustomer(Connection conn, long companyId,
			Collection<Integer> customerIds) throws SQLException {
		if (customerIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_tools_productclock_customer SET state = 'DELETE' WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("customer_id IN (").append(DBUtil.COMMA_JOINER.join(customerIds)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取产品（客户端）
	 * @param conn
	 * @param companyId
	 * @param lastProductId
	 * @param lastCreateTime
	 * @param size
	 * @param productName
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> getProductIdList(Connection conn, long companyId,
			@Nullable Integer lastProductId, @Nullable Integer lastCreateTime,
			int size, @Nullable String productName) throws SQLException {
		if (size < 1) {
			return Collections.emptyList();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT product_id FROM weizhu_tools_productclock_product WHERE company_id = ").append(companyId).append(" AND ");
		if (productName != null) {
			sqlBuilder.append("product_name like '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(productName)).append("%' AND ");
		}
		if (lastProductId != null && lastCreateTime != null) {
			sqlBuilder.append("(create_time < ").append(lastCreateTime);
			sqlBuilder.append(" OR (create_time = ").append(lastCreateTime).append(" AND product_id < ").append(lastProductId).append(")) AND ");
		}
		sqlBuilder.append("1=1 ORDER BY create_time DESC, product_id ASC LIMIT ").append(size).append("; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			List<Integer> productIdList = Lists.newArrayList();
			while (rs.next()) {
				productIdList.add(rs.getInt("product_id"));
			}
			
			return productIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
		
	}
	
	/**
	 * 获取产品（服务端）
	 * @param conn
	 * @param companyId
	 * @param start
	 * @param length
	 * @param productName
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<Integer> getProductIdPage(Connection conn, long companyId,
			int start, int length,
			@Nullable String productName) throws SQLException {
		if (length < 1) {
			return new DataPage<Integer>(Collections.emptyList(), 0, 0);
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT COUNT(1) AS total FROM weizhu_tools_productclock_product WHERE company_id = ").append(companyId).append(" AND ");
		if (productName != null) {
			sqlBuilder.append("product_name like '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(productName)).append("%' AND ");
		}
		sqlBuilder.append("state = 'NORMAL'; ");
		sqlBuilder.append("SELECT product_id FROM weizhu_tools_productclock_product WHERE company_id = ").append(companyId).append(" AND ");
		if (productName != null) {
			sqlBuilder.append("product_name like '%").append(DBUtil.SQL_LIKE_STRING_ESCAPER.escape(productName)).append("%' AND ");
		}
		sqlBuilder.append("state = 'NORMAL' ORDER BY create_time DESC, product_id ASC LIMIT ").append(start).append(", ").append(length).append("; ");
		
		String sql = sqlBuilder.toString();

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			
			rs = stmt.getResultSet();
			if (!rs.next()) {
				throw new RuntimeException("cannot get total!");
			}
			int total = rs.getInt("total");
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			List<Integer> productIdList = Lists.newArrayList();
			while (rs.next()) {
				productIdList.add(rs.getInt("product_id"));
			}
			
			return new DataPage<Integer>(productIdList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 增加产品
	 * @param conn
	 * @param companyId
	 * @param products      产品集合
	 * @param createAdminId
	 * @param createTime
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> insertProduct(Connection conn, long companyId,
			Collection<ProductclockProtos.Product> products,
			long createAdminId, int createTime) throws SQLException {
		if (products.isEmpty()) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tools_productclock_product (company_id, product_name, image_name, default_remind_day, product_desc, state, create_admin, create_time) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, ?); ", Statement.RETURN_GENERATED_KEYS);
			for (ProductclockProtos.Product product : products) {
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, product.getProductName());
				DBUtil.set(pstmt, 3, product.hasImageName() ? product.getImageName() : null);
				DBUtil.set(pstmt, 4, product.getDefaultRemindDay());
				DBUtil.set(pstmt, 5, product.hasProductDesc() ? product.getProductDesc() : null);
				DBUtil.set(pstmt, 6, "NORMAL");
				DBUtil.set(pstmt, 7, createAdminId);
				DBUtil.set(pstmt, 8, createTime);
				
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			
			List<Integer> productIdList = Lists.newArrayList();
			while (rs.next()) {
				productIdList.add(rs.getInt(1));
			}
			
			return productIdList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 修改产品信息
	 * @param conn
	 * @param companyId
	 * @param productId
	 * @param productName
	 * @param remindDay
	 * @param imageName
	 * @param productDesc
	 * @param updateAdmin
	 * @param updateTime
	 * @throws SQLException
	 */
	public static void updateProduct(Connection conn, long companyId, int productId,
			String productName, int remindDay, @Nullable String imageName, @Nullable String productDesc,
			long updateAdmin, int updateTime) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_tools_productclock_product SET product_name = ?, default_remind_day = ?, image_name = ?, product_desc = ?, update_admin = ?, update_time = ? "
					+ "WHERE company_id = ? AND product_id = ?; ");
			DBUtil.set(pstmt, 1, productName);
			DBUtil.set(pstmt, 2, remindDay);
			DBUtil.set(pstmt, 3, imageName);
			DBUtil.set(pstmt, 4, productDesc);
			DBUtil.set(pstmt, 5, updateAdmin);
			DBUtil.set(pstmt, 6, updateTime);
			DBUtil.set(pstmt, 7, companyId);
			DBUtil.set(pstmt, 8, productId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 删除产品
	 * @param conn
	 * @param companyId
	 * @param productIdList
	 * @throws SQLException
	 */
	public static void deleteProduct(Connection conn, long companyId, 
			List<Integer> productIdList) throws SQLException {
		if (productIdList.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("UPDATE weizhu_tools_productclock_product SET state = 'DELETE' WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("product_id IN (").append(DBUtil.COMMA_JOINER.join(productIdList)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<ToolsDAOProtos.ProductclockCustomerProduct> CUSTOMER_PRODUCT_MAPPER = 
			ProtobufMapper.createMapper(ToolsDAOProtos.ProductclockCustomerProduct.getDefaultInstance(),
			  "customer_id",
			  "product_id",
			  "buy_time",
			  "remind_period_day"); 
	
	/**
	 * 获取顾客关联的产品
	 * @param conn
	 * @param companyId
	 * @param customerIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> getCustomerProduct(Connection conn, long companyId,
			Collection<Integer> customerIds) throws SQLException {
		if (customerIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM weizhu_tools_productclock_customer_product cp, weizhu_tools_productclock_product p WHERE cp.company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("cp.customer_id IN (").append(DBUtil.COMMA_JOINER.join(customerIds)).append(") AND ");
		sqlBuilder.append("cp.product_id = p.product_id AND p.state = 'NORMAL'; ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Integer, List<ToolsDAOProtos.ProductclockCustomerProduct>> customerProductMap = Maps.newHashMap();
			List<ToolsDAOProtos.ProductclockCustomerProduct> customerProductList = null;
			ToolsDAOProtos.ProductclockCustomerProduct.Builder customerProductBuilder = ToolsDAOProtos.ProductclockCustomerProduct.newBuilder();
			while (rs.next()) {
				customerProductBuilder.clear();
				
				CUSTOMER_PRODUCT_MAPPER.mapToItem(rs, customerProductBuilder);
				
				int customerId = rs.getInt("customer_id");
				customerProductList = customerProductMap.get(customerId);
				if (customerProductList == null) {
					customerProductList = Lists.newArrayList();
				}
				customerProductList.add(customerProductBuilder.build());
				
				customerProductMap.put(customerId, customerProductList);
			}
			
			return customerProductMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 增加顾客购买产品的信息
	 * @param conn
	 * @param companyId
	 * @param customerId 顾客id
	 * @param productId  产品id
	 * @param buyTime    购买时间
	 * @param remindPeriodDay 提醒周期（天）
	 * @throws SQLException
	 */
	public static void insertCustomerProduct(Connection conn, long companyId, 
			int customerId, int productId, int buyTime, int remindPeriodDay) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tools_productclock_customer_product (company_id, customer_id, product_id, buy_time, remind_period_day) VALUES "
					+ "(?, ?, ?, ?, ?); ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, customerId);
			DBUtil.set(pstmt, 3, productId);
			DBUtil.set(pstmt, 4, buyTime);
			DBUtil.set(pstmt, 5, remindPeriodDay);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 更新客户的产品信息
	 * @param conn
	 * @param companyId
	 * @param customerId   顾客id
	 * @param oldProductId 原来的产品id
	 * @param newProductId 更新后的产品id
	 * @param buyTime      购买时间
	 * @param remindPeriodDay 提醒周期（天）
	 * @throws SQLException
	 */
	public static void updateCustomerProduct(Connection conn, long companyId,
			int customerId, int oldProductId, int newProductId,
			int buyTime, int remindPeriodDay) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("DELETE FROM weizhu_tools_productclock_customer_product WHERE company_id = ? AND customer_id = ? AND product_id = ?; "
					+ "INSERT INTO weizhu_tools_productclock_customer_product (company_id, customer_id, product_id, buy_time, remind_period_day) VALUES "
					+ "(?, ?, ?, ?, ?); ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, customerId);
			DBUtil.set(pstmt, 3, oldProductId);
			DBUtil.set(pstmt, 4, companyId);
			DBUtil.set(pstmt, 5, customerId);
			DBUtil.set(pstmt, 6, newProductId);
			DBUtil.set(pstmt, 7, buyTime);
			DBUtil.set(pstmt, 8, remindPeriodDay);
			
			pstmt.execute();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 删除客户和产品的关联信息
	 * @param conn
	 * @param companyId
	 * @param customerId 顾客id
	 * @param productIds 产品集合
	 * @throws SQLException
	 */
	public static void deleteCustomerProduct(Connection conn, long companyId,
			int customerId, Collection<Integer> productIds) throws SQLException {
		if (productIds.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("DELETE FROM weizhu_tools_productclock_customer_product WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("customer_id = ").append(customerId).append(" AND ");
		sqlBuilder.append("product_id IN (").append(DBUtil.COMMA_JOINER.join(productIds)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
		
	}
	
	private static final ProtobufMapper<ProductclockProtos.CommunicateRecord> COMMUNICATE_RECORD = 
			ProtobufMapper.createMapper(ProductclockProtos.CommunicateRecord.getDefaultInstance(),
					  "record_id",
					  "user_id",
					  "customer_id",
					  "content_text",
					  "create_time"); 
	
	/**
	 * 获取交流信息（客户端）
	 * @param conn
	 * @param companyId
	 * @param userId     销售id
	 * @param customerId 顾客id
	 * @param recordId   交流记录id（可null）
	 * @param createTime	
	 * @param size
	 * @return
	 * @throws SQLException
	 */
	public static List<ProductclockProtos.CommunicateRecord> getCommunicateRecord(Connection conn, long companyId, 
			long userId, int customerId,
			@Nullable Integer recordId, @Nullable Integer createTime,
			int size) throws SQLException {
		if (size < 1) {
			return Collections.emptyList();
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (recordId != null && createTime != null) {
				pstmt = conn.prepareStatement("SELECT * FROM weizhu_tools_productclock_communicate_record WHERE company_id = ? AND "
						+ "user_id = ? AND customer_id = ? AND "
						+ "(create_time < ? or (create_time = ? AND record_id > ?)) ORDER BY create_time DESC, record_id ASC "
						+ "LIMIT ?; ");
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, userId);
				DBUtil.set(pstmt, 3, customerId);
				DBUtil.set(pstmt, 4, createTime);
				DBUtil.set(pstmt, 5, createTime);
				DBUtil.set(pstmt, 6, recordId);
				DBUtil.set(pstmt, 7, size);
			} else {
				pstmt = conn.prepareStatement("SELECT * FROM weizhu_tools_productclock_communicate_record WHERE company_id = ? AND "
						+ "user_id = ? AND customer_id = ? "
						+ "ORDER BY create_time DESC, record_id ASC LIMIT ?; ");
				DBUtil.set(pstmt, 1, companyId);
				DBUtil.set(pstmt, 2, userId);
				DBUtil.set(pstmt, 3, customerId);
				DBUtil.set(pstmt, 4, size);
			}

			rs = pstmt.executeQuery();
			
			List<ProductclockProtos.CommunicateRecord> communicateRecordList = Lists.newArrayList();
			ProductclockProtos.CommunicateRecord.Builder communicateRecordBuilder = ProductclockProtos.CommunicateRecord.newBuilder();
			while (rs.next()) {
				communicateRecordBuilder.clear();
				
				COMMUNICATE_RECORD.mapToItem(rs, communicateRecordBuilder);
				
				communicateRecordList.add(communicateRecordBuilder.build());
			}
			
			return communicateRecordList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	/**
	 * 获取交流信息（管理后台）
	 * @param conn
	 * @param companyId
	 * @param customerId
	 * @param start
	 * @param length
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<ProductclockProtos.CommunicateRecord> getCommunicateRecordPage(Connection conn, long companyId,
			int customerId, int start, int length) throws SQLException {
		if (length < 1) {
			return new DataPage<ProductclockProtos.CommunicateRecord>(Collections.emptyList(), 0, 0);
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("SELECT COUNT(1) AS total FROM weizhu_tools_productclock_communicate_record WHERE company_id = ? AND customer_id = ?; "
					+ "SELECT * FROM weizhu_tools_productclock_communicate_record WHERE company_id = ? AND customer_id = ? ORDER BY create_time DESC LIMIT ?, ?; ");
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, customerId);
			DBUtil.set(pstmt, 3, companyId);
			DBUtil.set(pstmt, 4, customerId);
			DBUtil.set(pstmt, 5, start);
			DBUtil.set(pstmt, 6, length);
			
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
			
			List<ProductclockProtos.CommunicateRecord> recordList = Lists.newArrayList();
			ProductclockProtos.CommunicateRecord.Builder builder = ProductclockProtos.CommunicateRecord.newBuilder();
			while (rs.next()) {
				builder.clear();
				
				COMMUNICATE_RECORD.mapToItem(rs, builder);
				
				recordList.add(builder.build());
			}
			return new DataPage<ProductclockProtos.CommunicateRecord>(recordList, total, total);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(conn);
		}
	}
	
	/**
	 * 增加交流信息
	 * @param conn         
	 * @param companyId 
	 * @param customerId   顾客id
	 * @param contentText  内容
	 * @param createTime   创建时间
	 * @param createUserId 创建用户
	 * @return
	 * @throws SQLException
	 */
	public static int insertCommunicateRecord(Connection conn, long companyId, 
			int customerId, String contentText, 
			int createTime, long createUserId) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_tools_productclock_communicate_record (company_id, user_id, customer_id, content_text, create_time) "
					+ "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			DBUtil.set(pstmt, 1, companyId);
			DBUtil.set(pstmt, 2, createUserId);
			DBUtil.set(pstmt, 3, customerId);
			DBUtil.set(pstmt, 4, contentText);
			DBUtil.set(pstmt, 5, createTime);
			
			pstmt.execute();
			rs = pstmt.getGeneratedKeys();
			if (!rs.next()) {
				throw new RuntimeException("cannot get key!");
			}
			
			return rs.getInt(1);
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void updateCommunicateRecord(Connection conn, long companyId, 
			String contentText, int recordId) throws SQLException {
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_tools_productclock_communicate_record SET content_text = ? WHERE company_id = ? AND record_id = ?; ");
			DBUtil.set(pstmt, 1, contentText);
			DBUtil.set(pstmt, 2, companyId);
			DBUtil.set(pstmt, 3, recordId);
			
			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}
	
	public static void deleteCommunicateRecord(Connection conn, long companyId,
			List<Integer> recordIdList) throws SQLException {
		if (recordIdList.isEmpty()) {
			return;
		}
		
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("DELETE FROM weizhu_tools_productclock_communicate_record WHERE company_id = ").append(companyId).append(" AND ");
		sqlBuilder.append("record_id IN (").append(DBUtil.COMMA_JOINER.join(recordIdList)).append("); ");
		
		String sql = sqlBuilder.toString();
		
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} finally {
			DBUtil.closeQuietly(stmt);
		}
	}
	
	/**
	 * 获取开启提醒的用户
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static Map<Long, List<Integer>> getRemindCustomerId(Connection conn) throws SQLException {
		String sql = "SELECT company_id, customer_id FROM weizhu_tools_productclock_customer WHERE is_remind = 1; ";
		
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			
			Map<Long, List<Integer>> companyCustomerMap = Maps.newHashMap();
			List<Integer> customerIdList = null;
			while (rs.next()) {
				long companyId = rs.getLong("company_id");
				customerIdList = companyCustomerMap.get(companyId);
				if (customerIdList == null) {
					customerIdList = Lists.newArrayList();
				}
				customerIdList.add(rs.getInt("customer_id"));
				
				companyCustomerMap.put(companyId, customerIdList);
			}
			
			return companyCustomerMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}

}
