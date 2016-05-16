INSERT INTO weizhu_profile_value (company_id, name, value) VALUES (0, 'credits:app_key', '3E9TTxbaiN5cMYRQryvHe5w974xC');
INSERT INTO weizhu_profile_value (company_id, name, value) VALUES (0, 'credits:app_secret', 'ADW564kbSzR93SpCDSE7EEq8QDH');

INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 10000124196, 222);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 20000124196, 221);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 30000124196, 223);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 40000124196, 224);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 50000124196, 225);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 60000124196, 226);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 70000124196, 227);
INSERT INTO weizhu_credits_user (company_id, user_id, credits) VALUES (0, 80000124196, 228);

INSERT INTO weizhu_credits (company_id, credits, version) VALUES (0, 1111, 1);

INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 1, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'SUCCESS', 1452964116, 0, '01');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num, operation_id) VALUES (0, 2, 10000124196, 'WEIZHU_INCOME', 20, '兑换失败，返还积分', 'SUCCESS', 1452964116, 0, '02', 1);
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 3, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '03');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 4, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '04');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 5, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '05');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 6, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '06');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 7, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '07');
INSERT INTO weizhu_credits_order (company_id, order_id, user_id, `type`, credits_delta, `desc`, state, create_time, create_admin, order_num) VALUES (0, 8, 10000124196, 'EXPENSE', -20, '积分商城兑换礼品0', 'FAIL', 1452964116, 0, '08');

INSERT INTO weizhu_credits_rule (company_id, rule) VALUES (0, '1111111111111122222222222222222222333333333333333333aaaaaaaaaaaaaaaaabbbbbbbbbbbb');

INSERT INTO weizhu_credits_operation (company_id, operation_id, `desc`, create_time, create_admin) VALUES (0, 1, 'AABAUUCDD', 1111111, 1);