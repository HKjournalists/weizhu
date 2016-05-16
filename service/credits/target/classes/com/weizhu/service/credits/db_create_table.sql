CREATE TABLE IF NOT EXISTS weizhu_credits (
  company_id BIGINT NOT NULL,
  credits    BIGINT NOT NULL,
  version    INT    NOT NULL,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_credits_log (
  company_id    BIGINT NOT NULL,
  log_id        INT NOT NULL AUTO_INCREMENT,
  credits_delta BIGINT NOT NULL,
  `desc`        VARCHAR(191),
  create_time   INT NOT NULL,
  create_admin  BIGINT NOT NULL,
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_credits_user (
  company_id BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  credits    BIGINT NOT NULL,
  PRIMARY KEY (company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_credits_order (
  company_id    BIGINT NOT NULL,
  order_id      INT    NOT NULL AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  `type`        VARCHAR(191) NOT NULL,
  credits_delta BIGINT NOT NULL,
  `desc`        VARCHAR(191) NOT NULL,
  state         VARCHAR(191) NOT NULL,
  create_time   INT NOT NULL,
  create_admin  BIGINT,
  order_num     VARCHAR(191),
  operation_id  INT,
  params        TEXT,
  PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_credits_rule (
  company_id BIGINT NOT NULL,
  rule       TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_credits_operation (
  company_id   BIGINT NOT NULL,
  operation_id INT NOT NULL AUTO_INCREMENT,
  `desc`       VARCHAR(191),
  create_time  INT,
  create_admin BIGINT,
  PRIMARY KEY (operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;