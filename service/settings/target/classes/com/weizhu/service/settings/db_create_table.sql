CREATE TABLE IF NOT EXISTS weizhu_settings (
  company_id                BIGINT NOT NULL,
  user_id                   BIGINT NOT NULL,
  do_not_disturb_enable     TINYINT(1),
  do_not_disturb_begin_time INT,
  do_not_disturb_end_time   INT,
  PRIMARY KEY (company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;