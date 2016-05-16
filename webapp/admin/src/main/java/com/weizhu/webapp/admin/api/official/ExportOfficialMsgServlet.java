package com.weizhu.webapp.admin.api.official;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialMessageResponse;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListRequest;
import com.weizhu.proto.AdminOfficialProtos.GetOfficialSendPlanListResponse;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminProtos.GetAdminByIdRequest;
import com.weizhu.proto.AdminProtos.GetAdminByIdResponse;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class ExportOfficialMsgServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	private final AdminService adminService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService ;
	
	@Inject
	public ExportOfficialMsgServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService, AdminService adminService,
			AdminUserService adminUserService,
			UploadService uploadService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
		this.adminService = adminService;
		this.adminUserService = adminUserService;
		this.uploadService = uploadService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		Long officialId = ParamUtil.getLong(httpRequest, "official_id", null);
		
		if (officialId == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_OFFICIAL_INVALID");
			result.addProperty("fail_text", "请指定需要导出的服务号！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead head = adminHeadProvider.get();
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		
		// 写入execl（服务号发送的消息）
		HSSFWorkbook wb = new HSSFWorkbook();
		// 写服务号发送消息的表头
		HSSFSheet officialSendSheet = wb.createSheet("服务号发送消息");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 5);
		officialSendSheet.addMergedRegion(cra);

		HSSFRow headRow = officialSendSheet.createRow(0);
		HSSFCellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		headStyle.setAlignment(HSSFCellStyle.BIG_SPOTS);
		HSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("服务号发送消息");
		headCell.setCellStyle(headStyle);
		
		// 发送内容格式
		HSSFCellStyle style = wb.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		HSSFRow row1 = officialSendSheet.createRow(1); // 标题行
		HSSFCell cell1 = row1.createCell(0);
		cell1.setCellValue("消息内容");
		cell1.setCellStyle(style);
		cell1 = row1.createCell(1);
		cell1.setCellValue("创建者");
		cell1.setCellStyle(style);
		cell1 = row1.createCell(2);
		cell1.setCellValue("创建时间");
		cell1.setCellStyle(style);
		cell1 = row1.createCell(3);
		cell1.setCellValue("消息发送时间");
		cell1.setCellStyle(style);
		cell1 = row1.createCell(4);
		cell1.setCellValue("状态");
		cell1.setCellStyle(style);
		
		boolean hasMore = true;
		int start = 0;
		int rowIdx = 2;
		while (hasMore) {
			// 获取服务号发送列表
			GetOfficialSendPlanListRequest sendPlanListRequest = GetOfficialSendPlanListRequest.newBuilder()
					.setOfficialId(officialId)
					.setStart(start)
					.setLength(100)
					.build();
			start += 100;
			
			GetOfficialSendPlanListResponse sendPlanListResponse = Futures.getUnchecked(
					adminOfficialService.getOfficialSendPlanList(head, sendPlanListRequest));
			
			if (sendPlanListResponse.getTotalSize() < 100) {
				hasMore = false;
			}
			
			// 查询出adminUser,userId
			Set<Long> refAdminIdSet = new TreeSet<Long>();
			Set<Long> refUserIdSet = new TreeSet<Long>();
			for (AdminOfficialProtos.OfficialSendPlan officialSendPlan : sendPlanListResponse.getOfficialSendPlanList()) {
				if (officialSendPlan.hasCreateAdminId()) {
					refAdminIdSet.add(officialSendPlan.getCreateAdminId());
				}
				if (officialSendPlan.getSendMsg().getMsgTypeCase().equals(OfficialProtos.OfficialMessage.MsgTypeCase.USER)) {
					refUserIdSet.add(officialSendPlan.getSendMsg().getUser().getUserId());
				}
			}

			Map<Long, AdminProtos.Admin> refAdminMap;
			if (refAdminIdSet.isEmpty()) {
				refAdminMap = Collections.emptyMap();
			} else {
				
				GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(
						adminService.getAdminById(head, 
								GetAdminByIdRequest.newBuilder().addAllAdminId(refAdminIdSet).build()
								));
				
				refAdminMap = new TreeMap<Long, AdminProtos.Admin>();
				for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
					refAdminMap.put(admin.getAdminId(), admin);
				}
			}
			
			Map<Long, UserProtos.User> refUserMap;
			GetUserByIdResponse getUserByIdResponse = null;
			if (!refUserIdSet.isEmpty()) {
				getUserByIdResponse = Futures.getUnchecked(
						this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
								.addAllUserId(refUserIdSet)
								.build()));
				refUserMap = new TreeMap<Long, UserProtos.User>();
				for (UserProtos.User user : getUserByIdResponse.getUserList()) {
					refUserMap.put(user.getBase().getUserId(), user);
				}
			} else {
				refUserMap = Collections.emptyMap();
			}
			
			Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
			Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
			Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>();
			
			if (getUserByIdResponse != null) {
				for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
					UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
					teamMap.put(team.getTeamId(), team);
				}
				
				for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
					UserProtos.Position position = getUserByIdResponse
							.getRefPosition(i);
					positionMap.put(position.getPositionId(), position);
				}
				
				for (int i = 0; i < getUserByIdResponse.getRefLevelCount(); ++i) {
					UserProtos.Level level = getUserByIdResponse.getRefLevel(i);
					levelMap.put(level.getLevelId(), level);
				}
			}
			
			try {
				rowIdx = writeSendMsgExecl(wb, officialSendSheet, rowIdx, sendPlanListResponse, refAdminMap, refUserMap, teamMap, positionMap, levelMap, imageUrlPrefix);
			} catch (Exception ex) {
				JsonObject result = new JsonObject();
				result.addProperty("result", "FAIL_WRITE_EXECL_INVALID");
				result.addProperty("fail_text", "生成execl出错！");

				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.GSON.toJson(result, httpResponse.getWriter());
				return ;
			}
		}
		
		// 写接收的execl表头
		HSSFSheet officialRecvSheet = wb.createSheet("服务号接收消息");
		cra = new CellRangeAddress(0, 0, 0, 5);
		officialRecvSheet.addMergedRegion(cra);

		headRow = officialRecvSheet.createRow(0);
		headStyle = wb.createCellStyle();
		headStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		headStyle.setAlignment(HSSFCellStyle.BIG_SPOTS);
		headCell = headRow.createCell(0);
		headCell.setCellValue("服务号接收消息");
		headCell.setCellStyle(headStyle);
		
		// 发送内容格式
		style = wb.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		row1 = officialRecvSheet.createRow(1); // 标题行
		HSSFCell cell = row1.createCell(0);
		cell.setCellValue("一级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(1);
		cell.setCellValue("二级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(2);
		cell.setCellValue("三级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(3);
		cell.setCellValue("四级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(4);
		cell.setCellValue("五级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(5);
		cell.setCellValue("六级部门");
		cell.setCellStyle(style);
		cell = row1.createCell(6);
		cell.setCellValue("岗位");
		cell.setCellStyle(style);
		cell = row1.createCell(7);
		cell.setCellValue("姓名");
		cell.setCellStyle(style);
		cell = row1.createCell(8);
		cell.setCellValue("工号");
		cell.setCellStyle(style);
		cell = row1.createCell(9);
		cell.setCellValue("电话");
		cell.setCellStyle(style);
		cell = row1.createCell(10);
		cell.setCellValue("发送消息时间");
		cell.setCellStyle(style);
		cell = row1.createCell(11);
		cell.setCellValue("消息内容");
		cell.setCellStyle(style);
		
		boolean hasMore1 = true;
		int start1 = 0;
		int rowIdx1 = 2;
		while (hasMore1) {
			// 获取服务号发送列表
			GetOfficialMessageRequest recvMessageRequest = GetOfficialMessageRequest.newBuilder()
					.setOfficialId(officialId)
					.setStart(start1)
					.setLength(100)
					.setIsFromUser(true)
					.build();
			start1 += 100;
			GetOfficialMessageResponse recvMessageResponse = Futures.getUnchecked(adminOfficialService.getOfficialMessage(head, recvMessageRequest));
			
			if (recvMessageResponse.getTotalSize() < 100) {
				hasMore1 = false;
			}
			
			// 查询出adminUser,userId
			Set<Long> refAdminIdSet = new TreeSet<Long>();
			Set<Long> refUserIdSet = new TreeSet<Long>();
			for (AdminOfficialProtos.OfficialMessageInfo officialMessageInfo : recvMessageResponse.getMsgInfoList()) {
				refUserIdSet.add(officialMessageInfo.getUserId());
				
				if (officialMessageInfo.getMsg().getMsgTypeCase() == OfficialProtos.OfficialMessage.MsgTypeCase.USER) {
					refUserIdSet.add(officialMessageInfo.getMsg().getUser().getUserId());
				}
			}

			Map<Long, AdminProtos.Admin> refAdminMap;
			if (refAdminIdSet.isEmpty()) {
				refAdminMap = Collections.emptyMap();
			} else {
				
				GetAdminByIdResponse getAdminByIdResponse = Futures.getUnchecked(
						adminService.getAdminById(head, 
								GetAdminByIdRequest.newBuilder().addAllAdminId(refAdminIdSet).build()
								));
				
				refAdminMap = new TreeMap<Long, AdminProtos.Admin>();
				for (AdminProtos.Admin admin : getAdminByIdResponse.getAdminList()) {
					refAdminMap.put(admin.getAdminId(), admin);
				}
			}
			
			Map<Long, UserProtos.User> refUserMap;
			GetUserByIdResponse getUserByIdResponse = null;
			if (!refUserIdSet.isEmpty()) {
				getUserByIdResponse = Futures.getUnchecked(
						this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
								.addAllUserId(refUserIdSet)
								.build()));
				refUserMap = new TreeMap<Long, UserProtos.User>();
				for (UserProtos.User user : getUserByIdResponse.getUserList()) {
					refUserMap.put(user.getBase().getUserId(), user);
				}
			} else {
				refUserMap = Collections.emptyMap();
			}
			
			Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
			Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
			Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>();
			
			if (getUserByIdResponse != null) {
				for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
					UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
					teamMap.put(team.getTeamId(), team);
				}
				
				for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
					UserProtos.Position position = getUserByIdResponse
							.getRefPosition(i);
					positionMap.put(position.getPositionId(), position);
				}
				
				for (int i = 0; i < getUserByIdResponse.getRefLevelCount(); ++i) {
					UserProtos.Level level = getUserByIdResponse.getRefLevel(i);
					levelMap.put(level.getLevelId(), level);
				}
			}
			
			try {
				rowIdx = writeRecvMsg(wb, officialRecvSheet, rowIdx1, recvMessageResponse, refAdminMap, refUserMap, teamMap, positionMap, levelMap, imageUrlPrefix);
			} catch (Exception ex) {
				JsonObject result = new JsonObject();
				result.addProperty("result", "FAIL_WRITE_EXECL_INVALID");
				result.addProperty("fail_text", "生成execl出错！");

				httpResponse.setContentType("application/json;charset=UTF-8");
				JsonUtil.GSON.toJson(result, httpResponse.getWriter());
				return ;
			}
		}

		String path = "./OfficialMessage";  
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
		
		String name = "official_message.xls";
		File fileName = new File(path + "/" + name);
		fileName.createNewFile();
		
		try {
			FileOutputStream fo = new FileOutputStream(path + "/" + name);
			wb.write(fo);
			fo.close();
		} catch (IOException ex) {
			throw new RuntimeException("io exception", ex);
		} finally {
		}
		
		File fileRead = new File(path + "/" + name);
		HSSFWorkbook wbRead = new HSSFWorkbook(new FileInputStream(fileRead));
		
		httpResponse.addHeader("Content-Disposition", "attachment;filename=" + new String(name.getBytes("utf-8"),"iso8859-1"));
		httpResponse.addHeader("Content-Length", "" + fileRead.length());
		httpResponse.setContentType("application/vnd.ms-excel; charset=utf-8");
		
		OutputStream os = null;
		try {
			os = httpResponse.getOutputStream();
			wbRead.write(os);
		} catch (Exception ex) {
			throw new RuntimeException("io exception", ex);
		} finally {
			wbRead.close();
			os.flush();
			os.close();
			wb.close();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private int writeSendMsgExecl(HSSFWorkbook wb, HSSFSheet officialSendSheet, int rowIdx, GetOfficialSendPlanListResponse sendPlanListResponse,
			Map<Long, AdminProtos.Admin> refAdminMap,
			Map<Long, UserProtos.User> refUserMap,
			Map<Integer, UserProtos.Team> teamMap,
			Map<Integer, UserProtos.Position> positionMap,
			Map<Integer, UserProtos.Level> levelMap,
			String imageUrlPrefix) {
		

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (AdminOfficialProtos.OfficialSendPlan sendPlan : sendPlanListResponse.getOfficialSendPlanList()) {
			HSSFRow row = officialSendSheet.createRow(rowIdx++);
			
			int cellIdx = 0;
			
			HSSFCellStyle style = wb.createCellStyle();
			style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			
			HSSFCell cell = row.createCell(cellIdx++);
			cell.setCellValue(getSendMsg(sendPlan.getSendMsg(), refUserMap, imageUrlPrefix));
			cell.setCellStyle(style);
			
			cell = row.createCell(cellIdx++);
			cell.setCellValue(refAdminMap.get(sendPlan.getCreateAdminId()) != null ? refAdminMap.get(sendPlan.getCreateAdminId()).getAdminName() : "[未知管理员]");
			cell.setCellStyle(style);
			
			cell = row.createCell(cellIdx++);
			cell.setCellValue(sdf.format(new Date(sendPlan.getCreateTime() * 1000L)));
			cell.setCellStyle(style);
			
			cell = row.createCell(cellIdx++);
			cell.setCellValue(sdf.format(new Date(sendPlan.getSendTime() * 1000L)));
			cell.setCellStyle(style);
		
			cell = row.createCell(cellIdx++);
			cell.setCellValue(sendPlan.getSendState().name());
			cell.setCellStyle(style);
		}
		
		return rowIdx;
	}
	
	private int writeRecvMsg(HSSFWorkbook wb, HSSFSheet officialRecvSheet, int rowIdx, GetOfficialMessageResponse recvMessageResponse,
			Map<Long, AdminProtos.Admin> refAdminMap,
			Map<Long, UserProtos.User> refUserMap,
			Map<Integer, UserProtos.Team> teamMap,
			Map<Integer, UserProtos.Position> positionMap,
			Map<Integer, UserProtos.Level> levelMap,
			String imageUrlPrefix) {
		

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		for (AdminOfficialProtos.OfficialMessageInfo recvMsg : recvMessageResponse.getMsgInfoList()) {
			HSSFRow row = officialRecvSheet.createRow(rowIdx++);
			
			HSSFCellStyle style = wb.createCellStyle();
			style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			
			UserProtos.User user = refUserMap.get(recvMsg.getUserId());
			if (user != null) {
				// 写用户信息
				this.writeUserInfo(row, user, teamMap, positionMap, levelMap);
				HSSFCell cell = row.createCell(10);
				cell.setCellValue(sdf.format(new Date(recvMsg.getMsg().getMsgTime() * 1000L)));
				cell.setCellStyle(style);
				cell = row.createCell(11);
				cell.setCellValue(getSendMsg(recvMsg.getMsg(), refUserMap, imageUrlPrefix));
				cell.setCellStyle(style);
			}
		}
		
		return rowIdx;
	}
	
	private String getSendMsg(OfficialProtos.OfficialMessage msg, Map<Long, UserProtos.User> refUserMap, String imageUrlPrefix) {
		String value = null;
		switch (msg.getMsgTypeCase()) {
			case TEXT: {
				value = msg.getText().getContent();
				break;
			}
			case VOICE: {
				value = "语音";
				break;
			}
			case IMAGE: {
				value = imageUrlPrefix + msg.getImage().getName() + ",name=" + msg.getImage().getName();
				break;
			}
			case USER: {
				UserProtos.User u = refUserMap.get(msg.getUser().getUserId());
				value = u != null ? u.getBase().getUserName() : "未知[UserId:" + msg.getUser().getUserId() + "]";
				break;
			}
			case DISCOVER_ITEM: {
				value = "发现条目id:" + String.valueOf(msg.getDiscoverItem().getItemId());
				break;
			}
			default: {
				value = "[未知内容]";
				break;
			}
		}
		
		return value;
	}
	
	private void writeUserInfo(HSSFRow row, UserProtos.User user,
			Map<Integer, UserProtos.Team> teamMap, 
			Map<Integer, UserProtos.Position> positionMap, 
			Map<Integer, UserProtos.Level> levelMap) {
		HSSFCell cell = null;
		if (user != null) {

			if (user.getTeamCount() > 0) {
				UserProtos.UserTeam userTeam = user.getTeam(0);

				LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
				int tmpTeamId = userTeam.getTeamId();
				while (true) {
					UserProtos.Team team = teamMap.get(tmpTeamId);
					if (team == null) {
						// warn : cannot find team
						teamList.clear();
						break;
					}

					teamList.addFirst(team);

					if (team.hasParentTeamId()) {
						tmpTeamId = team.getParentTeamId();
					} else {
						break;
					}
				}
				while (teamList.size() < 6) {
					teamList.add(UserProtos.Team.newBuilder()
							.setTeamId(0)
							.setTeamName("")
							.build());
				}

				int teamCell = 0;
				for (UserProtos.Team team : teamList) {
					cell = row.createCell(teamCell);
					cell.setCellValue(team.getTeamName());
					teamCell++;
				}

				if (userTeam.hasPositionId()) {
					UserProtos.Position position = positionMap.get(userTeam.getPositionId());
					cell = row.createCell(6);
					cell.setCellValue(position == null ? "" : position.getPositionName());
				}
			}

			cell = row.createCell(7);
			cell.setCellValue(user.getBase().getUserName());

			cell = row.createCell(8);
			cell.setCellValue(user.getBase().getRawId());

			cell = row.createCell(9);
			StringBuilder mobileStr = new StringBuilder();
			cell.setCellValue(DBUtil.COMMA_JOINER.appendTo(mobileStr, user.getBase().getMobileNoList()).toString());
		}
	}
}
