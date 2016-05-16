package com.weizhu.service.scene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.weizhu.common.db.DBUtil;
import com.weizhu.common.db.ProtobufMapper;
import com.weizhu.common.utils.DataPage;
import com.weizhu.proto.SceneProtos;
import com.weizhu.proto.SceneProtos.State;

public class SceneDB {

	private static final ProtobufMapper<SceneProtos.Scene> SCENE_MAPPER = ProtobufMapper.createMapper(SceneProtos.Scene.getDefaultInstance(),
			"scene_id",
			"scene_name",
			"image_name",
			"scene_desc",
			"parent_scene_id",
			"is_leaf_scene",
			"item_id_order_str",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	private static final ProtobufMapper<SceneProtos.Item.ItemIndex> ITEM_INDEX_MAPPER = ProtobufMapper.createMapper(SceneProtos.Item.ItemIndex.getDefaultInstance(),
			"item_id",
			"scene_id",
			"discover_item_id",
			"community_item_id",
			"state",
			"create_admin_id",
			"create_time",
			"update_admin_id",
			"update_time");

	public static SceneDAOProtos.SceneHome getSceneHome(Connection conn, long companyId, @Nullable Collection<State> states) throws SQLException {

		if (states != null && states.isEmpty()) {
			return SceneDAOProtos.SceneHome.newBuilder().build();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" WHERE state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder("SELECT scene_id_order_str FROM weizhu_scene_home WHERE company_id = ").append(companyId)
				.append(" ; SELECT * FROM weizhu_scene_scene ")
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC, scene_id DESC;");
		String sceneIdOrderStr = null;
		Map<Integer, SceneProtos.Scene> sceneMap = new LinkedHashMap<Integer, SceneProtos.Scene>();
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			if (rs.next()) {
				sceneIdOrderStr = rs.getString(1);
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			st.getMoreResults();
			rs = st.getResultSet();
			SceneProtos.Scene.Builder tmpBuilder = SceneProtos.Scene.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				sceneMap.put(rs.getInt("scene_id"), SCENE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

		List<Integer> sceneIdOrderList;
		if (sceneIdOrderStr == null || sceneIdOrderStr.isEmpty()) {
			sceneIdOrderList = Collections.emptyList();
		} else {
			List<String> sceneIdOrderStrList = DBUtil.COMMA_SPLITTER.splitToList(sceneIdOrderStr);
			sceneIdOrderList = new ArrayList<Integer>(sceneIdOrderStrList.size());
			for (String sceneIdStr : sceneIdOrderStrList) {
				sceneIdOrderList.add(Integer.parseInt(sceneIdStr));
			}
		}

		List<Integer> rootSceneIdList = new ArrayList<Integer>();
		Map<Integer, List<Integer>> parentToSubSceneIdListMap = new HashMap<Integer, List<Integer>>();
		Set<Integer> usedSceneIds = new TreeSet<Integer>();

		for (int sceneId : sceneIdOrderList) {

			SceneProtos.Scene scene = sceneMap.get(sceneId);

			if (usedSceneIds.contains(sceneId) || scene == null) {
				continue;
			}

			if (scene.hasParentSceneId()) {
				List<Integer> subSceneList = parentToSubSceneIdListMap.get(scene.getParentSceneId());
				if (subSceneList == null) {
					subSceneList = new ArrayList<Integer>();
					parentToSubSceneIdListMap.put(scene.getParentSceneId(), subSceneList);
				}
				subSceneList.add(sceneId);

			} else {
				rootSceneIdList.add(sceneId);
			}
			usedSceneIds.add(sceneId);
		}

		for (Entry<Integer, SceneProtos.Scene> entry : sceneMap.entrySet()) {
			SceneProtos.Scene scene = entry.getValue();
			if (scene == null || usedSceneIds.contains(scene.getSceneId())) {
				continue;
			}

			if (scene.hasParentSceneId()) {
				List<Integer> subSceneList = parentToSubSceneIdListMap.get(scene.getParentSceneId());
				if (subSceneList == null) {
					subSceneList = new ArrayList<Integer>();
					parentToSubSceneIdListMap.put(scene.getParentSceneId(), subSceneList);
				}
				subSceneList.add(scene.getSceneId());

			} else {
				rootSceneIdList.add(scene.getSceneId());
			}
			usedSceneIds.add(scene.getSceneId());
		}

		List<SceneProtos.Scene> sceneList = new ArrayList<SceneProtos.Scene>();
		Queue<Integer> sceneIdQueue = new LinkedList<Integer>();
		sceneIdQueue.addAll(rootSceneIdList);
		while (!sceneIdQueue.isEmpty()) {

			int sceneId = sceneIdQueue.poll();

			SceneProtos.Scene scene = sceneMap.get(sceneId);
			if (scene == null) {
				continue;
			}
			sceneList.add(scene);

			List<Integer> subSceneIdList = parentToSubSceneIdListMap.get(scene.getSceneId());
			if (subSceneIdList != null) {
				sceneIdQueue.addAll(subSceneIdList);
			}
		}
		return SceneDAOProtos.SceneHome.newBuilder().addAllScene(sceneList).build();
	}

	public static Map<Integer, SceneProtos.Scene> getSceneBySceneIds(Connection conn, long companyId, Collection<Integer> sceneIds, @Nullable Collection<State> states)
			throws SQLException {

		if (sceneIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND scene_id IN(").append(DBUtil.COMMA_JOINER.join(sceneIds))
				.append(") ")
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC, scene_id DESC;");
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, SceneProtos.Scene> sceneMap = new HashMap<Integer, SceneProtos.Scene>();
			SceneProtos.Scene.Builder tmpBuilder = SceneProtos.Scene.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				sceneMap.put(rs.getInt("scene_id"), SCENE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return sceneMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	private static void appendStatesCondSql(StringBuilder sql, Collection<State> states) {
		if (states.isEmpty()) {
			throw new RuntimeException("states is empty");
		}
		sql.append("('");
		boolean isFirst = true;
		for (State state : states) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append("', '");
			}
			sql.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
		}

		sql.append("') ");
	}

	public static Map<Integer, SceneDAOProtos.SceneExt> getSceneExt(Connection conn, long companyId, Collection<Integer> sceneIds, int itemIdsize,
			@Nullable Collection<State> states) throws SQLException {

		if (sceneIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT scene_id, item_id_order_str FROM weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND scene_id IN(")
				.append(DBUtil.COMMA_JOINER.join(sceneIds))
				.append(") ")
				.append(statsCondStr)
				.append(";");
		for (int sceneId : sceneIds) {
			sql.append("SELECT * FROM weizhu_scene_item_index WHERE company_id = ")
					.append(companyId)
					.append(" AND scene_id = ")
					.append(sceneId)
					.append(statsCondStr)
					.append(" ORDER BY create_time DESC, item_id DESC ")
					.append(" LIMIT ")
					.append(itemIdsize)
					.append("; ");
		}

		Map<Integer, String> sceneIdItemIdOrderStrMap = new HashMap<Integer, String>();
		Map<Integer, Map<Integer, SceneProtos.Item.ItemIndex.Builder>> sceneIdItemIndexBuilderMap = new HashMap<Integer, Map<Integer, SceneProtos.Item.ItemIndex.Builder>>();

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			while (rs.next()) {
				int sceneId = rs.getInt("scene_id");
				String itemIdOrderStr = rs.getString("item_id_order_str");
				if (itemIdOrderStr != null) {
					sceneIdItemIdOrderStrMap.put(sceneId, itemIdOrderStr);
				}
			}

			for (int i = 0; i < sceneIds.size(); i++) {
				DBUtil.closeQuietly(rs);
				rs = null;
				st.getMoreResults();
				rs = st.getResultSet();
				while (rs.next()) {
					int sceneId = rs.getInt("scene_id");
					SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
					Map<Integer, SceneProtos.Item.ItemIndex.Builder> itemIndexMap = sceneIdItemIndexBuilderMap.get(sceneId);
					if (itemIndexMap == null) {
						itemIndexMap = new LinkedHashMap<Integer, SceneProtos.Item.ItemIndex.Builder>();
						sceneIdItemIndexBuilderMap.put(sceneId, itemIndexMap);
					}
					ITEM_INDEX_MAPPER.mapToItem(rs, itemIndexBuilder);
					itemIndexMap.put(itemIndexBuilder.getItemId(), itemIndexBuilder);
				}
			}

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

		Map<Integer, SceneDAOProtos.SceneExt> sceneExtMap = new HashMap<Integer, SceneDAOProtos.SceneExt>();
		SceneDAOProtos.SceneExt.Builder sceneExtBuilder = SceneDAOProtos.SceneExt.newBuilder();
		for (Entry<Integer, Map<Integer, SceneProtos.Item.ItemIndex.Builder>> entry : sceneIdItemIndexBuilderMap.entrySet()) {
			String itemIdOrderStr = sceneIdItemIdOrderStrMap.get(entry.getKey());
			List<Integer> itemIdOrderList = new ArrayList<Integer>();
			if (itemIdOrderStr != null) {
				List<String> tmpitemIdOrderList = DBUtil.COMMA_SPLITTER.splitToList(itemIdOrderStr);
				for (String itemIdStr : tmpitemIdOrderList) {
					itemIdOrderList.add(Integer.parseInt(itemIdStr));
				}
			}

			Map<Integer, SceneProtos.Item.ItemIndex.Builder> itemIndexBuilderMap = sceneIdItemIndexBuilderMap.get(entry.getKey());
			if (itemIndexBuilderMap == null || itemIndexBuilderMap.isEmpty()) {
				continue;
			}

			sceneExtBuilder.clear();
			int itemOrder = 0;
			// 添加字符串itemIdOrderStr中的序列
			for (int itemId : itemIdOrderList) {
				SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = itemIndexBuilderMap.remove(itemId);
				if (itemIndexBuilder == null) {
					continue;
				}
				itemIndexBuilder.setItemOrder(itemOrder++);
				sceneExtBuilder.addItemIndex(itemIndexBuilder.build());
			}
			// 添加不在序列中的ItemIndex
			for (SceneProtos.Item.ItemIndex.Builder itemIndexBuilder : itemIndexBuilderMap.values()) {
				itemIndexBuilder.setItemOrder(itemOrder++);
				sceneExtBuilder.addItemIndex(itemIndexBuilder.build());
			}
			sceneExtMap.put(entry.getKey(), sceneExtBuilder.build());
		}

		return sceneExtMap;
	}

	/**
	 * 根据lastIndex获取条目索引并使用item_id_order_str排序
	 * 
	 * @param conn
	 * @param sceneId
	 * @param size
	 * @param lastIndex
	 * @param states
	 * @return
	 * @throws SQLException
	 */
	public static List<SceneProtos.Item.ItemIndex> getItemIndexByOrderStr(Connection conn, long companyId, int sceneId, int size,
			@Nullable SceneProtos.Item.ItemIndex lastIndex, @Nullable Collection<State> states) throws SQLException {

		if (states != null && states.isEmpty()) {
			return Collections.emptyList();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT scene_id, item_id_order_str FROM weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND scene_id =").append(sceneId).append(statsCondStr).append(";");
		sql.append("SELECT * FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND scene_id = ").append(sceneId);
		if (lastIndex != null) {
			sql.append(" AND (create_time<")
					.append(lastIndex.getCreateTime())
					.append(" OR (create_time = ")
					.append(lastIndex.getCreateTime())
					.append(" AND item_id<")
					.append(lastIndex.getItemId())
					.append(")) ");
		}

		sql.append(statsCondStr).append(" ORDER BY create_time DESC, item_id DESC; ");

		String itemIdOrderStr = null;
		Map<Integer, SceneProtos.Item.ItemIndex.Builder> itemIndexBuilderMap = new LinkedHashMap<Integer, SceneProtos.Item.ItemIndex.Builder>();

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			if (rs.next()) {
				itemIdOrderStr = rs.getString("item_id_order_str");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();
			while (rs.next()) {
				SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
				ITEM_INDEX_MAPPER.mapToItem(rs, itemIndexBuilder);
				itemIndexBuilderMap.put(itemIndexBuilder.getItemId(), itemIndexBuilder);
			}

		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

		List<SceneProtos.Item.ItemIndex> itemIndexList = new ArrayList<SceneProtos.Item.ItemIndex>();
		List<Integer> itemIdOrderList = new ArrayList<Integer>();
		if (itemIdOrderStr != null) {
			List<String> tmpitemIdOrderList = DBUtil.COMMA_SPLITTER.splitToList(itemIdOrderStr);
			for (String itemIdStr : tmpitemIdOrderList) {
				itemIdOrderList.add(Integer.parseInt(itemIdStr));
			}
		}

		if (itemIndexBuilderMap == null || itemIndexBuilderMap.isEmpty()) {
			return Collections.emptyList();
		}

		int itemOrder = 0;
		// 添加字符串itemIdOrderStr中的序列
		for (int itemId : itemIdOrderList) {
			SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = itemIndexBuilderMap.remove(itemId);
			if (itemIndexBuilder == null) {
				continue;
			}
			itemIndexBuilder.setItemOrder(itemOrder++);
			itemIndexList.add(itemIndexBuilder.build());
		}
		// 添加不在序列中的ItemIndex
		for (SceneProtos.Item.ItemIndex.Builder itemIndexBuilder : itemIndexBuilderMap.values()) {
			itemIndexBuilder.setItemOrder(itemOrder++);
			itemIndexList.add(itemIndexBuilder.build());
		}
		// 全部取出数据才能正确排序，所以itemIndexList中存放的是所有数据，返回时要截取size长度的列表
		return itemIndexList.subList(0, size > itemIndexList.size() ? itemIndexList.size() : size);
	}

	public static void setSceneHome(Connection conn, long companyId, String sceneIdOrderStr) throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("REPLACE INTO weizhu_scene_home(company_id, scene_id_order_str) VALUES(?, ?); ");
			pstmt.setLong(1, companyId);
			pstmt.setString(2, sceneIdOrderStr);

			pstmt.executeUpdate();

		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static Map<Integer, Set<Integer>> getChildrenSceneId(Connection conn, long companyId, Collection<Integer> parentSceneIds, @Nullable Collection<SceneProtos.State> states)
			throws SQLException {

		if (states != null && states.isEmpty()) {
			return Collections.emptyMap();
		}

		StringBuilder stateCondition = new StringBuilder();
		if (states != null && !states.isEmpty()) {
			stateCondition.append(" AND state IN('");
			boolean isFirst = true;
			for (SceneProtos.State state : states) {

				if (isFirst) {
					isFirst = false;
				} else {
					stateCondition.append("','");
				}
				stateCondition.append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()));
			}
			stateCondition.append("') ");
		}

		StringBuilder sql = new StringBuilder("SELECT scene_id,parent_scene_id FROM  weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND parent_scene_id IN(").append(DBUtil.COMMA_JOINER.join(parentSceneIds)).append(") ")
				.append(stateCondition)
				.append(";");

		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, Set<Integer>> parentSceneIdChildrenSceneIdsMap = new HashMap<Integer, Set<Integer>>();
			while (rs.next()) {
				int parentSceneId = rs.getInt("parent_scene_id");
				Set<Integer> childrenSceneIds = parentSceneIdChildrenSceneIdsMap.get(parentSceneId);
				if(childrenSceneIds==null){
					childrenSceneIds = new TreeSet<Integer>();
					parentSceneIdChildrenSceneIdsMap.put(parentSceneId, childrenSceneIds);
				}
				childrenSceneIds.add(rs.getInt("scene_id"));
			}

			return parentSceneIdChildrenSceneIdsMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static int insertSceneScene(Connection conn, long companyId, String sceneName, String imageName, String sceneDesc, @Nullable Integer parentSceneId,
			boolean isLeafScene, long createAdminId, int createTime, SceneProtos.State state) throws SQLException {

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_scene_scene(company_id, scene_name, image_name, scene_desc, parent_scene_id, is_leaf_scene, state, create_admin_id, create_time ) VALUES(?,?,?,?,?,?,?,?,?); ",
					Statement.RETURN_GENERATED_KEYS);
			
			pstmt.setLong(1, companyId);
			pstmt.setString(2, sceneName);
			pstmt.setString(3, imageName);
			pstmt.setString(4, sceneDesc);
			if (parentSceneId == null) {
				pstmt.setNull(5, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(5, parentSceneId);
			}
			pstmt.setBoolean(6, isLeafScene);
			pstmt.setString(7, state.name());
			pstmt.setLong(8, createAdminId);
			pstmt.setInt(9, createTime);

			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();

			Integer sceneId = null;
			if (rs.next()) {
				sceneId = rs.getInt(1);
			}

			if (sceneId == null) {
				throw new RuntimeException("插入scene出错！");
			}

			return sceneId;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateItemScene(Connection conn, long companyId, List<Integer> itemIds, int sceneId) throws SQLException {

		if (itemIds.isEmpty()) {
			return;
		}

		String itemIdStr = DBUtil.COMMA_JOINER.join(itemIds);

		StringBuilder sql = new StringBuilder("UPDATE weizhu_scene_item_index SET scene_id =").append(sceneId)
				.append(" WHERE company_id = ").append(companyId)
				.append(" AND item_id IN( ")
				.append(itemIdStr)
				.append(");");
		Statement st = null;

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());
		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	public static void updateScene(Connection conn, long companyId, int sceneId, String sceneName, String imageName, String sceneDesc, long updateAdminId,
			int updateTime) throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_scene_scene SET scene_name = ?, image_name =?, scene_desc = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND scene_id = ?; ");

			pstmt.setString(1, sceneName);
			pstmt.setString(2, imageName);
			pstmt.setString(3, sceneDesc);
			pstmt.setLong(4, updateAdminId);
			pstmt.setInt(5, updateTime);
			pstmt.setLong(6, companyId);
			pstmt.setInt(7, sceneId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateSceneIsLeafScene(Connection conn, long companyId, int sceneId, boolean isLeafScene, long updateAdminId, int updateTime)
			throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_scene_scene SET is_leaf_scene = ?, update_admin_id = ?, update_time = ? WHERE company_id = ? AND scene_id = ?; ");

			pstmt.setBoolean(1, isLeafScene);
			pstmt.setLong(2, updateAdminId);
			pstmt.setInt(3, updateTime);
			pstmt.setLong(4, companyId);
			pstmt.setInt(5, sceneId);

			pstmt.executeUpdate();
		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static void updateSceneState(Connection conn, long companyId, Collection<Integer> sceneId, long updateAdminId, int updateTime, SceneProtos.State state)
			throws SQLException {

		String sceneIdStr = DBUtil.COMMA_JOINER.join(sceneId);

		StringBuilder sql = new StringBuilder("UPDATE weizhu_scene_scene SET state = '").append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()))
				.append("' , update_admin_id = ")
				.append(updateAdminId)
				.append(", update_time = ")
				.append(updateTime)
				.append("  WHERE company_id = ").append(companyId)
				.append(" AND scene_id IN(")
				.append(sceneIdStr)
				.append(") ;");
		Statement st = null;

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());

		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	public static List<Integer> insertSceneItem(Connection conn, long companyId, Collection<SceneProtos.Item.ItemIndex> itemIndexList) throws SQLException {

		if (itemIndexList.isEmpty()) {
			return Collections.emptyList();
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement("INSERT INTO weizhu_scene_item_index(company_id,discover_item_id,scene_id,create_admin_id,create_time,state) VALUES(?,?,?,?,?,?); ",
					Statement.RETURN_GENERATED_KEYS);
			for (SceneProtos.Item.ItemIndex itemIndex : itemIndexList) {
				pstmt.setLong(1, companyId);
				DBUtil.set(pstmt, 2, itemIndex.hasDiscoverItemId(), itemIndex.getDiscoverItemId());
				pstmt.setInt(3, itemIndex.getSceneId());
				pstmt.setLong(4, itemIndex.getCreateAdminId());
				pstmt.setInt(5, itemIndex.getCreateTime());
				pstmt.setString(6, itemIndex.getState().name());
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			rs = pstmt.getGeneratedKeys();
			List<Integer> itemIds = new ArrayList<Integer>();
			while (rs.next()) {
				itemIds.add(rs.getInt(1));
			}
			if (itemIds.size() != itemIndexList.size()) {
				throw new RuntimeException("创建场景条目出错！");
			}

			return itemIds;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(pstmt);
		}

	}

	public static Map<Integer, SceneProtos.Item.ItemIndex> getItemIndexById(Connection conn, long companyId, Collection<Integer> itemIds) throws SQLException {

		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String itemIdStr = DBUtil.COMMA_JOINER.join(itemIds);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND item_id IN(").append(itemIdStr).append("); ");
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, SceneProtos.Item.ItemIndex> itemIndexMap = new HashMap<Integer, SceneProtos.Item.ItemIndex>();
			SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
			while (rs.next()) {
				itemIndexBuilder.clear();
				itemIndexMap.put(rs.getInt("item_id"), ITEM_INDEX_MAPPER.mapToItem(rs, itemIndexBuilder).build());
			}

			return itemIndexMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static void updateSceneItemState(Connection conn, long companyId, Collection<Integer> itemIds, SceneProtos.State state, long updateAdminId, int updateTime)
			throws SQLException {

		if (itemIds.isEmpty()) {
			return;
		}

		String itemIdStr = DBUtil.COMMA_JOINER.join(itemIds);

		StringBuilder sql = new StringBuilder("UPDATE weizhu_scene_item_index SET state = '").append(DBUtil.SQL_STRING_ESCAPER.escape(state.name()))
				.append("', update_admin_id = ")
				.append(updateAdminId)
				.append(" , update_time =")
				.append(updateTime)
				.append(" WHERE company_id = ").append(companyId)
				.append(" AND item_id IN(")
				.append(itemIdStr)
				.append(") ;");

		Statement st = null;

		try {
			st = conn.createStatement();
			st.executeUpdate(sql.toString());

		} finally {
			DBUtil.closeQuietly(st);
		}

	}

	public static Map<Integer, List<Integer>> getSceneIdByItemId(Connection conn, long companyId, Collection<Integer> itemIds) throws SQLException {

		if (itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		String itemIdStr = DBUtil.COMMA_JOINER.join(itemIds);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT item_id,scene_id FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND item_id IN(").append(itemIdStr).append("); ");
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, List<Integer>> itemIdModuelIdListMap = new HashMap<Integer, List<Integer>>();
			while (rs.next()) {
				int itemId = rs.getInt("item_id");
				List<Integer> sceneIdList = itemIdModuelIdListMap.get(itemId);
				if (sceneIdList == null) {
					sceneIdList = new ArrayList<Integer>();
					itemIdModuelIdListMap.put(itemId, sceneIdList);
				}
				sceneIdList.add(rs.getInt("scene_id"));
			}

			return itemIdModuelIdListMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	/**
	 * 根据start，length获取条目索引列表，并根据item_id_order_str排序
	 * 
	 * @param conn
	 * @param sceneId
	 * @param start
	 * @param length
	 * @param states
	 * @return
	 * @throws SQLException
	 */
	public static DataPage<SceneProtos.Item.ItemIndex> getItemIndexByOrderStr(Connection conn, long companyId, int sceneId, @Nullable Integer start, int length,
			@Nullable Collection<SceneProtos.State> states) throws SQLException {

		if (states != null && states.isEmpty()) {
			return new DataPage<SceneProtos.Item.ItemIndex>(Collections.<SceneProtos.Item.ItemIndex> emptyList(), 0, 0);
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT scene_id, item_id_order_str FROM weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND scene_id =").append(sceneId).append(statsCondStr).append(";");
		sql.append("SELECT * FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND scene_id = ")
				.append(sceneId)
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC, item_id DESC; ");
		// 场景下条目总数
		sql.append("SELECT count(item_id) AS total_size FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND scene_id =")
				.append(sceneId)
				.append(statsCondStr)
				.append(";");
		String itemIdOrderStr = null;
		Map<Integer, SceneProtos.Item.ItemIndex.Builder> itemIndexBuilderMap = new LinkedHashMap<Integer, SceneProtos.Item.ItemIndex.Builder>();
		int totalSize = 0;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			if (rs.next()) {
				itemIdOrderStr = rs.getString("item_id_order_str");
			}

			DBUtil.closeQuietly(rs);
			rs = null;
			st.getMoreResults();
			rs = st.getResultSet();
			while (rs.next()) {
				SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
				ITEM_INDEX_MAPPER.mapToItem(rs, itemIndexBuilder);
				itemIndexBuilderMap.put(itemIndexBuilder.getItemId(), itemIndexBuilder);
			}

			DBUtil.closeQuietly(rs);
			rs = null;

			st.getMoreResults();
			rs = st.getResultSet();
			if (rs.next()) {
				totalSize = rs.getInt("total_size");
			}
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}

		List<SceneProtos.Item.ItemIndex> itemIndexList = new ArrayList<SceneProtos.Item.ItemIndex>();
		List<Integer> itemIdOrderList = new ArrayList<Integer>();
		if (itemIdOrderStr != null) {
			List<String> tmpitemIdOrderList = DBUtil.COMMA_SPLITTER.splitToList(itemIdOrderStr);
			for (String itemIdStr : tmpitemIdOrderList) {
				itemIdOrderList.add(Integer.parseInt(itemIdStr));
			}
		}

		if (itemIndexBuilderMap == null || itemIndexBuilderMap.isEmpty()) {
			return new DataPage<SceneProtos.Item.ItemIndex>(Collections.<SceneProtos.Item.ItemIndex> emptyList(), 0, 0);
		}

		int itemOrder = 0;
		// 添加字符串itemIdOrderStr中的序列
		for (int itemId : itemIdOrderList) {
			SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = itemIndexBuilderMap.remove(itemId);
			if (itemIndexBuilder == null) {
				continue;
			}
			itemIndexBuilder.setItemOrder(itemOrder++);
			itemIndexList.add(itemIndexBuilder.build());
		}
		// 添加不在序列中的ItemIndex
		for (SceneProtos.Item.ItemIndex.Builder itemIndexBuilder : itemIndexBuilderMap.values()) {
			itemIndexBuilder.setItemOrder(itemOrder++);
			itemIndexList.add(itemIndexBuilder.build());
		}
		// 全部取出数据才能正确排序，所以itemIndexList中存放的是所有数据，返回时要从start开始截取length长度的列表
		start = (start == null || start < 0) ? 0 : start;
		length = length > itemIndexList.size() - start ? itemIndexList.size() - start : length;
		return new DataPage<SceneProtos.Item.ItemIndex>(itemIndexList.subList(start, length), totalSize, totalSize);
	}

	/**
	 * 根据lastIndex获取条目索引列表，并根据create_time排序
	 * 
	 * @param conn
	 * @param sceneId
	 * @param size
	 * @param lastIndex
	 * @param states
	 * @return
	 * @throws SQLException
	 */
	public static List<SceneProtos.Item.ItemIndex> getItemIndexByCreateTime(Connection conn, long companyId, @Nullable Integer sceneId, int size,
			@Nullable SceneProtos.Item.ItemIndex lastIndex, @Nullable Collection<SceneProtos.State> states) throws SQLException {

		if (states != null && states.isEmpty()) {
			return Collections.emptyList();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM weizhu_scene_item_index WHERE company_id = ").append(companyId);
		if (sceneId != null) {
			sql.append(" AND scene_id = ").append(sceneId);
		}
		if (lastIndex != null) {
			sql.append(" AND (create_time<")
					.append(lastIndex.getCreateTime())
					.append(" OR (create_time = ")
					.append(lastIndex.getCreateTime())
					.append(" AND item_id<")
					.append(lastIndex.getItemId())
					.append(")) ");
		}

		sql.append(statsCondStr).append(" ORDER BY create_time DESC, item_id DESC; ");

		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			List<SceneProtos.Item.ItemIndex> itemIndexList = new ArrayList<SceneProtos.Item.ItemIndex>();
			SceneProtos.Item.ItemIndex.Builder itemIndexBuilder = SceneProtos.Item.ItemIndex.newBuilder();
			while (rs.next()) {
				itemIndexBuilder.clear();
				ITEM_INDEX_MAPPER.mapToItem(rs, itemIndexBuilder);
				itemIndexList.add(itemIndexBuilder.build());
			}
			return itemIndexList;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

	public static void updateSceneItemOrder(Connection conn, long companyId, int sceneId, String itemIdOrderStr) throws SQLException {

		PreparedStatement pstmt = null;

		try {
			pstmt = conn.prepareStatement("UPDATE weizhu_scene_scene SET item_id_order_str = ? WHERE company_id = ? AND scene_id = ?");
			pstmt.setLong(1, companyId);
			pstmt.setString(2, itemIdOrderStr);
			pstmt.setInt(3, sceneId);
			pstmt.executeUpdate();

		} finally {
			DBUtil.closeQuietly(pstmt);
		}
	}

	public static Map<Integer, SceneProtos.Scene> getSceneByItemIds(Connection conn, long companyId, Collection<Integer> itemIds, Collection<State> states)
			throws SQLException {
		if (itemIds.isEmpty() || (states != null && states.isEmpty())) {
			return Collections.emptyMap();
		}

		String statsCondStr;
		if (states == null) {
			statsCondStr = "";
		} else {
			StringBuilder sb = new StringBuilder(" AND state IN ");
			appendStatesCondSql(sb, states);
			statsCondStr = sb.toString();
		}

		StringBuilder sql = new StringBuilder("SELECT * FROM weizhu_scene_scene WHERE company_id = ").append(companyId)
				.append(" AND scene_id IN( SELECT DISTINCT scene_id FROM weizhu_scene_item_index WHERE company_id = ").append(companyId)
				.append(" AND item_id IN(").append(DBUtil.COMMA_JOINER.join(itemIds))
				.append(")) ")
				.append(statsCondStr)
				.append(" ORDER BY create_time DESC, scene_id DESC;");
		Statement st = null;
		ResultSet rs = null;

		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());

			Map<Integer, SceneProtos.Scene> sceneMap = new HashMap<Integer, SceneProtos.Scene>();
			SceneProtos.Scene.Builder tmpBuilder = SceneProtos.Scene.newBuilder();
			while (rs.next()) {
				tmpBuilder.clear();
				sceneMap.put(rs.getInt("scene_id"), SCENE_MAPPER.mapToItem(rs, tmpBuilder).build());
			}

			return sceneMap;
		} finally {
			DBUtil.closeQuietly(rs);
			DBUtil.closeQuietly(st);
		}
	}

}
