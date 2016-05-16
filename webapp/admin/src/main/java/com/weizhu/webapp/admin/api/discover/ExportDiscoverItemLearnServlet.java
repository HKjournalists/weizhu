package com.weizhu.webapp.admin.api.discover;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExportDiscoverItemLearnServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final AdminUserService adminUserService;
	
	@Inject
	public ExportDiscoverItemLearnServlet(Provider<AdminHead> adminHeadProvider, AdminDiscoverService adminDiscoverService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final long itemId = ParamUtil.getLong(httpRequest, "item_id", -1L);
		
		final AdminHead head = this.adminHeadProvider.get();
		
		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(Resources.getResource("com/weizhu/webapp/admin/api/discover/export_discover_item_learn_file.xlsx").openStream()));
		try {
			final Set<Long> learnUserIdSet = new TreeSet<Long>();
			
			final CellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			Font font = wb.createFont();
			font.setFontHeight((short)200);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);
			// titleStyle.setFillForegroundColor(XSSFColor.LIGHT_ORANGE.index);    //填充的背景颜色
			// titleStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);    //填充图案
			
			final Sheet learnSheet = wb.getSheetAt(0);
			int learnRowIdx = 2;
			int learnStart = 0;
			final int learnLength = 500;
			while (true) {
				AdminDiscoverProtos.GetItemLearnListResponse response = Futures.getUnchecked(
						this.adminDiscoverService.getItemLearnList(head, AdminDiscoverProtos.GetItemLearnListRequest.newBuilder()
								.setItemId(itemId)
								.setStart(learnStart)
								.setLength(learnLength)
								.build()));
				// 获取user信息
				Set<Long> userIdSet = new TreeSet<Long>();
				for (DiscoverV2Protos.ItemLearn itemLearn : response.getItemLearnList()) {
					userIdSet.add(itemLearn.getUserId());
					learnUserIdSet.add(itemLearn.getUserId());
				}
				AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(
						this.adminUserService.getUserById(
								head, AdminUserProtos.GetUserByIdRequest.newBuilder()
								.addAllUserId(userIdSet)
								.build()));
				
				Map<Long, UserProtos.User> refUserMap = new TreeMap<Long, UserProtos.User>();
				for (UserProtos.User user : userResponse.getUserList()) {
					refUserMap.put(user.getBase().getUserId(), user);
				}
				Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
				for (UserProtos.Team team : userResponse.getRefTeamList()) {
					refTeamMap.put(team.getTeamId(), team);
				}
				Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
				for (UserProtos.Position position : userResponse.getRefPositionList()) {
					refPositionMap.put(position.getPositionId(), position);
				}
				
				//写出到excel文件
				for (DiscoverV2Protos.ItemLearn itemLearn : response.getItemLearnList()) {
					Row row = learnSheet.createRow(learnRowIdx++);
					row.createCell(0).setCellValue(itemLearn.getItemId());
					row.createCell(1).setCellValue(itemLearn.getUserId());
					
					UserProtos.User user = refUserMap.get(itemLearn.getUserId());
					if (user != null) {
						row.createCell(2).setCellValue(user.getBase().getUserName());
						
						if (user.getTeamCount() > 0) {
							UserProtos.UserTeam userTeam = user.getTeam(0);
							
							LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
							int teamId = userTeam.getTeamId();
							while (true) {
								UserProtos.Team team = refTeamMap.get(teamId);
								if (team == null) {
									// warn : cannot find team
									teamList.clear();
									break;
								}
								
								teamList.addFirst(team);
								
								if (team.hasParentTeamId()) {
									teamId = team.getParentTeamId();
								} else {
									break;
								}
							}
							
							Iterator<UserProtos.Team> teamIt = teamList.iterator();
							for (int i=0; i<5 && teamIt.hasNext(); ++i) {
								row.createCell(3 + i).setCellValue(teamIt.next().getTeamName());
							}
							
							if (userTeam.hasPositionId()) {
								UserProtos.Position position = refPositionMap.get(userTeam.getPositionId());
								if (position != null) {
									row.createCell(8).setCellValue(position.getPositionName());
								}
							}
						}
					} else {
						row.createCell(2).setCellValue("已删除用户:" + itemLearn.getUserId());
					}
					
					row.createCell(9).setCellValue(df.format(new Date(itemLearn.getLearnTime() * 1000L)));
					row.createCell(10).setCellValue(itemLearn.getLearnDuration());
					row.createCell(11).setCellValue(itemLearn.getLearnCnt());
					
					for (int i = 0; i <= 11; i++) {
						Cell cell = row.getCell(i);
						if (cell == null) {
							cell = row.createCell(i);
							cell.setCellValue("");
						}
						cell.setCellStyle(cellStyle);
					}
				}
				
				learnStart += learnLength;
				if (learnStart >= response.getFilteredSize()) {
					break;
				}
			}
			
			final Sheet notLearnSheet = wb.getSheetAt(1);
			int notLearnRowIdx = 2;
			int notLearnStart = 0;
			final int notLearnLength = 500;
			while (true) {
				GetUserListResponse response = Futures.getUnchecked(
						adminUserService.getUserList(head, 
								GetUserListRequest.newBuilder()
									.setStart(notLearnStart)
									.setLength(notLearnLength)
									.build()));

				Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
				for (UserProtos.Team team : response.getRefTeamList()) {
					refTeamMap.put(team.getTeamId(), team);
				}
				Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
				for (UserProtos.Position position : response.getRefPositionList()) {
					refPositionMap.put(position.getPositionId(), position);
				}
				
				//写出到excel文件
				for (UserProtos.User user : response.getUserList()) {
					if (learnUserIdSet.contains(user.getBase().getUserId())) {
						continue;
					}
					
					Row row = notLearnSheet.createRow(notLearnRowIdx++);
					row.createCell(0).setCellValue(itemId);
					row.createCell(1).setCellValue(user.getBase().getUserId());
					row.createCell(2).setCellValue(user.getBase().getUserName());
					
					if (user.getTeamCount() > 0) {
						UserProtos.UserTeam userTeam = user.getTeam(0);
						
						LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
						int teamId = userTeam.getTeamId();
						while (true) {
							UserProtos.Team team = refTeamMap.get(teamId);
							if (team == null) {
								// warn : cannot find team
								teamList.clear();
								break;
							}
							
							teamList.addFirst(team);
							
							if (team.hasParentTeamId()) {
								teamId = team.getParentTeamId();
							} else {
								break;
							}
						}
						
						Iterator<UserProtos.Team> teamIt = teamList.iterator();
						for (int i=0; i<5 && teamIt.hasNext(); ++i) {
							row.createCell(3 + i).setCellValue(teamIt.next().getTeamName());
						}
						
						if (userTeam.hasPositionId()) {
							UserProtos.Position position = refPositionMap.get(userTeam.getPositionId());
							if (position != null) {
								row.createCell(8).setCellValue(position.getPositionName());
							}
						}
					}
					
					row.createCell(9).setCellValue("");
					row.createCell(10).setCellValue("");
					row.createCell(11).setCellValue("");
					
					for (int i = 0; i <= 11; i++) {
						Cell cell = row.getCell(i);
						if (cell == null) {
							cell = row.createCell(i);
							cell.setCellValue("");
						}
						cell.setCellStyle(cellStyle);
					}
				}
				
				notLearnStart += notLearnLength;
				if (notLearnStart >= response.getFilteredSize()) {
					break;
				}
			}
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_discover_item_learn.xlsx");
			wb.write(httpResponse.getOutputStream());
			wb.dispose();
		} finally {
			wb.close();
		}
	}
}
