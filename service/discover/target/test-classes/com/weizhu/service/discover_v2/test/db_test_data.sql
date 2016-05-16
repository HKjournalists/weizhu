

INSERT IGNORE INTO weizhu_discover_v2_banner (company_id, banner_id, banner_name, image_name, item_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`,state, create_admin_id, create_time, update_admin_id, update_time)VALUES
(0, 1, '卡萨帝 BCD-455WDCCU1', 'f3af3d569b997bb11050a572a909ad72.jpg', 9, '',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 2, '卡萨帝 BCD-801WBCAU1', 'a884cf4f9d73c8e407017c407beb0c81.jpg', 11,'',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 3, '海尔 BCD-316WDCN', '39aeaa89a1c48ed86e7aca693796387f.jpg', 1,'',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 4, '海尔 BCD-518WDGK', 'be8cc8e1bde3ee5b1eaeaee654ed8268.jpg', 6, '',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 5, '海尔 BCD-412WDCN', '3ae163a5c7e611661210e8bf187185e4.jpg', 3, '',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 6, '海尔 BCD-432WDCN', 'd46590b71a3b40356558e8601567873c.jpg', 4, '',true,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 7, '海尔 BCD-518WDGH', 'fc2e62129d1d47af320418dff92b6feb.jpg', 5, '',true,'','NORMAL', 1, 1422869341,1,1422869341);

INSERT IGNORE INTO weizhu_discover_v2_module (company_id, module_id, module_name, image_name, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, prompt_dot_timestamp, category_order_str, state, create_admin_id, create_time, update_admin_id, update_time) VALUES
(0, 1, '热门销售教程', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 2, '经典案例', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 3, '私人定制', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 4, '阶段攻略', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 5, '岗位课程', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 6, '知识信息', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341),
(0, 7, '考试', '',null,'',true,'',null,'','NORMAL', 1, 1422869341,1,1422869341);

INSERT IGNORE INTO weizhu_discover_v2_module_category (company_id, category_id, module_id, category_name,allow_model_id,state,create_admin_id,create_time,update_admin_id,update_time) VALUES 
(0, 1, 1, '私人定制',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 2, 2, '热门推荐',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 3, 3, '阶段分类',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 4, 1, '客户案例',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 5, 2, '销售案例',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 6, 1, '最新',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 7, 2, '最热',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 8, 1, '默认列表',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 9, 1, '默认列表',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 10, 1, '默认列表',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 11, 1, '参加考试',null,'NORMAL', 1, 1422869341,1,1422869341),
(0, 12, 2, '已结束考试',null,'NORMAL', 1, 1422869341,1,1422869341);

INSERT IGNORE INTO weizhu_discover_v2_module_category_item (company_id, category_id, item_id, create_admin_id, create_time) VALUES 
(0, 1, 1, 1, 1422869341),
(0, 1, 2, 1, 1422869341),
(0, 1, 3, 1, 1422869341),
(0, 1, 4, 1, 1422869341),
(0, 1, 5, 1, 1422869341),
(0, 1, 6, 1, 1422869341),
(0, 1, 7, 1, 1422869341),
(0, 1, 8, 1, 1422869341),
(0, 1, 9, 1, 1422869341),
(0, 1, 10, 1, 1422869341),
(0, 1, 11, 1, 1422869341),
(0, 1, 12, 1, 1422869341),
(0, 2, 4, 1, 1422869341),
(0, 2, 5, 1, 1422869341),
(0, 2, 6, 1, 1422869341),
(0, 2, 7, 1, 1422869341),
(0, 2, 8, 1, 1422869341),
(0, 2, 9, 1, 1422869341),
(0, 1, 4, 1, 1422869341),
(0, 1, 5, 1, 1422869341),
(0, 1, 6, 1, 1422869341),
(0, 1, 7, 1, 1422869341),
(0, 2, 4, 1, 1422869341),
(0, 2, 5, 1, 1422869341),
(0, 2, 6, 1, 1422869341),
(0, 2, 7, 1, 1422869341),
(0, 1, 4, 1, 1422869341),
(0, 1, 5, 1, 1422869341),
(0, 2, 6, 1, 1422869341),
(0, 2, 7, 1, 1422869341),
(0, 1, 7, 1, 1422869341),
(0, 1, 7, 1, 1422869341),
(0, 1, 7, 1, 1422869341);



INSERT IGNORE INTO weizhu_discover_v2_item_base (company_id, item_id, item_name, item_desc, image_name, enable_comment, enable_score, enable_remind, enable_like, enable_share, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, `document.document_url`, `document.document_type`, `document.document_size`, `document.is_download`, `document.check_md5`, `video.video_url`, `video.video_type`, `video.video_size`, `video.video_time`,`video.is_download`, `video.check_md5`,`audio.audio_url`,`audio.audio_type`,`audio.audio_size`,`audio.audio_time`,`audio.is_download`,`audio.check_md5`, `app_uri.app_uri`,state,create_admin_id,create_time, update_admin_id,update_time) VALUES 
(0, 1, '海尔 BCD-316WDCN','', '77bf8c98051765a89473150878f92135.jpg', 1,1,1,1,1,null,'',false,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 2, '海尔 BCD-402WDBA', '多门智控感湿 天然保鲜首选','bbf9f78d3cc369c68ebf2da191eb57fd.jpg', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 3, '海尔 BCD-412WDCN 技术', '智能双变温  随鲜而动','220de2f43b3bcb9a15cf205a70580508.jpg', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 4, '海尔 BCD-432WDCN', '引领多门潮流 静享极致空间  ','952818472ca9b20c8370b047ffb2fb5c.jpg',1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 5, '海尔 BCD-518WDGH','精致超薄 彩晶艺术', 'ac4b1c8554ad6e02388e529087bcc1c4.jpg', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 6, '海尔 BCD-518WDGK', '精致超薄 彩晶艺术','7e95435749fa9dca3f6e6f9175cb6349.jpg',1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 7, '海尔 BCD-800WBOU1', '极智私享 鲜而易见','e1d7397449fac60300bcfe58d9e3a602.png', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 8, '卡萨帝 BCD-408WDCAU1', '净格局 享天然','150b649ee4cbcbc74ef3f57187df2e85.png', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 9, '卡萨帝 BCD-455WDCAU1','新鲜、健康、养生都护卫', '02e0089467aa05979e936432485c014c.jpg', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 10, '卡萨帝 BCD-728WDCA', '新鲜智慧 大有不同','ba663ad519268886c5913be10dd68dbf.jpg', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 11, '卡萨帝 BCD-801WBCAU1', '卡萨帝翊动朗度 双系统叠门冰箱','72447b4b4452fe07bbf862818bef7b7d.png', 1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341), 
(0, 12, '冷柜 SC332', '竞品产品参数对比','707f6a8986d979ff909e15b74f9e365c.jpg',1,1,1,1,1,null,'',true,'','',0,1,'','','',0,0,1,'','','',0,0,1,'','','NORMAL',1,1422869341,1,1422869341);

INSERT IGNORE INTO weizhu_discover_v2_item_comment(company_id, comment_id, item_id, user_id, comment_time, comment_text, is_delete) VALUES 
(0, 1, 1, 10000124196, 1422869341, 'COMMENT_TEST1', false),
(0, 2, 1, 10000124196, 1422869341, 'COMMENT_TEST2', false),
(0, 3, 1, 10000124196, 1422869341, 'COMMENT_TEST3', false),
(0, 4, 1, 10000124196, 1422869341, 'COMMENT_TEST4', false);

INSERT IGNORE INTO weizhu_discover_v2_item_score(company_id, item_id, user_id, score_time, score_number) VALUES 
(0, 1, 10000124196, 1422869341, 50),
(0, 2, 10000124196, 1422869342, 60),
(0, 3, 10000124196, 1422869343, 70),
(0, 4, 10000124196, 1422869344, 80);

INSERT IGNORE INTO weizhu_discover_v2_item_like(company_id, item_id, user_id, like_time) VALUES 
(0, 1, 10000124196, 1422869341),
(0, 2, 10000124196, 1422869342),
(0, 3, 10000124196, 1422869343),
(0, 4, 10000124196, 1422869344);

INSERT IGNORE INTO weizhu_discover_v2_item_share(company_id, item_id, user_id, share_time) VALUES 
(0, 1, 10000124196, 1422869341),
(0, 2, 10000124196, 1422869342),
(0, 3, 10000124196, 1422869343),
(0, 4, 10000124196, 1422869344);
