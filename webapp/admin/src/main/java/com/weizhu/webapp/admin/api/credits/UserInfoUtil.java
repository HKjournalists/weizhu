package com.weizhu.webapp.admin.api.credits;

import java.util.LinkedList;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.weizhu.proto.UserProtos;

public class UserInfoUtil {

	public static void getUserTeamPosition(JsonObject obj, UserProtos.User user, Map<Integer, UserProtos.Team> teamMap, Map<Integer, UserProtos.Position> positionMap) {
		if (user == null) {
			obj.addProperty("user_team", "");
			obj.addProperty("user_position", "");
		} else if (user.getTeamCount() > 0) {
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
}
