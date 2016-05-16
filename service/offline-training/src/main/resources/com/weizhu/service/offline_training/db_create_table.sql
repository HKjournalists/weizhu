CREATE TABLE IF NOT EXISTS weizhu_offline_training_train (
  company_id           BIGINT NOT NULL,
  train_id             INT NOT NULL AUTO_INCREMENT,
  train_name           VARCHAR(191) NOT NULL,
  image_name           VARCHAR(191),
  start_time           INT NOT NULL,
  end_time             INT NOT NULL,
  apply_enable         TINYINT NOT NULL,
  apply_start_time     INT,
  apply_end_time       INT,
  apply_user_count     INT,
  apply_is_notify      TINYINT,
  train_address        VARCHAR(191) NOT NULL,
  lecturer_name        VARCHAR(191),
  check_in_start_time  INT NOT NULL,
  check_in_end_time    INT NOT NULL,
  arrangement_text     TEXT NOT NULL,
  describe_text        TEXT,
  allow_model_id       INT,
  
  state                VARCHAR(191),
  create_time          INT,
  create_admin_id      BIGINT,
  update_time          INT,
  update_admin_id      BIGINT,
  PRIMARY KEY (train_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_offline_training_train_lecturer_user (
  company_id  BIGINT NOT NULL,
  train_id    INT NOT NULL,
  user_id     BIGINT NOT NULL,
  PRIMARY KEY (company_id, train_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_offline_training_train_discover_item (
  company_id  BIGINT NOT NULL,
  train_id    INT NOT NULL,
  item_id     BIGINT NOT NULL,
  PRIMARY KEY (company_id, train_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_offline_training_user (
  company_id     BIGINT NOT NULL,
  train_id       INT NOT NULL,
  user_id        BIGINT NOT NULL,
  is_apply       TINYINT NOT NULL,
  apply_time     INT,
  is_check_in    TINYINT NOT NULL,
  check_in_time  INT,
  is_leave       TINYINT NOT NULL,
  leave_time     INT,
  leave_reason   VARCHAR(191),
  update_time    INT,
  PRIMARY KEY (company_id, train_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
