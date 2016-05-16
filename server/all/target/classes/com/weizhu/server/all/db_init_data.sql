INSERT IGNORE INTO weizhu_company (company_id, company_name, server_name) VALUES (1, '微助', 'LOCAL');
INSERT IGNORE INTO weizhu_company_key (company_id, company_key) VALUES (1, 'weizhu');

INSERT IGNORE INTO weizhu_admin (admin_id, company_id, admin_name, admin_email, admin_password, is_enable, force_reset_password) VALUES 
(1, 1, '微助超级管理员', 'weizhu@wehelpu.cn', 'afd4d5a811e7dd7d161fb5f4769ceb0c1e5b1c07', 1, 0);

INSERT IGNORE INTO weizhu_admin_permission 
  (admin_id, permission_id) 
VALUES 
  (1, 1001),
  (1, 1002),
  (1, 1003),
  (1, 1004),
  (1, 2001),
  (1, 2002),
  (1, 2003),
  (1, 2004),
  (1, 2005),
  (1, 2006),
  (1, 2007),
  (1, 2008),
  (1, 2009),
  (1, 2010),
  (1, 3001),
  (1, 3002),
  (1, 3003),
  (1, 3004),
  (1, 3005);
  
INSERT IGNORE INTO weizhu_user_base 
  (company_id, user_id, raw_id, raw_id_unique, user_name, gender, avatar, state) 
VALUES 
  (1, 10000000001, 'test', 'test', '测试用户', 'MALE', '', 'NORMAL');
  
INSERT IGNORE INTO weizhu_user_base_mobile_no 
  (company_id, user_id, mobile_no, mobile_no_unique) 
VALUES 
  (1, 10000000001, '18600000000', '18600000000');