CREATE TABLE IF NOT EXISTS weizhu_allow_model (
  company_id        BIGINT NOT NULL,
  model_id          INT NOT NULL AUTO_INCREMENT,
  model_name        VARCHAR(191) NOT NULL,
  default_action    VARCHAR(191) NOT NULL,
  create_admin_id   BIGINT,
  create_time       INT,
  rule_order_str    TEXT,
  PRIMARY KEY (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_allow_rule (
  company_id  BIGINT NOT NULL,
  rule_id     INT NOT NULL AUTO_INCREMENT,
  model_id    INT NOT NULL,
  rule_name   VARCHAR(191) NOT NULL,
  `action`    VARCHAR(191) NOT NULL,
  rule_type   VARCHAR(191) NOT NULL,
  rule_data   BLOB,
  PRIMARY KEY (rule_id),
  KEY (company_id, model_id, rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;