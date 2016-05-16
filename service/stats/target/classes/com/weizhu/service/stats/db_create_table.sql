CREATE TABLE IF NOT EXISTS weizhu_stats_dim_date (
  `date`     INT NOT NULL,
  `year`     INT NOT NULL,
  `quarter`  INT NOT NULL,
  `month`    INT NOT NULL,
  `week`     INT NOT NULL, 
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_dim_company (
  company_id    BIGINT NOT NULL,
  company_name  CHAR(50) NOT NULL,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_dim_user (
  user_id       BIGINT NOT NULL,
  user_name     CHAR(50) NOT NULL,
  state         CHAR(10) NOT NULL,
  create_time   BIGINT NOT NULL,
  gender        CHAR(10),
  level_id      BIGINT,
  level_name    CHAR(20),
  team_id_1     BIGINT,
  team_name_1   CHAR(20),
  team_id_2     BIGINT,
  team_name_2   CHAR(20),
  team_id_3     BIGINT,
  team_name_3   CHAR(20),
  team_id_4     BIGINT,
  team_name_4   CHAR(20),
  team_id_5     BIGINT,
  team_name_5   CHAR(20),
  team_id_6     BIGINT,
  team_name_6   CHAR(20),
  position_id   BIGINT,
  position_name CHAR(20),
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_dim_discover_item (
  item_id       BIGINT NOT NULL,
  item_name     CHAR(50) NOT NULL,
  state         CHAR(10) NOT NULL,
  create_time   BIGINT NOT NULL,
  module_id     BIGINT,
  module_name   CHAR(20),
  category_id   BIGINT,
  category_name CHAR(20),
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_weizhu_version (
  log_date      INT NOT NULL,
  platform      CHAR(10) NOT NULL,
  version_name  CHAR(50) NOT NULL,
  version_code  INT NOT NULL,
  company_id    BIGINT,
  user_id       BIGINT,
  log_cnt       BIGINT,
  PRIMARY KEY (log_date, platform, version_name, version_code, company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_admin_action (
  log_id      BIGINT NOT NULL AUTO_INCREMENT,
  log_time    BIGINT NOT NULL,
  log_date    INT NOT NULL,
  duration    INT NOT NULL,
  server      CHAR(50) NOT NULL,
  service     CHAR(50) NOT NULL,
  function    CHAR(50) NOT NULL,
  company_id  BIGINT,
  admin_id    BIGINT,
  session_id  BIGINT,
  result      CHAR(50),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_user_access (
  log_date    INT NOT NULL,
  company_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  log_cnt     BIGINT NOT NULL,
  PRIMARY KEY (log_date, company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_user_action (
  log_id      BIGINT NOT NULL AUTO_INCREMENT,
  log_time    BIGINT NOT NULL,
  log_date    INT NOT NULL,
  duration    INT NOT NULL,
  server      CHAR(50) NOT NULL,
  service     CHAR(50) NOT NULL,
  function    CHAR(50) NOT NULL,
  company_id  BIGINT,
  user_id     BIGINT,
  session_id  BIGINT,
  result      CHAR(50),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_user_discover (
  log_id      BIGINT NOT NULL AUTO_INCREMENT,
  log_time    BIGINT NOT NULL,
  log_date    INT NOT NULL,
  company_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  function    CHAR(50) NOT NULL,
  item_id     BIGINT NOT NULL,
  result      CHAR(50),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_stats_fact_user_login (
  log_id      BIGINT NOT NULL AUTO_INCREMENT,
  log_time    BIGINT NOT NULL,
  log_date    INT NOT NULL,
  company_id  BIGINT,
  user_id     BIGINT,
  session_id  BIGINT,
  function    CHAR(50) NOT NULL,
  company_key CHAR(30),
  mobile_no   CHAR(30),
  result      CHAR(50),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;