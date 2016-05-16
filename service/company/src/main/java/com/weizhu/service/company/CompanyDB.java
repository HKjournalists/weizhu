package com.weizhu.service.company;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.proto.CompanyProtos;

public class CompanyDB {

	private static final ProtobufMapper<CompanyProtos.Company> COMPANY_MAPPER = 
			ProtobufMapper.createMapper(CompanyProtos.Company.getDefaultInstance(),
					"company_id",
					"company_name",
					"server_name");
	
	public static Map<Long, CompanyProtos.Company> getCompany(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("SELECT * FROM weizhu_company_key; SELECT * FROM weizhu_company ORDER BY company_id ASC; ");
			rs = stmt.getResultSet();
			
			Map<Long, List<String>> companyKeyMap = new TreeMap<Long, List<String>>();
			while (rs.next()) {
				DBUtil.addMapArrayList(companyKeyMap, rs.getLong("company_id"), rs.getString("company_key"));
			}
			
			DBUtil.closeQuietly(rs);
			rs = null;
			
			stmt.getMoreResults();
			rs = stmt.getResultSet();
			
			Map<Long, CompanyProtos.Company> companyMap = new TreeMap<Long, CompanyProtos.Company>();
			
			CompanyProtos.Company.Builder tmpCompanyBuilder = CompanyProtos.Company.newBuilder();
			while (rs.next()) {
				tmpCompanyBuilder.clear();
				
				COMPANY_MAPPER.mapToItem(rs, tmpCompanyBuilder);
				
				long companyId = tmpCompanyBuilder.getCompanyId();
				
				List<String> companyKeyList = companyKeyMap.get(companyId);
				if (companyKeyList != null) {
					tmpCompanyBuilder.addAllCompanyKey(companyKeyList);
				}
				
				companyMap.put(companyId, tmpCompanyBuilder.build());
			}
			
			return companyMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
	
	private static final ProtobufMapper<CompanyProtos.Server.Address> SERVER_ADDRESS_MAPPER = 
			ProtobufMapper.createMapper(CompanyProtos.Server.Address.getDefaultInstance(), 
					"host",
					"port");
	
	public static Map<String, CompanyProtos.Server> getServer(Connection conn) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM weizhu_company_server_address ORDER BY server_name ASC; ");
			
			Map<String, List<CompanyProtos.Server.Address>> serverAddressMap = new TreeMap<String, List<CompanyProtos.Server.Address>>();
			CompanyProtos.Server.Address.Builder tmpRpcAddressBuilder = CompanyProtos.Server.Address.newBuilder();
			while (rs.next()) {
				tmpRpcAddressBuilder.clear();
				
				String serverName = rs.getString("server_name");
				CompanyProtos.Server.Address address = SERVER_ADDRESS_MAPPER.mapToItem(rs, tmpRpcAddressBuilder).build();
				
				DBUtil.addMapArrayList(serverAddressMap, serverName, address);
			}
			
			Map<String, CompanyProtos.Server> serverMap = new TreeMap<String, CompanyProtos.Server>();
			
			CompanyProtos.Server.Builder tmpServerBuilder = CompanyProtos.Server.newBuilder();
			for (Entry<String, List<CompanyProtos.Server.Address>> entry : serverAddressMap.entrySet()) {
				tmpServerBuilder.clear();
				
				CompanyProtos.Server server = tmpServerBuilder
						.setServerName(entry.getKey())
						.addAllAddress(entry.getValue())
						.build();
				
				serverMap.put(server.getServerName(), server);
			}
			
			return serverMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(stmt);
		}
	}
}
