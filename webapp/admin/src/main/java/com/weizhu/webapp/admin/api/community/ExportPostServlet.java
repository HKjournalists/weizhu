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
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminCommunityProtos;
import com.weizhu.proto.AdminCommunityService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListRequest;
import com.weizhu.proto.AdminCommunityProtos.GetBoardListResponse;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExportPostServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminCommunityService adminCommunityService;
	private final AdminUserService adminUserService;
	private final UploadService uploadService;
	
	@Inject
	public ExportPostServlet(
			Provider<AdminHead> adminHeadProvider, 
			AdminCommunityService adminCommunityService, 
			AdminUserService adminUserService,
			UploadService uploadService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminCommunityService = adminCommunityService;
		this.adminUserService = adminUserService;
		this.uploadService = uploadService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = adminHeadProvider.get();

		final XSSFWorkbook wb = new XSSFWorkbook(Resources.getResource("com/weizhu/webapp/admin/api/community/export_community_post_file.xlsx")
				.openStream());

		Integer lastPostId = null;
		int size = 1000;
		boolean hasMore = false;
		Integer boardId = ParamUtil.getInt(httpRequest, "board_id", null);
		String postTitle = ParamUtil.getString(httpRequest, "post_title", null);

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
			
			String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();

			do {
				AdminCommunityProtos.ExportPostListRequest.Builder requestBuilder = AdminCommunityProtos.ExportPostListRequest.newBuilder();
				if (lastPostId != null) {
					requestBuilder.setLastPostId(lastPostId);
				}
				if (boardId != null) {
					requestBuilder.setBoardId(boardId);
				}
				if (postTitle != null) {
					requestBuilder.setPostTitle(postTitle);
				}
				requestBuilder.setSize(size);
				AdminCommunityProtos.ExportPostListResponse response = Futures.getUnchecked(adminCommunityService.exportPostList(head,
						requestBuilder.build()));
				if (response.getPostCount() <= 0) {
					// error
					break;
				}

				lastPostId = response.getPost(response.getPostCount() - 1).getPostId();
				hasMore = response.getHasMore();
				//获取问题列表对应的用户列表信息
				List<CommunityProtos.Post> posts = response.getPostList();
				Set<Long> userIds = new TreeSet<Long>();
				for (CommunityProtos.Post post : posts) {
					userIds.add(post.getCreateUserId());
				}
				AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserById(head,
						GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
				Map<Long, UserProtos.User> userMap = CommunityServletUtil.getUserMap(userResponse.getUserList());
				Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
				for (int i=0; i<userResponse.getRefTeamCount(); ++i) {
					UserProtos.Team team = userResponse.getRefTeam(i);
					teamMap.put(team.getTeamId(), team);
				}
				Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
				for (int i=0; i<userResponse.getRefPositionCount(); ++i) {
					UserProtos.Position position = userResponse.getRefPosition(i);
					positionMap.put(position.getPositionId(), position);
				}
				
				GetBoardListResponse getBoardListResponse = Futures.getUnchecked(adminCommunityService.getBoardList(head, GetBoardListRequest.newBuilder()
						.build()));
				Map<Integer, CommunityProtos.Board> boardMap = CommunityServletUtil.getBoardMap(getBoardListResponse.getBoardList());

				//写出到excel文件
				for (int i = 0; i < response.getPostCount(); ++i) {
					final CommunityProtos.Post post = posts.get(i);
					long userId = post.getCreateUserId();
					Row row = sheet.createRow(idx++);
					row.createCell(0).setCellValue(post.getPostId());
					row.createCell(1).setCellValue(post.getPostTitle());
					
					StringBuilder postContent = new StringBuilder();
					boolean isFirst = true;
					for(CommunityProtos.Post.Part postPart : post.getPostPartList()){
						if(isFirst){
							isFirst = false;
						}else{
							postContent.append("\r");
						}
						
						if(!postPart.getImageName().isEmpty()){
							postContent.append(imageUrlPrefix + postPart.getImageName());
							postContent.append("\r");
						}
						postContent.append(postPart.getText());
					}
					row.createCell(2).setCellValue(postContent.toString());
					row.createCell(3).setCellValue(CommunityServletUtil.getBoardName(boardMap, post.getBoardId()));
					row.createCell(4).setCellValue(CommunityServletUtil.getUserName(userMap, userId));
					
					UserProtos.User user = userMap.get(post.getCreateUserId());
					if(user!=null){
						
						int j=0;

						if(user.getTeamCount()>0){
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
								row.createCell(5 + j).setCellValue(team.getTeamName());
								j++;
							}
							
							if (userTeam.hasPositionId()) {
								
								UserProtos.Position position = positionMap.get(userTeam.getPositionId());
								if (position != null) {
									row.createCell(10).setCellValue(position.getPositionName());
								}else{
									row.createCell(10).setCellValue("");
								}
							}else{
								row.createCell(10).setCellValue("");
							}
						}
						
						for (; j<5; ++j) {
							row.createCell(5 + j).setCellValue("");
						}
					}else{
						for (int k=5;k<=10;k++) {
							row.createCell(k).setCellValue("");
						}
					}
					
					row.createCell(11).setCellValue(post.getIsHot() ? "是" : "否");
					row.createCell(12).setCellValue(post.getCommentCount());
					row.createCell(13).setCellValue(post.getLikeCount());
					row.createCell(14).setCellValue(CommunityServletUtil.getDate(post.getCreateTime()));
					
					for (int j = 0; j <= 14; j++) {
						Cell cell = row.getCell(j);
						if (cell == null) {
							cell = row.createCell(j);
							cell.setCellValue("");
						}
						cell.setCellStyle(cellStyle);
					}
				}
			} while (hasMore);
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_community_post.xlsx");

			wb.write(httpResponse.getOutputStream());
		} finally {
			wb.close();
		}
	}

}
