CREATE TABLE IF NOT EXISTS weizhu_absence (
  company_id   BIGINT NOT NULL, 
  absence_id   INT NOT NULL AUTO_INCREMENT,
  `type`       VARCHAR(191),
  start_time   INT NOT NULL,
  pre_end_time INT NOT NULL,
  fac_end_time INT,
  `desc`       VARCHAR(191),
  days         VARCHAR(191),
  state        VARCHAR(191),
  create_user  BIGINT, 
  create_time  INT,
  PRIMARY KEY (absence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_absence_notify_user (
  company_id   BIGINT NOT NULL,
  absence_id   INT NOT NULL,
  user_id      BIGINT NOT NULL,
  PRIMARY KEY (company_id, absence_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_absence_head (
  company_id   BIGINT NOT NULL,
  absence_id   INT NOT NULL,
  head_data    BLOB,
  PRIMARY KEY (company_id, absence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;