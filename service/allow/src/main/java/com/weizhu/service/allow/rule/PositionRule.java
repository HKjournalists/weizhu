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

public class PositionRule extends AbstractRule {

	public final Set<Integer> positionIdSet;
	
	public PositionRule(AllowProtos.PositionRule positionRule) {
		if (positionRule.getPositionIdCount() < 0) {
			positionIdSet = Collections.emptySet();
		} else {
			positionIdSet = new TreeSet<Integer>(positionRule.getPositionIdList());
		}
	}
	
	@Override
	public boolean match(User user, Map<Integer, Team> refTeamMap, Map<Integer, Position> refPositionMap) {
		for (UserProtos.UserTeam userTeam : user.getTeamList()) {
			if (userTeam.hasPositionId()) {
				int positionId = userTeam.getPositionId();
				if (refPositionMap.containsKey(positionId) && positionIdSet.contains(positionId)) {
					return true;
				}
			}
		}
 		return false;
	}

	public static String checkRule(AllowProtos.PositionRule positionRule) {
		if (positionRule.getPositionIdCount() <= 0) {
			return "职位规则内容数量为零";
		} else if (positionRule.getPositionIdCount() > 100) {
			return "职位结构规则内容数量超过最大值100";
		} else {
			return null;
		}
	}
}
