CREATE TABLE IF NOT EXISTS weizhu_push_seq (
  company_id BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  push_seq   BIGINT NOT NULL,
  PRIMARY KEY (company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;
