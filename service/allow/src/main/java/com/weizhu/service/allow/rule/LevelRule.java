package com.weizhu.service.allow.rule;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.UserProtos.User;

public class LevelRule extends AbstractRule {

    public final Set<Integer> levelIdSet;
	
	public LevelRule(AllowProtos.LevelRule levelRule) {
		if (levelRule.getLevelIdCount() < 0) {
			levelIdSet = Collections.emptySet();
		} else {
			levelIdSet = new TreeSet<Integer>(levelRule.getLevelIdList());
		}
	}
	
	@Override
	public boolean match(User user, Map<Integer, Team> refTeamMap, Map<Integer, Position> refPositionMap) {
 		return levelIdSet.contains(user.getBase().getLevelId());
	}

	public static String checkRule(AllowProtos.LevelRule levelRule) {
		if (levelRule.getLevelIdCount() <= 0) {
			return "职位规则内容数量为零";
		} else if (levelRule.getLevelIdCount() > 100) {
			return "职位结构规则内容数量超过最大值100";
		} else {
			return null;
		}
	}

}
