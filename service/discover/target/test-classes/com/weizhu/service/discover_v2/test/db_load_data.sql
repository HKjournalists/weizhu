INSERT INTO weizhu_discover_v2_item_base 
(company_id, item_id, item_name, item_desc, image_name, 
enable_comment, enable_score, enable_remind, enable_like, allow_model_id, 
`web_url.web_url`, `web_url.is_weizhu`, 
`document.document_url`, `document.document_type`, `document.document_size`, `document.is_download`, `document.check_md5`, `document.is_auth_url`, 
`video.video_url`, `video.video_type`, `video.video_size`, `video.video_time`,`video.is_download`, `video.check_md5`, `video.is_auth_url`, 
`audio.audio_url`,`audio.audio_type`,`audio.audio_size`,`audio.audio_time`,`audio.is_download`,`audio.check_md5`, `audio.is_auth_url`, 
`app_uri.app_uri`, 
state, create_admin_id, create_time, update_admin_id, update_time
) VALUES 
(0, NULL, '视频测试1', '需要授权', '77bf8c98051765a89473150878f92135.jpg', 
1, 1, 1, 1, NULL, 
NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL, 
'http://7xlo7u.media1.z0.glb.clouddn.com/1/discover/video/3c2ce1b2a6f079baf4121a424fd41ff9.mp4', 'mp4', 13816890, 356, 1, '3c2ce1b2a6f079baf4121a424fd41ff9', 1, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL, 
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406), 
(0, NULL, '视频测试2', '无需授权, 不能评论, https访问', '77bf8c98051765a89473150878f92135.jpg', 
0, 1, 1, 1, NULL, 
NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL, 
'https://dn-weizhu.qbox.me/test/3c2ce1b2a6f079baf4121a424fd41ff9.mp4', 'mp4', 13816890, 356, 1, '3c2ce1b2a6f079baf4121a424fd41ff9', 0, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL,
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406),

(0, NULL, '音频测试1', '需要授权, 不能评分', '77bf8c98051765a89473150878f92135.jpg', 
1, 0, 1, 1, NULL, 
NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL, 
'http://7xlo7u.media1.z0.glb.clouddn.com/1/discover/audio/f40e040002a55191524fb8c420c738ba.mp3', 'mp3', 4801989, 300, 1, 'f40e040002a55191524fb8c420c738ba', 1, 
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406),
(0, NULL, '音频测试2', '无需授权, 不提醒打分', '77bf8c98051765a89473150878f92135.jpg', 
1, 1, 0, 1, NULL, 
NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL,
NULL, NULL, NULL, NULL, NULL, NULL, NULL,
'http://dn-weizhu.qbox.me/test/f40e040002a55191524fb8c420c738ba.mp3', 'mp3', 4801989, 300, 1, 'f40e040002a55191524fb8c420c738ba', 0, 
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406),

(0, NULL, '文档测试1', '需要授权, 不能点赞', '77bf8c98051765a89473150878f92135.jpg', 
1, 1, 1, 0, NULL, 
NULL, NULL, 
'http://7xlbbd.dl1.z0.glb.clouddn.com/1/discover/document/19f411a1bfced9a23bf71e670fcc7acf.pdf', 'pdf', 17260803, 1, '19f411a1bfced9a23bf71e670fcc7acf', 1, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL, 
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406),
(0, NULL, '文档测试2', '无需授权, 啥都没有', '77bf8c98051765a89473150878f92135.jpg', 
0, 0, 0, 0, NULL, 
NULL, NULL, 
'https://dn-weizhu.qbox.me/test/19f411a1bfced9a23bf71e670fcc7acf.pdf', 'pdf', 17260803, 1, '19f411a1bfced9a23bf71e670fcc7acf', 0, 
NULL, NULL, NULL, NULL, NULL, NULL, NULL,
NULL, NULL, NULL, NULL, NULL, NULL, NULL,
NULL, 
'NORMAL', NULL, 1442390406, NULL, 1442390406);

UPDATE weizhu_discover_v2_module_category SET category_name = '冰箱' WHERE company_id = 0 AND category_id = 22 ;

INSERT INTO weizhu_discover_v2_module_category (company_id, category_id, category_name, module_id, state) VALUES 
(0, NULL, '洗衣机', 10, 'NORMAL'), (0, NULL, '冷柜', 10, 'NORMAL');

REPLACE INTO weizhu_discover_v2_module (company_id, module_id, module_name, image_name, `web_url.web_url`, `web_url.is_weizhu`, prompt_dot_timestamp, state) VALUES 
(0, 10, '问答社区', 'ec2ceb35b9a35d496c97404708c93d2d.png', 'http://112.126.80.91/mobile/qa/qa_info.html', 1, 1443001807727, 'NORMAL');



INSERT INTO weizhu_discover_v2_module (company_id, module_id, module_name, image_name, `web_url.web_url`, `web_url.is_weizhu`, state) VALUES
(0, 1, '小乐有话', 'b4cfa8a2eb54740c5b497c6bd6be5175.png', NULL, NULL, 'NORMAL'),
(0, 2, '我的课堂', '7fd3e4154e27e25951bba172cec98038.png', NULL, NULL, 'NORMAL'),
(0, 3, '我的考试', '56487e857344140f0631fc30f6dfd2fb.png', 'http://112.126.80.91/mobile/exam/exam_list.html', 1, 'NORMAL'),
(0, 4, '销售手册', '2a038d4e84f4e0fa559230dfc96682b3.png', NULL, NULL, 'NORMAL'),
(0, 5, '我的通知', '8baf6ca1db934abb33607320c8d4c13d.png', NULL, NULL, 'NORMAL'),
(0, 6, '我的调研', '28553dfc13d024d7cf9bcbca75dd8998.png', NULL, NULL, 'NORMAL'),
(0, 7, '产品图集', '620bccb0b789b3aaaeb6fff16951f2c9.png', NULL, NULL, 'NORMAL'),
(0, 8, '宣传图集', 'cdcbf14377c93d510fea9c8cba15ba4a.png', NULL, NULL, 'NORMAL'),
(0, 9, '官方微信', '114c30860c8e5b463f06ed4b00633c35.png', NULL, NULL, 'NORMAL');

INSERT INTO weizhu_discover_v2_module_category (company_id, category_id, category_name, module_id, state) VALUES
(0, 1, '小乐有话', 1, 'NORMAL'),

(0, 2, '乐视简介', 2, 'NORMAL'),
(0, 3, '乐视生态', 2, 'NORMAL'),
(0, 4, 'EUI', 2, 'NORMAL'),
(0, 5, '乐1', 2, 'NORMAL'),
(0, 6, '乐1Pro', 2, 'NORMAL'),
(0, 7, '乐Max', 2, 'NORMAL'),

(0, 8, '乐1', 4, 'NORMAL'),
(0, 9, '乐1Pro', 4, 'NORMAL'),
(0, 10, '乐Max', 4, 'NORMAL'),

(0, 11, '我的通知', 5, 'NORMAL'),
(0, 12, '我的调研', 6, 'NORMAL'),

(0, 13, '乐1', 7, 'NORMAL'),
(0, 14, '乐1Pro', 7, 'NORMAL'),
(0, 15, '乐Max', 7, 'NORMAL'),

(0, 16, '乐1', 8, 'NORMAL'),
(0, 17, '乐1Pro', 8, 'NORMAL'),
(0, 18, '乐Max', 8, 'NORMAL'),

(0, 19, '乐1', 9, 'NORMAL'),
(0, 20, '乐1Pro', 9, 'NORMAL'),
(0, 21, '乐Max', 9, 'NORMAL'),
(0, 22, '手机对比', 9, 'NORMAL'),
(0, 23, '小乐讲堂', 9, 'NORMAL'),
(0, 24, '乐视商城', 9, 'NORMAL'),
(0, 25, '乐迷福利', 9, 'NORMAL');

INSERT INTO weizhu_discover_v2_module_category (company_id, category_id, category_name, module_id, state) VALUES
(0, 26, '产品图集', 7, 'NORMAL'),
(0, 27, '宣传视频', 8, 'NORMAL');

UPDATE weizhu_discover_v2_module SET `web_url.web_url` = 'http://112.126.80.91/mobile/exam/exam_list.html', `web_url.is_weizhu` = 1 WHERE company_id = 0 AND module_id = 2;
DELETE weizhu_discover_v2_module_category WHERE company_id = 0 AND category_id = 7;




INSERT INTO weizhu_discover_v2_module (company_id, module_id, module_name, image_name, `web_url.web_url`, `web_url.is_weizhu`, state) VALUES
(0, 1, '新产品', 'ac677c11762767fbf5b5292d6be8c5fd.png', NULL, NULL, 'NORMAL'),
(0, 2, '销售秘籍', 'e44bde92f7257948569ddf22f1db8413.png', NULL, NULL, 'NORMAL'),
(0, 3, '终端执行', '53db89c6b86ee7f46f8d3f7485d300b8.png', NULL, NULL, 'NORMAL'),
(0, 4, '陈列展示', 'a26ebf427af86dda00370797a0822ac0.png', NULL, NULL, 'NORMAL'),
(0, 5, '促销信息', '1ec2fcb2ccf1ad8a32bfb967d7bae3ae.png', NULL, NULL, 'NORMAL'),
(0, 6, '知识大全', 'fdb8da313c30241d0b89d17675bef8b7.png', NULL, NULL, 'NORMAL'),
(0, 7, '企业动态', '0432003ba7de330b4d4eba7a0686c5f9.png', NULL, NULL, 'NORMAL'),
(0, 8, '在线考试', 'edf605e986e6cb04f226ccc0fe59ad9d.png', 'http://112.126.80.91/mobile/exam/exam_list.html', 1, 'NORMAL'),
(0, 9, '微型调研', 'c69532b8023613d399727dd9921e78e0.png', NULL, NULL, 'NORMAL');

INSERT INTO weizhu_discover_v2_module_category (company_id, category_id, category_name, module_id, state) VALUES
(0, 1, '男士爱慕先生', 1, 'NORMAL'),
(0, 2, '男士宝迪威德', 1, 'NORMAL'),
(0, 3, '女士爱慕', 1, 'NORMAL'),
(0, 4, '女士laclover', 1, 'NORMAL'),
(0, 5, '女士慕澜', 1, 'NORMAL'),
(0, 6, '儿童', 1, 'NORMAL'),
(0, 7, '快时尚爱美丽', 1, 'NORMAL'),
(0, 8, '销售宝典', 2, 'NORMAL'),
(0, 9, '产品故事', 2, 'NORMAL'),
(0, 10, '销售案例', 2, 'NORMAL'),
(0, 11, '终端执行', 3, 'NORMAL'),
(0, 12, '陈列展示', 4, 'NORMAL'),
(0, 13, '促销信息', 5, 'NORMAL'),
(0, 14, '知识大全', 6, 'NORMAL'),
(0, 15, '企业动态', 7, 'NORMAL'),
(0, 16, '微型调研', 9, 'NORMAL');


