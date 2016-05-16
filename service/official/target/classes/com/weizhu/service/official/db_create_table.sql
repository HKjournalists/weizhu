CREATE TABLE IF NOT EXISTS weizhu_official (
  company_id       BIGINT NOT NULL,
  official_id      BIGINT NOT NULL AUTO_INCREMENT,
  official_name    VARCHAR(191) NOT NULL,
  avatar           VARCHAR(191) NOT NULL,
  official_desc    VARCHAR(191),
  function_desc    VARCHAR(191),
  allow_model_id   INT,
  
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (official_id)
) AUTO_INCREMENT = 100001 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_official_send_plan (
  company_id       BIGINT NOT NULL,
  plan_id          INT NOT NULL AUTO_INCREMENT,
  official_id      BIGINT NOT NULL,
  send_time        INT NOT NULL,
  send_state       VARCHAR(191) NOT NULL,
  send_msg_ref_id  BIGINT NOT NULL,
  allow_model_id   INT,
  
  create_admin_id  BIGINT NOT NULL,
  create_time      INT NOT NULL,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_official_send_plan_head (
  company_id  BIGINT NOT NULL,
  plan_id     INT NOT NULL,
  head_type   VARCHAR(191) NOT NULL,
  head_data   BLOB NOT NULL,
  PRIMARY KEY (company_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_official_chat (
  company_id      BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  official_id     BIGINT NOT NULL,
  latest_msg_seq  BIGINT NOT NULL,
  latest_msg_time INT NOT NULL,
  PRIMARY KEY (company_id, user_id, official_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_official_msg (
  company_id    BIGINT NOT NULL,
  user_id       BIGINT NOT NULL,
  official_id   BIGINT NOT NULL,
  msg_seq       BIGINT NOT NULL,
  msg_time      INT NOT NULL,
  is_from_user  TINYINT(1) NOT NULL,
  msg_type      VARCHAR(191) NOT NULL,
  msg_data      MEDIUMBLOB,
  msg_ref_id    BIGINT,
  PRIMARY KEY (company_id, user_id, official_id, msg_seq),
  KEY (company_id, official_id, user_id, msg_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_official_msg_ref (
  msg_ref_id  BIGINT NOT NULL AUTO_INCREMENT,
  msg_type    VARCHAR(191) NOT NULL,
  msg_data    MEDIUMBLOB NOT NULL,
  PRIMARY KEY (msg_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
