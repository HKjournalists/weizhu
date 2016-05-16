package com.weizhu.service.allow.rule;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.UserProtos.User;

public class TeamRule extends AbstractRule {

	private final Set<Integer> teamIdSet;
	
	public TeamRule(AllowProtos.TeamRule teamRule) {
		if (teamRule.getTeamIdCount() < 0) {
			teamIdSet = Collections.emptySet();
		} else {
			teamIdSet = new TreeSet<Integer>(teamRule.getTeamIdList());
		}
	}
	
	@Override
	public boolean match(User user, Map<Integer, Team> refTeamMap, Map<Integer, Position> refPositionMap) {
		for (UserProtos.UserTeam userTeam : user.getTeamList()) {
			int teamId = userTeam.getTeamId();
			while (true) {
				UserProtos.Team team = refTeamMap.get(teamId);
				if (team == null) {
					// warnning
					break;
				}
				
				if (teamIdSet.contains(teamId)) {
					return true;
				}
				
				if (team.hasParentTeamId()) {
					teamId = team.getParentTeamId();
				} else {
					break;
				}
			}
		}
 		return false;
	}

	public static String checkRule(AllowProtos.TeamRule teamRule) {
		if (teamRule.getTeamIdCount() <= 0) {
			return "组织结构规则内容数量是零";
		} else if (teamRule.getTeamIdCount() > 100) {
			return "组织结构规则内容数量超过最大值100";
		} else {
			return null;
		}
	}

}
