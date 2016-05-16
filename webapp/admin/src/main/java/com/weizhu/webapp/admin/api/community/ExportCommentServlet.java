package com.weizhu.webapp.admin.api.community;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExportCommentServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;
	private final AdminUserService adminUserService;

	@Inject
	public ExportCommentServlet(Provider<AdminHead> adminHeadProvider, AdminCommunityService adminCommunityService,
			AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
		this.adminUserService = adminUserService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = adminHeadProvider.get();

		final XSSFWorkbook wb = new XSSFWorkbook(
				Resources.getResource("com/weizhu/webapp/admin/api/community/export_community_comment_file.xlsx").openStream());

		ByteString offsetIndex = null;
		int size = 1000;
		boolean hasMore = false;
		int postId = ParamUtil.getInt(httpRequest, "post_id", -1);

		try {
			Sheet sheet = wb.getSheetAt(0);

			int idx = 2;

			XSSFCellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			XSSFFont font = wb.createFont();
			font.setFontHeight(10);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);

			do {
				AdminCommunityProtos.ExportCommentListRequest.Builder requestBuilder = AdminCommunityProtos.ExportCommentListRequest.newBuilder()
						.setPostId(postId)
						.setSize(size);
				if (offsetIndex != null) {
					requestBuilder.setOffsetIndex(offsetIndex);
				}

				AdminCommunityProtos.ExportCommentListResponse response = Futures
						.getUnchecked(adminCommunityService.exportCommentList(head, requestBuilder.build()));
				if (response.getCommentCount() <= 0) {
					// error
					break;
				}

				offsetIndex = response.getOffsetIndex();
				hasMore = response.getHasMore();
				//获取评论列表对应的用户列表信息
				List<CommunityProtos.Comment> comments = response.getCommentList();
				Set<Long> userIds = new TreeSet<Long>();
				for (CommunityProtos.Comment comment : comments) {
					userIds.add(comment.getCreateUserId());
				}
				AdminUserProtos.GetUserByIdResponse userResponse = Futures
						.getUnchecked(this.adminUserService.getUserById(head, GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
				Map<Long, UserProtos.User> userMap = CommunityServletUtil.getUserMap(userResponse.getUserList());
				Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
				for (int i = 0; i < userResponse.getRefTeamCount(); ++i) {
					UserProtos.Team team = userResponse.getRefTeam(i);
					teamMap.put(team.getTeamId(), team);
				}
				Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
				for (int i = 0; i < userResponse.getRefPositionCount(); ++i) {
					UserProtos.Position position = userResponse.getRefPosition(i);
					positionMap.put(position.getPositionId(), position);
				}

				String postTitle = response.getRefPost().getPostTitle();
				//写出到excel文件
				for (int i = 0; i < response.getCommentCount(); ++i) {
					final CommunityProtos.Comment comment = comments.get(i);
					long userId = comment.getCreateUserId();
					Row row = sheet.createRow(idx++);
					row.createCell(0).setCellValue(comment.getCommentId());
					row.createCell(1).setCellValue(comment.getContent());
					row.createCell(2).setCellValue(comment.hasReplyCommentId() ? comment.getReplyCommentId() + "" : "");
					row.createCell(3).setCellValue(comment.getPostId());
					row.createCell(4).setCellValue(postTitle);
					row.createCell(5).setCellValue(CommunityServletUtil.getUserName(userMap, userId));

					UserProtos.User user = userMap.get(comment.getCreateUserId());
					if (user != null) {

						int j = 0;

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

							for (UserProtos.Team team : teamList) {
								row.createCell(6 + j).setCellValue(team.getTeamName());
								j++;
							}

							if (userTeam.hasPositionId()) {

								UserProtos.Position position = positionMap.get(userTeam.getPositionId());
								if (position != null) {
									row.createCell(11).setCellValue(position.getPositionName());
								} else {
									row.createCell(11).setCellValue("");
								}
							} else {
								row.createCell(11).setCellValue("");
							}
						}

						for (; j < 6; ++j) {
							row.createCell(6 + j).setCellValue("");
						}
					} else {
						for (int k = 6; k <= 11; k++) {
							row.createCell(k).setCellValue("");
						}
					}

					row.createCell(12).setCellValue(comment.getLikeCount());
					row.createCell(13).setCellValue(CommunityServletUtil.getDate(comment.getCreateTime()));

					for (int j = 0; j <= 13; j++) {
						Cell cell = row.getCell(j);
						if (cell == null) {
							cell = row.createCell(j);
							cell.setCellValue("");
						}
						cell.setCellStyle(cellStyle);					}
				}
			} while (hasMore);
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_community_comment.xlsx");

			wb.write(httpResponse.getOutputStream());
		} finally {
			wb.close();
		}
	}

}
