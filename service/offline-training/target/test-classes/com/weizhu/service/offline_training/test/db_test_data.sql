INSERT INTO weizhu_offline_training_train (
  company_id, train_id, train_name, start_time, end_time, 
  apply_enable, apply_start_time, apply_end_time, apply_user_count, apply_is_notify, 
  train_address, lecturer_name, 
  check_in_start_time, check_in_end_time, 
  arrangement_text, state
) VALUES (
  0, 1, '测试正在进行,无需报名的培训', UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  1, UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 10, 0,
  '地球', '人民', 
  UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  '直接讲', 'NORMAL'
), (
  0, 2, '测试已经结束的培训', UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2016-02-01 00:00:00'), 
  0, NULL, NULL, NULL, NULL,
  '地球2', '人民2', 
  UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2016-02-01 00:00:00'), 
  '直接讲', 'NORMAL'
), (
  0, 3, '测试禁用正在进行的培训', UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  0, NULL, NULL, NULL, NULL,
  '地球3', '人民3', 
  UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  '直接讲', 'DISABLE'
), (
  0, 4, '测试删除正在进行的培训', UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  0, NULL, NULL, NULL, NULL,
  '地球4', '人民4', 
  UNIX_TIMESTAMP('2016-01-01 00:00:00'), UNIX_TIMESTAMP('2030-01-01 00:00:00'), 
  '直接讲', 'DELETE'
);

INSERT INTO weizhu_offline_training_train_lecturer_user (
  company_id, train_id, user_id
) VALUES (
  0, 1, 100000001
);

INSERT INTO weizhu_offline_training_train_discover_item (
  company_id, train_id, item_id
) VALUES (
  0, 1, 1003
), (
  0, 1, 1004
), (
  0, 1, 1007
);

INSERT INTO weizhu_offline_training_user (
  company_id, train_id, user_id, 
  is_apply, apply_time, 
  is_check_in, check_in_time, 
  is_leave, leave_time, leave_reason, 
  update_time
) VALUES (
  0, 1, 100000,
  1, UNIX_TIMESTAMP('2016-01-02 11:00:00'),
  1, UNIX_TIMESTAMP('2016-02-02 12:00:00'),
  1, UNIX_TIMESTAMP('2016-03-03 13:00:00'), 'wakaka',
  UNIX_TIMESTAMP('2016-03-03 13:00:00')
);