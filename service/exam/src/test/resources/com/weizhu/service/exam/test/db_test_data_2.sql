insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 10001, '考试1-[已结束]', '1430323200', '1430409600', 'MANUAL','123,123,123', 'AFTER_SUBMIT');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 10002, '考试2-[正在进行]', '1430409600', '1431187200', 'MANUAL','123,123,123', 'AFTER_EXAM_END');
insert into weizhu_exam_exam (company_id, exam_id, exam_name, start_time, end_time, type, question_order_str, show_result) values (0, 10003, '考试3-[未开考]', '1431187200', '1432051200', 'MANUAL','123,123,123', 'AFTER_EXAM_END');

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100001, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100002, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100003, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100004, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100005, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100006, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100007, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100008, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100009, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10001, 100010, 15);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100001, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100002, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100003, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100004, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100005, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100006, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100007, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100008, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100009, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10002, 100010, 15);

insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100001, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100002, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100003, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100004, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100005, 5);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100006, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100007, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100008, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100009, 15);
insert into weizhu_exam_exam_question (company_id, exam_id, question_id, score) values (0, 10003, 100010, 15);


insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100001, '单选题1', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100002, '单选题2', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100003, '单选题3', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100004, '单选题4', 'OPTION_SINGLE');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100005, '单选题5', 'OPTION_SINGLE');

insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100006, '多选题6', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100007, '多选题7', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100008, '多选题8', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100009, '多选题9', 'OPTION_MULTI');
insert into weizhu_exam_question (company_id, question_id, question_name, type) values (0, 100010, '多选题10', 'OPTION_MULTI');

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000001, '男', 100001, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000002, '女', 100001, 1);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000003, '甲', 100002, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000004, '乙', 100002, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000005, '丙', 100002, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000006, '丁', 100002, 1);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000007, '子', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000008, '丑', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000009, '寅', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000010, '卯', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000011, '辰', 100003, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000012, '巳', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000013, '午', 100003, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000014, '未', 100003, 0);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000015, 'xxx', 100004, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000016, 'yyy', 100004, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000017, 'zzz', 100004, 0);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000018, '🀙', 100005, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000019, '🀚', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000020, '🀛', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000021, '🀜', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000022, '🀝', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000023, '🀞', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000024, '🀟', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000025, '🀠', 100005, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000026, '🀡', 100005, 0);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000027, '男', 100006, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000028, '女', 100006, 1);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000029, '甲', 100007, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000030, '乙', 100007, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000031, '丙', 100007, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000032, '丁', 100007, 1);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000033, '子', 100008, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000034, '丑', 100008, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000035, '寅', 100008, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000036, '卯', 100008, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000037, '辰', 100008, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000038, '巳', 100008, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000039, '午', 100008, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000040, '未', 100008, 1);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000041, 'xxx', 100009, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000042, 'yyy', 100009, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000043, 'zzz', 100009, 0);

insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000044, '🀙', 100010, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000045, '🀚', 100010, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000046, '🀛', 100010, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000047, '🀜', 100010, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000048, '🀝', 100010, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000049, '🀞', 100010, 0);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000050, '🀟', 100010, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000051, '🀠', 100010, 1);
insert into weizhu_exam_option (company_id, option_id, option_name, question_id, is_right) values (0, 1000052, '🀡', 100010, 1);


insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100001, 1000001, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100002, 1000004, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100003, 1000011, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100004, 1000016, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100005, 1000018, 1430323500);

insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100006, 1000027, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100006, 1000028, 1430323500);

insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100008, 1000034, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100008, 1000037, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100008, 1000040, 1430323500);

insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100009, 1000041, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100009, 1000042, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100009, 1000043, 1430323500);

insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100010, 1000045, 1430323500);
insert into weizhu_exam_user_answer (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) values (0, 10000124207, 10001, 100010, 1000051, 1430323500);

insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score, team_id_1, team_id_2, position_id, level_id) values (0, 10000124207, 10001, 1430323500, 1430323500, 50, 1, 2, 1, 1);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score, team_id_1, team_id_2, position_id, level_id) values (0, 10000124196, 10001, 1430323500, 1430309600, 40, 1, 2, 1, 1);
insert into weizhu_exam_user_result (company_id, user_id, exam_id, start_time, submit_time, score, team_id_1, team_id_2, position_id, level_id) values (0, 10000124195, 10001, 1430323500, 1430309600, 80, 1, 3, 2, 3);