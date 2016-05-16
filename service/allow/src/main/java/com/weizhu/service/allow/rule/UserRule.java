package com.weizhu.service.allow.rule;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.UserProtos.User;

public class UserRule extends AbstractRule {

	private final Set<Long> userIdSet;
	
	public UserRule(AllowProtos.UserRule userRule) {
		if (userRule.getUserIdCount() <= 0) {
			userIdSet = Collections.emptySet();
		} else {
			userIdSet = new TreeSet<Long>(userRule.getUserIdList());
		}
	}
	 
	@Override
	public boolean match(User user, Map<Integer, Team> refTeamMap, Map<Integer, Position> refPositionMap) {
		return userIdSet.contains(user.getBase().getUserId());
	}

	public static String checkRule(AllowProtos.UserRule userRule) {
		if (userRule.getUserIdCount() <= 0) {
			return "用户规则内容数量为零";
		} else if (userRule.getUserIdCount() > 100) {
			return "用户规则内容数量超过最大值100";
		} else {
			return null;
		}
	}

}
