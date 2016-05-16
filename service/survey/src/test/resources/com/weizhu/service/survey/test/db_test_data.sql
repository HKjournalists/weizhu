INSERT INTO weizhu_survey (company_id, survey_id, survey_name, survey_desc, image_name, start_time, end_time, show_result_type, question_order_str, state, create_time, create_admin_id, update_time, update_admin_id) 
  VALUES (0, 1, '调研小模块(已经提交)', '为测试调研小系统的功能', '', 1443516518, 1453516518, 'ALWAYS_DETAIL', '3,2,1', 'NORMAL', 1443516518, 1, 0, 0);
INSERT INTO weizhu_survey (company_id, survey_id, survey_name, survey_desc, image_name, start_time, end_time, show_result_type, question_order_str, state, create_time, create_admin_id, update_time, update_admin_id) 
  VALUES (0, 2, '调研小模块(未开始)', '为测试调研小系统的功能', '', 1543516518, 1553516518, 'ALWAYS_DETAIL', '1,2,3', 'NORMAL', 1443516518, 1, 0, 0);

INSERT INTO weizhu_survey_question (company_id, question_id, question_name, image_name, is_optional, `type`, state, create_time, create_admin_id, update_time, update_admin_id)
  VALUES (0, 1, '测试投票', '', 1, 'VOTE', 'NORMAL', 1443516518, 1, 0, 0);
INSERT INTO weizhu_survey_question (company_id, question_id, question_name, image_name, is_optional, `type`, state, create_time, create_admin_id, update_time, update_admin_id)
  VALUES (0, 2, '测试下拉框', '', 1, 'INPUT_SELECT', 'NORMAL', 1443516518, 1, 0, 0);
INSERT INTO weizhu_survey_question (company_id, question_id, question_name, image_name, is_optional, `type`, state, create_time, create_admin_id, update_time, update_admin_id)
  VALUES (0, 3, '测试输入框', '', 1, 'INPUT_TEXT', 'NORMAL', 1443516518, 1, 0, 0);

INSERT INTO weizhu_survey_join_question (company_id, survey_id, question_id) VALUES (0, 1, 1);
INSERT INTO weizhu_survey_join_question (company_id, survey_id, question_id) VALUES (0, 1, 2);
INSERT INTO weizhu_survey_join_question (company_id, survey_id, question_id) VALUES (0, 1, 3);

INSERT INTO weizhu_survey_vote_question (company_id, question_id, check_number) VALUES (0, 1, 2);
INSERT INTO weizhu_survey_vote_option (company_id, option_id, option_name, question_id, image_name) VALUES (0, 1, '投票选项1', 1, '');
INSERT INTO weizhu_survey_vote_option (company_id, option_id, option_name, question_id, image_name) VALUES (0, 2, '投票选项2', 1, '');
INSERT INTO weizhu_survey_vote_option (company_id, option_id, option_name, question_id, image_name) VALUES (0, 3, '投票选项3', 1, '');

INSERT INTO weizhu_survey_input_text_question (company_id, question_id, input_prompt) VALUES (0, 3, '请在这里录入信息');

INSERT INTO weizhu_survey_input_select_option (company_id, option_id, option_name, question_id) VALUES (0, 1, '下拉框选项1', 2);
INSERT INTO weizhu_survey_input_select_option (company_id, option_id, option_name, question_id) VALUES (0, 2, '下拉框选项2', 2);
INSERT INTO weizhu_survey_input_select_option (company_id, option_id, option_name, question_id) VALUES (0, 3, '下拉框选项3', 2);

INSERT INTO weizhu_survey_result (company_id, survey_id, user_id, submit_time) VALUES (0, 1, 10000124203, 1445516518);
INSERT INTO weizhu_survey_result (company_id, survey_id, user_id, submit_time) VALUES (0, 1, 10000124207, 1445516518);
INSERT INTO weizhu_survey_result (company_id, survey_id, user_id, submit_time) VALUES (0, 1, 10000124209, 1445516518);

INSERT INTO weizhu_survey_vote_answer (company_id, question_id, user_id, option_id, answer_time) VALUES (0, 1, 10000124203, 1, 1445516518);
INSERT INTO weizhu_survey_vote_answer (company_id, question_id, user_id, option_id, answer_time) VALUES (0, 1, 10000124203, 2, 1445516518);
INSERT INTO weizhu_survey_input_select_answer (company_id, question_id, user_id, option_id, answer_time) VALUES (0, 2, 10000124203, 1, 1445516518);
INSERT INTO weizhu_survey_input_text_answer (company_id, question_id, user_id, result_text, answer_time) VALUES (0, 3, 10000124203, 'think in java', 1445516518);