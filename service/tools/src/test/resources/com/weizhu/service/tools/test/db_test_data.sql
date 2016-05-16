INSERT INTO weizhu_tools_productclock_customer (company_id,customer_id,customer_name,mobile_no,gender,birthday_solar,birthday_lunar,
wedding_solar,wedding_lunar,address,remark,is_remind,belong_user,state,create_user,create_admin,create_time) VALUES 
(0, 1, '核桃', '18611111111', 'MALE', 121212121, 1212121, 1212121, 1212121, '袋子里', '保质期长', 1, 10, 'NORMAL', 10, 10, 1121212);
INSERT INTO weizhu_tools_productclock_customer (company_id,customer_id,customer_name,mobile_no,gender,birthday_solar,birthday_lunar,
wedding_solar,wedding_lunar,address,remark,is_remind,belong_user,state,create_user,create_admin,create_time) VALUES 
(0, 2, '噹哄噹', '15632117078', 'MALE', 586969200, 586969200, 1417017600, 1417017600, '河北石家庄藁城', 'java程序员', 1, 10, 'NORMAL', 1, 10, 1460548043);

INSERT INTO weizhu_tools_productclock_product (company_id, product_id, product_name, image_name, default_remind_day, product_desc, state, create_admin, create_time) VALUES
(0, 1, '微助', 'abc.jpg', 10, '好用，易上手', 'NORMAL', 1, 1460548043);
INSERT INTO weizhu_tools_productclock_product (company_id, product_id, product_name, image_name, default_remind_day, product_desc, state, create_admin, create_time) VALUES
(0, 2, 'MAC', 'abc1.jpg', 1, '好看', 'NORMAL', 1, 1460548043);
INSERT INTO weizhu_tools_productclock_product (company_id, product_id, product_name, image_name, default_remind_day, product_desc, state, create_admin, create_time) VALUES
(0, 3, 'IPHONE', 'abc2.jpg', 4, 'd顶', 'NORMAL', 1, 1460548043);
INSERT INTO weizhu_tools_productclock_product (company_id, product_id, product_name, image_name, default_remind_day, product_desc, state, create_admin, create_time) VALUES
(0, 4, 'GOOGLE', 'abc3.jpg', 20, 'guice， guava', 'NORMAL', 1, 1460548043);

INSERT INTO weizhu_tools_productclock_customer_product (company_id, customer_id, product_id, buy_time, remind_period_day) VALUES (0, 2, 1, 1460548043, 5);
INSERT INTO weizhu_tools_productclock_customer_product (company_id, customer_id, product_id, buy_time, remind_period_day) VALUES (0, 2, 2, 1460548043, 5);
INSERT INTO weizhu_tools_productclock_customer_product (company_id, customer_id, product_id, buy_time, remind_period_day) VALUES (0, 2, 3, 1460548043, 5);
