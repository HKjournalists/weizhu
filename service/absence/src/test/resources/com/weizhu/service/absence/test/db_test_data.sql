INSERT INTO weizhu_absence (company_id, absence_id, `type`, start_time, pre_end_time, fac_end_time, `desc`, days, state, create_user, create_time) VALUES (0, 1, '事假', 1456070400, 1582300800, 1511300800, '现在肚子疼', '2', 'NORMAL', 1, 1456070400);
INSERT INTO weizhu_absence (company_id, absence_id, `type`, start_time, pre_end_time, `desc`, days, state, create_user, create_time) VALUES (0, 2, '病假', 1582300800, 1740153600, '未来我会肚子疼', '2', 'NORMAL', 10000124196, 1456070400);
INSERT INTO weizhu_absence (company_id, absence_id, `type`, start_time, pre_end_time, `desc`, days, state, create_user, create_time) VALUES (0, 3, '婚假', 951148800, 1456070400, '我已经结过婚了', '2', 'NORMAL', 1, 1456070400);

INSERT INTO weizhu_absence_notify_user (company_id, absence_id, user_id) VALUES (0, 2, 10000124196); 
INSERT INTO weizhu_absence_notify_user (company_id, absence_id, user_id) VALUES (0, 2, 10000124197); 
INSERT INTO weizhu_absence_notify_user (company_id, absence_id, user_id) VALUES (0, 2, 10000124198); 
INSERT INTO weizhu_absence_notify_user (company_id, absence_id, user_id) VALUES (0, 2, 10000124199); 