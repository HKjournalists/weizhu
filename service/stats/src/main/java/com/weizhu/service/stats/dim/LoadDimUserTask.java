package com.weizhu.service.stats.dim;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.service.stats.StatsUtil;
import com.zaxxer.hikari.HikariDataSource;

public class LoadDimUserTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(LoadDimUserTask.class);
	
	private final HikariDataSource hikariDataSource;
	private final CompanyService companyService;
	private final AdminUserService adminUserService;
	
	public LoadDimUserTask(
			HikariDataSource hikariDataSource,
			CompanyService companyService,
			AdminUserService adminUserService
			) {
		this.hikariDataSource = hikariDataSource;
		this.companyService = companyService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void run() {
		Connection dbConn = null;
		try {
			List<CompanyProtos.Company> companyList = this.companyService.getCompanyList(
					SystemHead.newBuilder().build(), ServiceUtil.EMPTY_REQUEST
					).get().getCompanyList();
			if (companyList.isEmpty()) {
				return;
			}
			
			dbConn = this.hikariDataSource.getConnection();
			
			StatsUtil.replaceCompany(dbConn, companyList);
			
			for (CompanyProtos.Company company : companyList) {
				replaceUser(dbConn, company.getCompanyId());
			}
		} catch (Throwable th) {
			logger.error("LoadDimUserTask fail!", th);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
	}
	
	private void replaceUser(Connection dbConn, long companyId) throws InterruptedException, ExecutionException, SQLException {
		final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		
		final SystemHead head = SystemHead.newBuilder().setCompanyId(companyId).build();
		int start = 0;
		final int length = 500;
		while (true) {
			GetUserListResponse getUserListResponse = this.adminUserService.getUserList(head, 
							GetUserListRequest.newBuilder()
								.setStart(start)
								.setLength(length)
								.build()).get();
			
			if (getUserListResponse.getUserCount() <= 0) {
				break;
			}
			
			final Map<Integer, UserProtos.Team> refTeamMap = new TreeMap<Integer, UserProtos.Team>();
			for (UserProtos.Team team : getUserListResponse.getRefTeamList()) {
				refTeamMap.put(team.getTeamId(), team);
			}
			
			final Map<Integer, UserProtos.Position> refPositionMap = new TreeMap<Integer, UserProtos.Position>();
			for (UserProtos.Position position : getUserListResponse.getRefPositionList()) {
				refPositionMap.put(position.getPositionId(), position);
			}
			
			final Map<Integer, UserProtos.Level> refLevelMap = new TreeMap<Integer, UserProtos.Level>();
			for (UserProtos.Level level : getUserListResponse.getRefLevelList()) {
				refLevelMap.put(level.getLevelId(), level);
			}
			
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("REPLACE INTO weizhu_stats_dim_user (user_id, user_name, state, create_time, gender, level_id, level_name, team_id_1, team_name_1, team_id_2, team_name_2, team_id_3, team_name_3, team_id_4, team_name_4, team_id_5, team_name_5, team_id_6, team_name_6, position_id, position_name) VALUES ");
			
			boolean isFirst = true;
			for (UserProtos.User user : getUserListResponse.getUserList()) {
				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuilder.append(", ");
				}
				
				String userName = user.getBase().getUserName() + "|" + DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()) + "|" + user.getBase().getRawId();
			
				sqlBuilder.append("(");
				sqlBuilder.append(StatsUtil.toStatsUserId(companyId, user.getBase().getUserId())).append(", '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(userName, 50))).append("', '");
				sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(user.getBase().getState().name(), 10))).append("', ");
			
				if (user.getBase().hasCreateTime()) {
					sqlBuilder.append(timeFormat.format(new Date(user.getBase().getCreateTime() * 1000L)));
				} else {
					sqlBuilder.append("20150101000000");
				}
				sqlBuilder.append(", ");
				
				if (user.getBase().hasGender()) {
					sqlBuilder.append("'").append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(user.getBase().getGender().name(), 10))).append("'");
				} else {
					sqlBuilder.append("NULL");
				}
				sqlBuilder.append(", ");
				
				UserProtos.Level level = user.getBase().hasLevelId() ? refLevelMap.get(user.getBase().getLevelId()) : null;
				if (level == null) {
					sqlBuilder.append("NULL, NULL");
				} else {
					sqlBuilder.append(StatsUtil.toStatsLevelId(companyId, level.getLevelId())).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(level.getLevelName(), 20))).append("'");
				}
				sqlBuilder.append(", ");
				
				LinkedList<Integer> teamIdList = new LinkedList<Integer>();
				Integer positionId = null;
				if (user.getTeamCount() > 0) {
					if (user.getTeam(0).hasPositionId()) {
						positionId = user.getTeam(0).getPositionId();
					}
					
					Integer teamId = user.getTeam(0).getTeamId();
					while (teamId != null) {
						teamIdList.addFirst(teamId);
						
						UserProtos.Team team = refTeamMap.get(teamId);
						if (team == null) {
							// error
							teamIdList.clear();
							positionId = null;
							break;
						}
						
						teamId = team.hasParentTeamId() ? team.getParentTeamId() : null;
					}
				}
				
				for (int i=0; i<6; ++i) {
					Integer teamId = i < teamIdList.size() ? teamIdList.get(i) : null;
					
					if (teamId == null) {
						sqlBuilder.append("NULL, NULL, ");
					} else {
						UserProtos.Team team = refTeamMap.get(teamId);
						sqlBuilder.append(StatsUtil.toStatsTeamId(companyId, team.getTeamId())).append(", '");
						sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(team.getTeamName(), 20))).append("', ");
					}
				}
				
				UserProtos.Position position = positionId != null ? refPositionMap.get(positionId) : null;
				if (position == null) {
					sqlBuilder.append("NULL, NULL");
				} else {
					sqlBuilder.append(StatsUtil.toStatsPositionId(companyId, position.getPositionId())).append(", '");
					sqlBuilder.append(DBUtil.SQL_STRING_ESCAPER.escape(StatsUtil.trimToSize(position.getPositionName(), 20))).append("'");
				}
				
				sqlBuilder.append(")");
			}
			
			final String sql = sqlBuilder.toString();
			Statement stmt = dbConn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				DBUtil.closeQuietly(stmt);
			}
			
			start += length;
			if (start >= getUserListResponse.getFilteredSize()) {
				break;
			}
		}
	}

}
