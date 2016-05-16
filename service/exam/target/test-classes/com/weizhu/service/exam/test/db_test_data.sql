insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 123, '测试考试', '123123', '222222', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 1234, '小小测试', '1429350960', '1449850960', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 1224, '大大测试', '1429350960', '1639850960', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 1225, '大xiao大测试', '123123', '222222', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 111, 'xiao大xiao大测试', '123123', '222222', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 112, 'xiao大xiao大测试', '123123', '222222', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 100, 'xiao大xiao大测试', '123123', '222223', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 101, 'xiao大xiao大测试', '123123', '222223', 'MANUAL', '123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 12345, '小小小小测试', 1429350960, 1639850960, 'MANUAL', '123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 123456, '测试交卷', 1429350960, 1639850960, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 1234567, '测试交卷1', 1429350960, 1639850960, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 1234568, '测试交卷1', 1629350960, 1639850960, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');

insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 123, '你要做题么？', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 1234, '你不要做题吧？', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 1, '随便做题？', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 2, '没有正确答案？', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 3, '全部是正确答案？', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 4, '单选题？', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 5, '多选题？', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 6, '三个选项的？', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 7, '一个选项的？', 'OPTION_SINGLE');

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 25, '可以做', 123, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 26, '可以不做', 123, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 27, '做', 123, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 28, '不做', 123, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1, '可以做', 1, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 2, '可以不做', 1, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 3, '做', 1, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 4, '不做', 1, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 5, '没有正确答案', 2, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 6, '没有正确答案', 2, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 7, '没有正确答案', 2, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 8, '没有正确答案', 2, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 9, '全部是正确答案', 3, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 10, '全部是正确答案', 3, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 11, '全部是正确答案', 3, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 12, '全部是正确答案', 3, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 13, '我是单选题正确', 4, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 14, '我是单选题', 4, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 15, '我是单选题', 4, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 16, '我是单选题', 4, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 21, '我是多选题错误', 5, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 22, '我是多选题', 5, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 23, '我是多选题', 5, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 24, '我是多选题', 5, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 17, '我就三个选项', 6, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 18, '我就三个选项', 6, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 19, '我就三个选项', 6, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 20, '我就一个选项', 7, 1);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 1234567, 123, 100);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123, 123, 100);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 1234, 123, 50);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 1224, 123, 100);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 12345, 123, 50);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 1, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 2, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 3, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 4, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 5, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 6, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 123456, 7, 20);


insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124196, 1234, 123, 1, 1429460749);

insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124196, 112, 1429845746, 1429750960, 51);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124196, 111, 1429845746, 1429750960, 52);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124196, 123, 1429845746, 1429750960, 59);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124197, 123, 1429845746, 1429750960, 59);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124198, 123, 1429845746, 1429750960, 60);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124199, 123, 1429845746, 1429750960, 61);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124195, 123, 1429845746, 1429750960, 62);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score) values (0, 10000124194, 123, 1429845746, 1429750960, 61);


insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 111111, '考试已经结束', 1419845746, 1429845746, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 111112, '考试还未开始', 1519845746, 1529845746, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 111113, '考试还在进行', 1419845746, 1529845746, 'MANUAL', '7,2,3,1,5,4,6', 'AFTER_EXAM_END');

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 1, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 2, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 3, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 4, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 5, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 6, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111111, 7, 20);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 1, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 2, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 3, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 4, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 5, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 6, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111112, 7, 20);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 1, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 2, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 3, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 4, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 5, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 6, 10);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 111113, 7, 20);

insert into weizhu_exam_question_category (company_id, category_id, category_name, create_admin_id, create_time) values (0, 1, '测试', 1, 1429845746);
insert into weizhu_exam_question_category (company_id, category_id, category_name, parent_category_id, create_admin_id, create_time) values (0, 2, '测试1', 1, 1, 1429845746);
insert into weizhu_exam_question_category (company_id, category_id, category_name, parent_category_id, create_admin_id, create_time) values (0, 3, '测试2', 2, 1, 1429845746);
insert into weizhu_exam_question_category (company_id, category_id, category_name, create_admin_id, create_time) values (0, 4, '测试1', 1, 1429845746);
insert into weizhu_exam_question_category (company_id, category_id, category_name, parent_category_id, create_admin_id, create_time) values (0, 5, '测试3', 1, 1, 1429845746);

insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 1, 1);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 1, 2);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 1, 3);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 2, 4);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 2, 5);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 3, 6);
insert into weizhu_exam_question_category_join_question (company_id, category_id, question_id) values (0, 3, 7);


insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, show_result) values (0, 666, '题库随机出题', 1429350960, 1639850960, 'MANUAL', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 667, '正常出题', 1429350960, 1639850960, 'MANUAL', '7,6', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 668, '题库随机出题，正常出题混合', 1429350960, 1639850960, 'MANUAL', '7,6','AFTER_EXAM_END');

insert into weizhu_exam_exam_join_category (company_id, exam_id, question_category_str, question_num) values (0, 666, '1,2', 3);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 667, 6, 50);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 667, 7, 50);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 668, 6, 20);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 668, 7, 30);
insert into weizhu_exam_exam_join_category (company_id, exam_id, question_category_str, question_num) values (0, 668, '1,2', 3);
