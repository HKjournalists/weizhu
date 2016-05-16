CREATE TABLE IF NOT EXISTS weizhu_tools_productclock_customer (
  company_id      BIGINT NOT NULL,
  customer_id     INT NOT NULL AUTO_INCREMENT,
  customer_name   VARCHAR(191) NOT NULL,
  mobile_no       VARCHAR(11),
  gender          VARCHAR(10),
  birthday_solar  INT,
  birthday_lunar  INT,
  wedding_solar   INT,
  wedding_lunar   INT,
  address         VARCHAR(191),
  remark          VARCHAR(191),
  is_remind       TINYINT(1),
  belong_user     BIGINT,
  days_ago_remind INT,
  state           VARCHAR(191),
  create_user     BIGINT,
  create_admin    BIGINT,
  create_time     INT,
  update_user     BIGINT,
  update_admin    BIGINT,
  update_time     INT,
  PRIMARY KEY (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tools_productclock_product (
  company_id          BIGINT NOT NULL,
  product_id          INT NOT NULL AUTO_INCREMENT,
  product_name        VARCHAR(191),
  image_name          VARCHAR(191),
  default_remind_day  INT,
  product_desc        VARCHAR(191),
  state               VARCHAR(191),
  create_admin        BIGINT,
  create_time         INT,
  update_admin        BIGINT,
  update_time         INT,
  PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tools_productclock_customer_product (
  company_id        BIGINT NOT NULL,
  customer_id       INT,
  product_id        INT,
  buy_time          INT,
  remind_period_day INT,
  PRIMARY KEY (company_id, customer_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tools_productclock_communicate_record (
  company_id   BIGINT NOT NULL,
  record_id    INT NOT NULL AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  customer_id  INT NOT NULL,
  content_text VARCHAR(191),
  create_time  INT,
  PRIMARY KEY (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;