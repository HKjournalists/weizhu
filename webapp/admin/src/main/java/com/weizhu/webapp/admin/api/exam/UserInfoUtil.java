package com.weizhu.webapp.admin.api.exam;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.UserProtos;

public class UserInfoUtil {

	public static void getUserTeamPosition(JsonObject obj, UserProtos.User user, Map<Integer, UserProtos.Team> teamMap, Map<Integer, UserProtos.Position> positionMap) {
		if (user.getTeamCount() > 0) {
			UserProtos.UserTeam userTeam = user.getTeam(0);

			JsonArray teamArray = new JsonArray();

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
				JsonObject teamObj = new JsonObject();
				teamObj.addProperty("team_name", team.getTeamName());
				teamArray.add(teamObj);
			}
			obj.add("user_team", teamArray);
			if (userTeam.hasPositionId()) {
				UserProtos.Position position = positionMap.get(userTeam.getPositionId());
				obj.addProperty("user_position", position == null ? "" : position.getPositionName());
			}
		} else {
			obj.addProperty("user_team", "");
			obj.addProperty("user_position", "");
		}
	}
	
	public static void getStatisticParams(JsonObject obj, StatisticalParams statisticalParams) {
		BigDecimal totalNum = new BigDecimal(statisticalParams.hasTakeExamNum() ? statisticalParams.getTotalExamNum() : 0);
		BigDecimal takeNum = new BigDecimal(statisticalParams.hasTakeExamNum() ? statisticalParams.getTakeExamNum() : 0);
		BigDecimal passNum = new BigDecimal(statisticalParams.hasPassExamNum() ? statisticalParams.getPassExamNum() : 0);
		obj.addProperty("average", statisticalParams.hasAverageScore() ? statisticalParams.getAverageScore() : 0);
		obj.addProperty("total_num", totalNum.intValue());
		obj.addProperty("take_num", takeNum.intValue());
		obj.addProperty("pass_num", passNum.intValue());
		obj.addProperty("take_rate", (totalNum.intValue() == 0 ? 0 : takeNum.divide(totalNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%");
		obj.addProperty("pass_rate", (takeNum.intValue() == 0 ? 0 : passNum.divide(takeNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%");
	}
	
	public static void createCell(SXSSFRow row, int cellNum, String cellValue, @Nullable CellStyle style, Map<Integer, Integer> cellWidth) {
		SXSSFCell cell = row.createCell(cellNum);
		cell.setCellValue(cellValue);
		if (style != null) {
			cell.setCellStyle(style);
		}
		if (cellWidth.get(cellNum) == null || cellWidth.get(cellNum) < cellValue.getBytes().length) {
			cellWidth.put(cellNum, cellValue.getBytes().length);
		}
	}
	
	public static void adjustWidth(SXSSFSheet sheet, Map<Integer, Integer> cellWidth) {
		for (Entry<Integer, Integer> entry : cellWidth.entrySet()) {
			int cellIndex = entry.getKey();
			int width = entry.getValue();
			sheet.setColumnWidth(cellIndex, width * 300);
		}
	}
	
}
