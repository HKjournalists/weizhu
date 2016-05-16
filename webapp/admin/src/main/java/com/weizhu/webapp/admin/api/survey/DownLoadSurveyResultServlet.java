package com.weizhu.webapp.admin.api.survey;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyByIdResponse;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListRequest;
import com.weizhu.proto.SurveyProtos.GetSurveyResultListResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class DownLoadSurveyResultServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	private final AllowService allowService;
	private final AdminUserService adminUserService;
	
	@Inject
	public DownLoadSurveyResultServlet(Provider<AdminHead> adminHeadProvider,
			SurveyService surveyService,
			AllowService allowService,
			AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
		this.allowService = allowService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
		final AdminHead adminHead = adminHeadProvider.get();

		GetSurveyByIdResponse getSurveyByIdResponse = Futures.getUnchecked(surveyService.getSurveyById(adminHead, GetSurveyByIdRequest.newBuilder()
				.setSurveyId(surveyId)
				.build()));

		final Integer allowModelId = getSurveyByIdResponse.getSurvey().getAllowModelId();
		// 获取所有满足访问模型的用户
		Map<Long, UserProtos.User> allowUserMap = this.getAllowUser(adminHead, allowModelId);
		
		// 获取所有的用户的详细信息
		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(allowUserMap.keySet())
				.build();
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(adminHead, getUserByIdRequest));

		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}

		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserByIdResponse
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}

		Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>();
		for (int i = 0; i < getUserByIdResponse.getRefLevelCount(); ++i) {
			UserProtos.Level level = getUserByIdResponse.getRefLevel(i);
			levelMap.put(level.getLevelId(), level);
		}
		
		SXSSFWorkbook wb = new SXSSFWorkbook();

		writeExecl(wb, adminHead, getSurveyByIdResponse, allowUserMap, teamMap, positionMap, levelMap);
		
		httpResponse.setContentType("text/plain");
		httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("survey_result.xlsx".getBytes("utf-8"),"iso8859-1"));
		
		wb.write(httpResponse.getOutputStream());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private Map<Long, UserProtos.User> getAllowUser(AdminHead adminHead, @Nullable Integer allowModelId) {
		GetUserListRequest.Builder getUserListRequestBuilder = GetUserListRequest.newBuilder()
				.setStart(0)
				.setLength(1000);
		
		Map<Long, UserProtos.User> allowUserMap = new HashMap<Long, UserProtos.User>();
		boolean hasMore = true;
		int start = 0;
		while (hasMore) {
			GetUserListResponse getUserListResponse = Futures.getUnchecked(adminUserService.getUserList(adminHead, getUserListRequestBuilder
					.setStart(start)
					.setLength(1000)
					.build()));
			start += 1000;

			if (getUserListResponse.getUserCount() < 1000) {
				hasMore = false;
			}
			Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserListResponse.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			
			// 如果allowModelId是空值，所有的用户都允许
			if (allowModelId == 0 || allowModelId == null) {
				allowUserMap.putAll(userMap);
			} else {
				CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
						.addAllModelId(Collections.singleton(allowModelId))
						.addAllUserId(userMap.keySet())
						.build();
				CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(adminHead, checkAllowRequest));
				for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
					if (checkResult.getModelId() == allowModelId) {
						for (long userId : checkResult.getAllowUserIdList()) {
							UserProtos.User user = userMap.get(userId);
							if (user != null) {
								allowUserMap.put(userId, user);
							}
						}
					}
				}
			}
		}
		
		return allowUserMap;
	}
	
	private SXSSFWorkbook writeExecl(SXSSFWorkbook wb, AdminHead adminHead, GetSurveyByIdResponse getSurveyByIdResponse,
			Map<Long, UserProtos.User> allowUserMap,
			Map<Integer, UserProtos.Team> teamMap, Map<Integer, UserProtos.Position> positionMap, Map<Integer, UserProtos.Level> levelMap) {
		String surveyName = getSurveyByIdResponse.getSurvey().getSurveyName();
		
		SXSSFSheet sheet = wb.createSheet("调研结果");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 13);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue(surveyName + "结果统计表");
		headCell.setCellStyle(headStyle);
		
		// 根据问题和选项数量统计表格多少列
		List<SurveyProtos.Question> questionList = getSurveyByIdResponse.getQuestionList();
		int i = 0;
		for (SurveyProtos.Question question : questionList) {
			String typeName = question.getTypeCase().name();
			if (typeName.equals("VOTE")) {
				i = i + question.getVote().getOptionCount();
			} else if (typeName.equals("INPUT_SELECT")) {
				i = i + question.getInputSelect().getOptionCount();
			} else {
				i++;
			}
		}
		
		for (int j = 0; j < i; j ++) {
			sheet.setColumnWidth(j, 10 * 256);
		}
		
		// 在表格中填问题
		SXSSFRow row1 = sheet.createRow(1); // 题目行
		SXSSFRow row2 = sheet.createRow(2); // 选项行

		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);

		try {
			Map<Integer, Integer> questionIndex = new HashMap<Integer, Integer>(); // 题目对应在execl表格中的位置
			Map<Integer, Integer> voteOptionIndex = new HashMap<Integer, Integer>(); // 投票选项对应在execl表格中的位置
			Map<Integer, Integer> inputSelectOptionIndex = new HashMap<Integer, Integer>(); // 下拉框选项对应在execl表格中的位置
			
			Map<Integer, Integer> voteOptionCount = new HashMap<Integer, Integer>();
			Map<Integer, Integer> inputSelectOptionCount = new HashMap<Integer, Integer>();
			
			Map<Integer, Integer> cellWidth = new HashMap<Integer, Integer>(); // 列宽
			
			// 添加答案作者信息
			createCell(row1, 0, "一级部门", style, cellWidth);
			createCell(row1, 1, "二级部门", style, cellWidth);
			createCell(row1, 2, "三级部门", style, cellWidth);
			createCell(row1, 3, "四级部门", style, cellWidth);
			createCell(row1, 4, "五级部门", style, cellWidth);
			createCell(row1, 5, "六级部门", style, cellWidth);
			createCell(row1, 6, "岗位", style, cellWidth);
			createCell(row1, 7, "姓名", style, cellWidth);
			createCell(row1, 8, "工号", style, cellWidth);
			createCell(row1, 9, "电话", style, cellWidth);
			
			int flag = 10;
			for (SurveyProtos.Question question : questionList) {
				String typeName = question.getTypeCase().name();
				if (typeName.equals("VOTE")) {
					createCell(row1, flag, question.getQuestionName(), style, cellWidth);
					
					questionIndex.put(question.getQuestionId(), flag);
					
					int optionLength = flag + question.getVote().getOptionCount();
					for (int j = flag; j < optionLength; j ++) {
						SurveyProtos.Vote.Option option = question.getVote().getOption(j - flag);
						createCell(row2, j, option.getOptionName(), style, cellWidth);
						
						voteOptionIndex.put(option.getOptionId(), j);
						voteOptionCount.put(j, 0);
					}
					
					cra = new CellRangeAddress(1, 1, flag, optionLength - 1);
					sheet.addMergedRegion(cra);
					
					flag = optionLength;
				} else if (typeName.equals("INPUT_SELECT")) {
					createCell(row1, flag, question.getQuestionName(), style, cellWidth);
					
					questionIndex.put(question.getQuestionId(), flag);
					
					int optionLength = flag + question.getInputSelect().getOptionCount();
					for (int j = flag; j < optionLength; j ++) {
						SurveyProtos.InputSelect.Option option = question.getInputSelect().getOption(j - flag);
						createCell(row2, j, option.getOptionName(), style, cellWidth);
						
						inputSelectOptionIndex.put(option.getOptionId(), j);
						inputSelectOptionCount.put(j, 0);
					}
					
					cra = new CellRangeAddress(1, 1, flag, optionLength - 1);
					sheet.addMergedRegion(cra);
					
					flag = optionLength;
				} else {
					createCell(row1, flag, question.getQuestionName(), style, cellWidth);
					
					questionIndex.put(question.getQuestionId(), flag);
					
					flag ++;
				}
			}
			
			Map<Long, UserProtos.User> noJoinUserMap = new HashMap<Long, UserProtos.User>(allowUserMap);
			
			boolean hasMore = true;
			int start = 0;
			int rowIndex = 3; // 题目区
			while (hasMore) {
				GetSurveyResultListRequest getSurveyResultListRequest = GetSurveyResultListRequest.newBuilder()
						.setSurveyId(getSurveyByIdResponse.getSurvey().getSurveyId())
						.setStart(start)
						.setLength(100)
						.build();
				start += 100;
				
				// 获取所有的调研结果
				GetSurveyResultListResponse getSurveyResultListResponse = Futures.getUnchecked(
						surveyService.getSurveyResultList(adminHead, getSurveyResultListRequest));
				
				for (SurveyProtos.SurveyResult result : getSurveyResultListResponse.getSurveyResultList()) {
					SXSSFRow row = sheet.createRow(rowIndex); // 题目行
					
					// 如果用户不存在，忽略此用户答题信息
					UserProtos.User user = allowUserMap.get(result.getUserId());
					if (user == null) {
						continue;
					}
					
					// 写用户信息
					this.writeUserInfo(row, result, allowUserMap, teamMap, positionMap, levelMap, cellWidth);
					
					for (SurveyProtos.Answer answer : result.getAnswerList()) {
						Integer index = null;
						if (answer.getVote().getOptionIdCount() > 0) {
							for (Integer optionId : answer.getVote().getOptionIdList()) {
								index = voteOptionIndex.get(optionId.intValue());
								if (index != null) {
									createCell(row, index, "√", style, cellWidth);
									
									voteOptionCount.put(index, voteOptionCount.get(index) + 1);
								}
							}
						} else if (answer.getInputSelect().getOptionId() > 0) {
							index = inputSelectOptionIndex.get(answer.getInputSelect().getOptionId());
							if (index != null) {
								createCell(row, index, "√", style, cellWidth);
								
								inputSelectOptionCount.put(index, inputSelectOptionCount.get(index) + 1);
							}
						} else if (!answer.getInputText().getResultText().isEmpty()) {
							index = questionIndex.get(answer.getQuestionId());
							if (index != null) {
								createCell(row, index, answer.getInputText().getResultText(), style, cellWidth);
							}
						}
					}
					rowIndex ++ ;
					noJoinUserMap.remove(result.getUserId());
				}
				
				if (getSurveyResultListResponse.getSurveyResultCount() == 0) {
					hasMore = false;
				}
			}
			
			// 写没有参加调研的用户信息
			this.writeUserInfoNoResult(sheet, rowIndex, noJoinUserMap, teamMap, positionMap, levelMap, cellWidth);
			
			// 简单的统计
			this.surveyCountResult(sheet, allowUserMap.size(), noJoinUserMap.size(), voteOptionCount, inputSelectOptionCount);
			
		} catch (Exception ex) {
			throw new RuntimeException("write execl error :", ex);
		}
		
		return wb;
	}
	
	private void createCell(SXSSFRow row, int cellNum, String cellValue, @Nullable CellStyle style, Map<Integer, Integer> cellWidth) {
		SXSSFCell cell = row.createCell(cellNum);
		cell.setCellValue(cellValue);
		if (style != null) {
			cell.setCellStyle(style);
		}
		if (cellWidth.get(cellNum) == null || cellWidth.get(cellNum) < cellValue.getBytes().length) {
			cellWidth.put(cellNum, cellValue.getBytes().length);
		}
		
	}
	
	private void writeUserInfo(SXSSFRow row, SurveyProtos.SurveyResult result, 
			Map<Long, UserProtos.User> allowUserMap,
			Map<Integer, UserProtos.Team> teamMap, 
			Map<Integer, UserProtos.Position> positionMap, 
			Map<Integer, UserProtos.Level> levelMap,
			Map<Integer, Integer> cellWidth) {
		
		long userId = result.getUserId();
		UserProtos.User user = allowUserMap.get(userId);

		userInof(row, user, teamMap, positionMap, levelMap, cellWidth);
	}
	
	private void writeUserInfoNoResult(SXSSFSheet sheet, int rowIndex, 
			Map<Long, UserProtos.User> noJoinUserMap,
			Map<Integer, UserProtos.Team> teamMap, 
			Map<Integer, UserProtos.Position> positionMap, 
			Map<Integer, UserProtos.Level> levelMap,
			Map<Integer, Integer> cellWidth) {
		for (Entry<Long, UserProtos.User> entry : noJoinUserMap.entrySet()) {
			UserProtos.User user = entry.getValue();
			
			SXSSFRow row = sheet.createRow(rowIndex ++ );
			userInof(row, user, teamMap, positionMap, levelMap, cellWidth);
		}

	}
	
	private void userInof(SXSSFRow row, 
			UserProtos.User user,
			Map<Integer, UserProtos.Team> teamMap, 
			Map<Integer, UserProtos.Position> positionMap, 
			Map<Integer, UserProtos.Level> levelMap,
			Map<Integer, Integer> cellWidth) {
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
					createCell(row, teamCell ++, team.getTeamName(), null, cellWidth);
				}

				if (userTeam.hasPositionId()) {
					UserProtos.Position position = positionMap.get(userTeam.getPositionId());
					createCell(row, 6, position == null ? "" : position.getPositionName(), null, cellWidth);
				}
			}
			
			createCell(row, 7, user.getBase().getUserName(), null, cellWidth);

			createCell(row, 8, user.getBase().getRawId(), null, cellWidth);
			
			StringBuilder mobileStr = new StringBuilder();
			createCell(row, 9, DBUtil.COMMA_JOINER.appendTo(mobileStr, user.getBase().getMobileNoList()).toString(), null, cellWidth);
		}
	}
	
	private void surveyCountResult(SXSSFSheet sheet, int totalUser, int noJoinUser, 
			Map<Integer, Integer> voteOptionCount, 
			Map<Integer, Integer> inputSelectOptionCount) {
		SXSSFRow row = sheet.createRow(totalUser + 3);
		
		SXSSFCell cell = row.createCell(0);
		cell.setCellValue("统计");
		
		int joinUser = totalUser - noJoinUser;
		
		for (Entry<Integer, Integer> entry : voteOptionCount.entrySet()) {
			cell = row.createCell(entry.getKey());
			cell.setCellValue(entry.getValue() + "/" + joinUser + "/" + totalUser);
		}
		for (Entry<Integer, Integer> entry : inputSelectOptionCount.entrySet()) {
			cell = row.createCell(entry.getKey());
			cell.setCellValue(entry.getValue() + "/" + joinUser + "/" + totalUser);
		}
	}
}
