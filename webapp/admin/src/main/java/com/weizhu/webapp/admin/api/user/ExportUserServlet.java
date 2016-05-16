package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagRequest;
import com.weizhu.proto.AdminUserProtos.GetUserAbilityTagResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.SessionProtos.GetSessionDataRequest;
import com.weizhu.proto.SessionProtos.GetSessionDataResponse;
import com.weizhu.web.ParamUtil;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.SessionProtos;
import com.weizhu.proto.SessionService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.WeizhuProtos;

@Singleton
@SuppressWarnings("serial")
public class ExportUserServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	private final AdminService adminService;
	private final SessionService sessionService;
	
	@Inject
	public ExportUserServlet(Provider<AdminHead> adminHeadProvider, 
			AdminUserService adminUserService,
			AdminService adminService, 
			SessionService sessionService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
		this.sessionService = sessionService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		// 1. 取出参数
		Boolean isExpert = ParamUtil.getBoolean(httpRequest, "is_expert", null);
		Integer teamId = ParamUtil.getInt(httpRequest, "team_id", null);
		Integer positionId = ParamUtil.getInt(httpRequest, "position_id", null);
		String keyword = ParamUtil.getString(httpRequest, "keyword", null);
		String mobileNo = ParamUtil.getString(httpRequest, "mobile_no", null);
		Boolean hasSessionData = ParamUtil.getBoolean(httpRequest, "has_session_data", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		List<String> extendsNameList = Futures.getUnchecked(this.adminUserService.getUserExtendsName(head, ServiceUtil.EMPTY_REQUEST)).getExtendsNameList();
		
		GetUserListRequest.Builder requestBuilder = GetUserListRequest.newBuilder();
		if (isExpert != null) {
			requestBuilder.setIsExpert(isExpert);
		}
		if (teamId != null) {
			requestBuilder.setTeamId(teamId);
		}
		if (positionId != null) {
			requestBuilder.setPositionId(positionId);
		}
		if (keyword != null) {
			requestBuilder.setKeyword(keyword);
		}
		if (mobileNo != null) {
			requestBuilder.setMobileNo(mobileNo);
		}
		
		final GetUserListRequest request = requestBuilder.buildPartial();
		
		XSSFWorkbook wb0 = null;
		SXSSFWorkbook wb = null;
		try {
			wb0 = new XSSFWorkbook(Resources.getResource("com/weizhu/webapp/admin/api/user/user_template.xlsx").openStream());
			
			LinkedHashMap<String, Integer> extsNameToIdxMap = new LinkedHashMap<String, Integer>();
			Row headRow = wb0.getSheetAt(0).getRow(11);
			for (int i=0; i<5; ++i) {
				int cellIdx = 18 + i;
				if (i < extendsNameList.size()) {
					String extsName = extendsNameList.get(i);
					extsNameToIdxMap.put(extsName, cellIdx);
					headRow.getCell(cellIdx).setCellValue(extsName);
				} else {
					headRow.getCell(cellIdx).setCellValue("");
				}
			}
			
			wb = new SXSSFWorkbook(wb0, -1);
			wb0 = null;
			SXSSFSheet sheet = wb.getSheetAt(0);
			
			CellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			Font font = wb.createFont();
			font.setFontHeight((short)200);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);
			
			int rowIdx = 12;
			
			int start = 0;
			final int length = 500;
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while (true) {
				
				GetUserListResponse response = Futures.getUnchecked(
						adminUserService.getUserList(head, 
								request.toBuilder()
									.setStart(start)
									.setLength(length)
									.build()));
				
				Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
				for (UserProtos.Team team : response.getRefTeamList()) {
					refTeamMap.put(team.getTeamId(), team);
				}
				
				Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
				for (UserProtos.Position position : response.getRefPositionList()) {
					refPositionMap.put(position.getPositionId(), position);
				}
				
				Map<Integer, UserProtos.Level> refLevelMap = new TreeMap<Integer, UserProtos.Level>();
				for (UserProtos.Level level : response.getRefLevelList()) {
					refLevelMap.put(level.getLevelId(), level);
				}
				
				Set<Long> adminIdSet = new TreeSet<Long>();
				for (UserProtos.User user : response.getUserList()) {
					if (user.getBase().hasCreateAdminId()) {
						adminIdSet.add(user.getBase().getCreateAdminId());
					}
					if (user.getBase().hasUpdateAdminId()) {
						adminIdSet.add(user.getBase().getUpdateAdminId());
					}
				}
				
				GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(
						this.adminService.getAdminById(head, 
								GetAdminByIdRequest.newBuilder()
									.addAllAdminId(adminIdSet)
									.build()));
				
				Map<Long, AdminProtos.Admin> refAdminMap = new TreeMap<Long, AdminProtos.Admin>();
				for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
					refAdminMap.put(admin.getAdminId(), admin);
				}
				
				Set<Long> userIdSet = new TreeSet<Long>();
				for (UserProtos.User user : response.getUserList()) {
					userIdSet.add(user.getBase().getUserId());
				}
				
				GetUserAbilityTagResponse getUserAbilityTagResponse = Futures.getUnchecked(
						this.adminUserService.getUserAbilityTag(head, 
								GetUserAbilityTagRequest.newBuilder()
									.addAllUserId(userIdSet)
									.build()));
				
				Map<Long, List<String>> refAbilityTagMap = new TreeMap<Long, List<String>>();
				for (UserProtos.UserAbilityTag tag : getUserAbilityTagResponse.getAbilityTagList()) {
					List<String> list = refAbilityTagMap.get(tag.getUserId());
					if (list == null) {
						list = new ArrayList<String>();
						refAbilityTagMap.put(tag.getUserId(), list);
					}
					list.add(tag.getTagName());
				}
				
				GetSessionDataResponse getSessionDataResponse = Futures.getUnchecked(
						this.sessionService.getSessionData(head, 
								GetSessionDataRequest.newBuilder()
									.addAllUserId(userIdSet)
									.build()));
				
				Map<Long, SessionProtos.SessionData> refSessionDataMap = new TreeMap<Long, SessionProtos.SessionData>();
				for (SessionProtos.SessionData data : getSessionDataResponse.getSessionDataList()) {
					SessionProtos.SessionData d = refSessionDataMap.get(data.getSession().getUserId());
					if (d == null || d.getActiveTime() < data.getActiveTime()) {
						refSessionDataMap.put(data.getSession().getUserId(), data);
					}
				}
				
				for (UserProtos.User user : response.getUserList()) {
					if (hasSessionData == null || (hasSessionData == refSessionDataMap.containsKey(user.getBase().getUserId())) ) {
						Row row = sheet.createRow(rowIdx++);
						processCell(row, extsNameToIdxMap, user, refTeamMap, refPositionMap, refLevelMap, refAdminMap, refAbilityTagMap, refSessionDataMap, df);
						
						for (int i=0; i<=33; ++i) {
							Cell cell = row.getCell(i);
							if (cell == null) {
								cell = row.createCell(i);
								cell.setCellValue("");
							}
							cell.setCellStyle(cellStyle);
						}
					}
				}
				
				sheet.flushRows();
				
				start += length;
				if (start >= response.getFilteredSize()) {
					break;
				}
			}
			
//			for (Entry<String, Integer> entry : extsNameToIdxMap.entrySet()) {
//				sheet.getRow(11).getCell(entry.getValue()).setCellValue(entry.getKey());
//			}
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_user.xlsx");
			
			wb.write(httpResponse.getOutputStream());
			wb.dispose();
		} finally {
			if (wb0 != null) {
				wb0.close();
			}
			if (wb != null) {
				wb.close();
			}
		}
	}
	
	private void processCell(Row row, LinkedHashMap<String, Integer> extsNameToIdxMap, 
			UserProtos.User user, 
			Map<Integer, UserProtos.Team> refTeamMap, 
			Map<Integer, UserProtos.Position> refPositionMap, 
			Map<Integer, UserProtos.Level> refLevelMap,
			Map<Long, AdminProtos.Admin> refAdminMap,
			Map<Long, List<String>> refAbilityTagMap,
			Map<Long, SessionProtos.SessionData> refSessionDataMap,
			DateFormat df
			) {
		
		row.createCell(0).setCellValue(user.getBase().getRawId());
		row.createCell(1).setCellValue(user.getBase().getUserName());
		
		LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
		UserProtos.Position position = null;
		if (user.getTeamCount() > 0) {
			UserProtos.UserTeam userTeam = user.getTeamList().get(0);
			
			int teamId = userTeam.getTeamId();
			while (true) {
				UserProtos.Team team = refTeamMap.get(teamId);
				if (team == null) {
					teamList.clear();
					break;
				}
				teamList.addFirst(team);
				if (!team.hasParentTeamId()) {
					break;
				}
				teamId = team.getParentTeamId();
			}
			
			if (userTeam.hasPositionId()) {
				position = refPositionMap.get(userTeam.getPositionId());
			}
		}
		
		for (int i=0; i<8; ++i) {
			String teamName = i < teamList.size() ? teamList.get(i).getTeamName() : "";
			row.createCell(2 + i).setCellValue(teamName);
		}
		
		if (user.getBase().hasGender()) {
			String value;
			switch (user.getBase().getGender()) {
				case FEMALE:
					value = "女";
					break;
				case MALE:
					value = "男";
					break;
				default:
					value = "";
					break;
			}
			row.createCell(10).setCellValue(value);
		} else {
			row.createCell(10).setCellValue("");
		}
		
		row.createCell(11).setCellValue(position == null ? "" : position.getPositionName());
		
		if (user.getBase().hasLevelId()) {
			UserProtos.Level level = refLevelMap.get(user.getBase().getLevelId());
			row.createCell(12).setCellValue(level == null ? "" : level.getLevelName());
		} else {
			row.createCell(12).setCellValue("");
		}
		
		row.createCell(13).setCellValue(DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()));
		row.createCell(14).setCellValue(DBUtil.COMMA_JOINER.join(user.getBase().getPhoneNoList()));
		row.createCell(15).setCellValue(user.getBase().hasEmail() ? user.getBase().getEmail() : "");
		
		if (user.getBase().hasIsExpert()) {
			row.createCell(16).setCellValue(user.getBase().getIsExpert() ? "是" : "否");
		} else {
			row.createCell(16).setCellValue("");
		}
		
		List<String> abilityTagList = refAbilityTagMap.get(user.getBase().getUserId());
		if (abilityTagList == null) {
			abilityTagList = Collections.emptyList();
		}
		row.createCell(17).setCellValue(DBUtil.COMMA_JOINER.join(abilityTagList));
		
		for (UserProtos.UserExtends exts : user.getExtList()) {
			Integer extsIdx = extsNameToIdxMap.get(exts.getName());
			
			/*
			if (extsIdx == null && extsNameToIdxMap.size() < 5) {
				extsIdx = 18 + extsNameToIdxMap.size();
				extsNameToIdxMap.put(exts.getName(), extsIdx);
			}
			*/
			
			if (extsIdx != null) {
				row.createCell(extsIdx).setCellValue(exts.getValue());
			}
		}
		
		// 系统内部信息
		
		row.createCell(23).setCellValue(String.valueOf(user.getBase().getUserId()));
		
		String stateStr;
		switch (user.getBase().getState()) {
			case NORMAL:
				stateStr = "正常";
				break;
			case DISABLE:
				stateStr = "禁用";
				break;
			case DELETE:
				stateStr = "已删除";
				break;
			case APPROVE:
				stateStr = "审核中";
				break;
			default:
				stateStr = "";
				break;
		}
		row.createCell(24).setCellValue(stateStr);
		
		row.createCell(25).setCellValue(user.getBase().hasCreateTime() ? df.format(new Date(user.getBase().getCreateTime() * 1000L)) : "");
		
		if (user.getBase().hasCreateAdminId()) {
			AdminProtos.Admin admin = refAdminMap.get(user.getBase().getCreateAdminId());
			row.createCell(26).setCellValue(admin == null ? "" : admin.getAdminName());
		} else {
			row.createCell(26).setCellValue("");
		}
		
		row.createCell(27).setCellValue(user.getBase().hasUpdateTime() ? df.format(new Date(user.getBase().getUpdateTime() * 1000L)) : "");
		
		if (user.getBase().hasUpdateAdminId()) {
			AdminProtos.Admin admin = refAdminMap.get(user.getBase().getUpdateAdminId());
			row.createCell(28).setCellValue(admin == null ? "" : admin.getAdminName());
		} else {
			row.createCell(28).setCellValue("");
		}
		
		// 在线信息
		
		SessionProtos.SessionData sessionData = refSessionDataMap.get(user.getBase().getUserId());
		if (sessionData != null) {
			row.createCell(29).setCellValue(df.format(new Date(sessionData.getLoginTime() * 1000L)));
			row.createCell(30).setCellValue(df.format(new Date(sessionData.getActiveTime() * 1000L)));
			if (sessionData.hasWeizhu()) {
				row.createCell(31).setCellValue(sessionData.getWeizhu().getPlatform().name());
				row.createCell(32).setCellValue(sessionData.getWeizhu().getVersionName());
				
				StringBuilder sb = new StringBuilder();
				if (sessionData.hasAndroid()) {
					final WeizhuProtos.Android android = sessionData.getAndroid();
					sb.append("[Android:");
					sb.append(android.getDevice()).append("/");
					sb.append(android.getManufacturer()).append("/");
					sb.append(android.getBrand()).append("/");
					sb.append(android.getModel()).append("/");
					sb.append(android.getSerial()).append("/");
					sb.append(android.getRelease()).append("/");
					sb.append(android.getSdkInt()).append("/");
					sb.append(android.getCodename()).append("]");
				}
				if (sessionData.hasIphone()) {
					final WeizhuProtos.Iphone iphone = sessionData.getIphone();
					sb.append("[Iphone:");
					sb.append(iphone.getName()).append("/");
					sb.append(iphone.getSystemName()).append("/");
					sb.append(iphone.getSystemVersion()).append("/");
					sb.append(iphone.getModel()).append("/");
					sb.append(iphone.getLocalizedModel()).append("/");
					sb.append(iphone.getDeviceToken()).append("/");
					sb.append(iphone.getMac()).append("/");
					sb.append(iphone.getAppId()).append("]");
				}
				row.createCell(33).setCellValue(sb.toString());
			}
		}
	} 
	
}
