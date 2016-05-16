CREATE TABLE IF NOT EXISTS weizhu_stats_user (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  user_name    VARCHAR(191) NOT NULL,
  raw_id       VARCHAR(191) NOT NULL,
  team_1_id    INT,
  team_1_name  VARCHAR(191),
  team_2_id    INT,
  team_2_name  VARCHAR(191),
  team_3_id    INT,
  team_3_name  VARCHAR(191),
  team_4_id    INT,
  team_4_name  VARCHAR(191),
  team_5_id    INT,
  team_5_name  VARCHAR(191),
  PRIMARY KEY (company_id, user_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS weizhu_stats_api (
  seq_id       BIGINT NOT NULL AUTO_INCREMENT,
  time_millis  BIGINT NOT NULL,
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  session_id   BIGINT NOT NULL,
  invoke_name  VARCHAR(191) NOT NULL,
  PRIMARY KEY (seq_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;
