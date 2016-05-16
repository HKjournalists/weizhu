CREATE TABLE IF NOT EXISTS weizhu_customer (
  user_id       BIGINT NOT NULL,
  customer_id   INT NOT NULL,
  customer_name VARCHAR(191) NOT NULL,
  mobile_no     VARCHAR(191) NOT NULL,
  is_star       TINYINT(1) NOT NULL,
  company       VARCHAR(191),
  position      VARCHAR(191),
  department    VARCHAR(191),
  address       VARCHAR(191),
  email         VARCHAR(191),
  wechat        VARCHAR(191),
  qq            BIGINT,
  remark        VARCHAR(191),
  PRIMARY KEY (user_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;