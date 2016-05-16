package com.weizhu.webapp.admin.api.community;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.CommunityProtos.Board;
import com.weizhu.proto.UserProtos;

public class CommunityServletUtil {
	public static final String ANONYMOUS_USER = "匿名用户";

	public static String getUserName(Map<Long, UserProtos.User> userMap, long userId) {
		String userName = "";
		UserProtos.User user = userMap.get(userId);

		if (user == null) {
			userName = ANONYMOUS_USER + ":" + userId;
		} else {
			userName = user.getBase().getUserName();
		}

		return userName;
	}

	public static Map<Long, UserProtos.User> getUserMap(List<UserProtos.User> users) {
		Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
		for (UserProtos.User user : users) {
			userMap.put(user.getBase().getUserId(), user);
		}
		return userMap;
	}

	public static String getBoardName(Map<Integer, CommunityProtos.Board> boardMap, int boardId) {
		LinkedList<String> list = Lists.newLinkedList();
		CommunityProtos.Board board = boardMap.get(boardId);
		if (board != null) {
			list.addFirst(board.getBoardName());
		}
		while (board != null && board.hasParentBoardId()) {
			if (board.getBoardId() == board.getParentBoardId()) {
				break;
			}
			board = boardMap.get(board.getParentBoardId());
			list.addFirst(board == null ? "" : board.getBoardName());
		}
		
		StringBuilder boardName = new StringBuilder();
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			boardName.append(it.next());
			if (it.hasNext()) {
				boardName.append("\\");
			}
		}
		
		return boardName.toString();
	}

	public static String getDate(int date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateFormate = sdf.format(new Date(date * 1000L));
		return dateFormate;
	}

	public static Map<Integer, Board> getBoardMap(List<CommunityProtos.Board> refBoardList) {
		//获取帖子列表对应的版块信息
		Map<Integer, CommunityProtos.Board> boardMap = new HashMap<Integer, CommunityProtos.Board>();
		for (CommunityProtos.Board board : refBoardList) {
			boardMap.put(board.getBoardId(), board);
		}
		return boardMap;
	}
}
