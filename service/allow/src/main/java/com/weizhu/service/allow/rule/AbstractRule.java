package com.weizhu.service.allow.rule;

import java.util.Map;

import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.UserProtos;

public abstract class AbstractRule {

	/**
	 * @param user
	 * @param refTeamMap
	 * @param refPositionMap
	 * @return  ture : 匹配  false : 失配
	 */
	public abstract boolean match(UserProtos.User user, Map<Integer, UserProtos.Team> refTeamMap, Map<Integer, UserProtos.Position> refPositionMap);
	
	public static AbstractRule create(AllowProtos.Rule rule) {
		switch(rule.getRuleTypeCase()) {
			case USER_RULE:
				return new UserRule(rule.getUserRule());
			case TEAM_RULE:
				return new TeamRule(rule.getTeamRule());
			case POSITION_RULE:
				return new PositionRule(rule.getPositionRule());
			case LEVEL_RULE:
				return new LevelRule(rule.getLevelRule());
			case RULETYPE_NOT_SET:
				break;
			default:
				break;
		}
		return null;
	}
	
	/**
	 * 检查规则合法性
	 * @param rule
	 * @return null ：合法  ， not null 异常信息
	 */
	public static String check(AllowProtos.Rule rule) {
		switch(rule.getRuleTypeCase()) {
			case USER_RULE:
				return UserRule.checkRule(rule.getUserRule());
			case TEAM_RULE:
				return TeamRule.checkRule(rule.getTeamRule());
			case POSITION_RULE:
				return PositionRule.checkRule(rule.getPositionRule());
			case LEVEL_RULE:
				return LevelRule.checkRule(rule.getLevelRule());
			case RULETYPE_NOT_SET:
				break;
			default:
				break;
		}
		return "没有找到对应的规则";
	}

}
