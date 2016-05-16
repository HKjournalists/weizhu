package com.weizhu.web.filter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.CompanyProtos;

public class AdminInfo {

	private final AdminProtos.Admin admin;
	private final ImmutableMap<Long, CompanyProtos.Company> refCompanyMap;
	private final ImmutableMap<Integer, AdminProtos.Role> refRoleMap;
	
	public AdminInfo(AdminProtos.Admin admin, List<AdminProtos.Role> refRoleList, List<CompanyProtos.Company> refCompanyList) {		
		Map<Long, CompanyProtos.Company> tmpCompanyMap = new TreeMap<Long, CompanyProtos.Company>();
		for (CompanyProtos.Company company : refCompanyList) {
			tmpCompanyMap.put(company.getCompanyId(), company);
		}
		Map<Integer, AdminProtos.Role> tmpRoleMap = new TreeMap<Integer, AdminProtos.Role>();
		for (AdminProtos.Role role : refRoleList) {
			tmpRoleMap.put(role.getRoleId(), role);
		}
		
		AdminProtos.Admin.Builder adminBuilder = admin.toBuilder().clearCompany();
		AdminProtos.Admin.Company.Builder tmpBuilder = AdminProtos.Admin.Company.newBuilder();
		for (AdminProtos.Admin.Company c : admin.getCompanyList()) {
			// 判断admin关联的公司是否存在
			if (tmpCompanyMap.containsKey(c.getCompanyId())) {
				tmpBuilder.clear();
				
				tmpBuilder.mergeFrom(c).clearRoleId();
				for (Integer roleId : c.getRoleIdList()) {
					AdminProtos.Role role = tmpRoleMap.get(roleId);
					// 判断该公司关联的角色是否正确
					if (role != null && (!role.hasCompanyId() || role.getCompanyId() == c.getCompanyId())) {
						tmpBuilder.addRoleId(roleId);
					}
				}
				
				// 将正确的管理员关联公司信息添加
				adminBuilder.addCompany(tmpBuilder.build());
			}
		}
		
		// 管理员关联的公司信息和角色信息校验完毕
		this.admin = adminBuilder.build();

		Map<Long, CompanyProtos.Company> refCompanyMap = Maps.newTreeMap();
		Map<Integer, AdminProtos.Role> refRoleMap = Maps.newTreeMap();
		for (AdminProtos.Admin.Company c : this.admin.getCompanyList()) {
			refCompanyMap.put(c.getCompanyId(), tmpCompanyMap.get(c.getCompanyId()));
			for (Integer roleId : c.getRoleIdList()) {
				refRoleMap.put(roleId, tmpRoleMap.get(roleId));
			}
		}
		this.refCompanyMap = ImmutableMap.copyOf(refCompanyMap);
		this.refRoleMap = ImmutableMap.copyOf(refRoleMap);
	}
	
	public AdminProtos.Admin admin() {
		return admin;
	}
	
	public ImmutableMap<Long, CompanyProtos.Company> refCompanyMap() {
		return refCompanyMap;
	}

	public ImmutableMap<Integer, AdminProtos.Role> refRoleMap() {
		return refRoleMap;
	}
}
