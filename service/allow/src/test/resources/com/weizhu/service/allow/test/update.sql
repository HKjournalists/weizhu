SET @COMPANY_ID=14;

CREATE TABLE IF NOT EXISTS tmp_weizhu_allow_model (
  company_id        BIGINT NOT NULL,
  model_id          INT NOT NULL AUTO_INCREMENT,
  model_name        VARCHAR(191) NOT NULL,
  default_action    VARCHAR(191) NOT NULL,
  create_admin_id   BIGINT,
  create_time       INT,
  rule_order_str    TEXT,
  PRIMARY KEY (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tmp_weizhu_allow_rule (
  company_id  BIGINT NOT NULL,
  rule_id     INT NOT NULL AUTO_INCREMENT,
  model_id    INT NOT NULL,
  rule_name   VARCHAR(191) NOT NULL,
  `action`    VARCHAR(191) NOT NULL,
  rule_type   VARCHAR(191) NOT NULL,
  rule_data   BLOB,
  PRIMARY KEY (rule_id),
  KEY (model_id, rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

INSERT INTO tmp_weizhu_allow_model
  (company_id, model_id, model_name, default_action, create_admin_id, create_time, rule_order_str) 
SELECT @COMPANY_ID, model_id, model_name, default_action, create_admin_id, create_time, rule_order_str FROM weizhu_allow_model;

INSERT INTO tmp_weizhu_allow_rule
  (company_id, rule_id, model_id, rule_name, `action`, rule_type, rule_data) 
SELECT @COMPANY_ID, rule_id, model_id, rule_name, `action`, rule_type, rule_data FROM weizhu_allow_rule;

RENAME TABLE weizhu_allow_model TO bak_weizhu_allow_model;
RENAME TABLE tmp_weizhu_allow_model TO weizhu_allow_model;

RENAME TABLE weizhu_allow_rule TO bak_weizhu_allow_rule;
RENAME TABLE tmp_weizhu_allow_rule TO weizhu_allow_rule;

SELECT distinct(company_id) FROM weizhu_allow_model;
SELECT distinct(company_id) FROM weizhu_allow_rule;