
-- 场景测试数据准备

INSERT INTO weizhu_scene_home (company_id, scene_id_order_str) VALUES (0, '3,2,1');

INSERT INTO weizhu_scene_scene (company_id, scene_id, scene_name, image_name, scene_desc, parent_scene_id, is_leaf_scene, state, create_admin_id, create_time, update_admin_id, update_time ) VALUES
(0, 1, '场景1', '', '场景1描述', NULL, 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 2, '场景1.1', '', '场景1.1描述', 1, 1, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 3, '场景2', '', '场景2描述', NULL, 1, 'NORMAL', 1, 1432626472,1, 1432626472);

INSERT INTO weizhu_scene_item_index (company_id, item_id, scene_id, discover_item_id, community_item_id, state, create_admin_id, create_time, update_admin_id, update_time) VALUES
(0, 1, 2, 1, null, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 2, 2, 2, null, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 3, 2, 3, null, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 4, 2, 4, null, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 5, 2, 5, null, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 6, 2, null, 100, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 7, 2, null, 101, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 8, 2, null, 102, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 9, 2, null, 103, 'NORMAL', 1, 1432626471, 1, 1432626471),
(0, 10, 2, null, 104, 'NORMAL', 1, 1432626471, 1, 1432626471);


-- 工具中的盖帽神器（超值推荐）测试数据准备

INSERT INTO weizhu_tool_recommender_category (company_id, category_id, category_name, image_name, category_desc, is_leaf_category, parent_category_id, state, create_admin_id, create_time, update_admin_id, update_time ) VALUES
(0, 1, '分类1', '', '分类1描述', 0, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 2, '分类1.1', '', '分类1.1描述', 0, 1, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 3, '分类1.1.1', '', '分类1.1.1描述', 1, 2, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 4, '分类2', '', '分类2描述', 1, NULL, 'NORMAL', 1, 1432626472,1, 1432626472);

INSERT INTO weizhu_tool_recommender_competitor_product (company_id, competitor_product_id, competitor_product_name, image_name, category_id, allow_model_id, state, create_admin_id, create_time, update_admin_id, update_time ) VALUES
(0, 1, '竞品1', '', 3, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 2, '竞品2', '', 3, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 3, '竞品3', '', 3, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 4, '竞品4', '', 3, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 5, '竞品5', '', 3, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 6, '竞品6', '', 4, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 7, '竞品7', '', 4, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 8, '竞品8', '', 4, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 9, '竞品9', '', 4, NULL, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 10, '竞品10', '', 4, NULL, 'NORMAL', 1, 1432626472,1, 1432626472);

INSERT INTO weizhu_tool_recommender_recommend_product (company_id, recommend_product_id, recommend_product_name, recommend_product_desc, image_name, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, state, create_admin_id, create_time, update_admin_id, update_time ) VALUES
(0, 1, '推荐产品1', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 2, '推荐产品2', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 3, '推荐产品3', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 4, '推荐产品4', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 5, '推荐产品5', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 6, '推荐产品6', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 7, '推荐产品7', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 8, '推荐产品8', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 9, '推荐产品9', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472),
(0, 10, '推荐产品10', '', '', NULL, 'http://www.baidu.com', 0, 'NORMAL', 1, 1432626472,1, 1432626472);

INSERT INTO weizhu_tool_recommender_competitor_recommend_product (company_id, competitor_product_id, recommend_product_id, create_admin_id, create_time) VALUES
(0, 1, 1, 1, 1432626472),
(0, 1, 2, 1, 1432626472),
(0, 2, 3, 1, 1432626472),
(0, 2, 4, 1, 1432626472),
(0, 3, 5, 1, 1432626472),
(0, 3, 6, 1, 1432626472),
(0, 4, 7, 1, 1432626472),
(0, 4, 8, 1, 1432626472),
(0, 5, 9, 1, 1432626472),
(0, 5, 10, 1, 1432626472);

INSERT INTO weizhu_tool_recommender_price_url (company_id, url_id, recommend_product_id, url_name, url_content, image_name, is_weizhu, create_admin_id, create_time) VALUES
(0, 1, 1, '京东价格1', 'http://www.jd.com/', '', 0, 1, 1432626472),
(0, 2, 1, '京东价格2', 'http://www.jd.com/', '', 0, 1, 1432626472),
(0, 3, 2, '京东价格3', 'http://www.jd.com/', '', 0, 1, 1432626472),
(0, 4, 2, '京东价格4', 'http://www.jd.com/', '', 0, 1, 1432626472);


