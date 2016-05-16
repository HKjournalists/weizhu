CREATE TABLE IF NOT EXISTS weizhu_profile_value (
  company_id  BIGINT NOT NULL,
  `name`      VARCHAR(191) NOT NULL,
  `value`     TEXT NOT NULL,
  PRIMARY KEY (company_id, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_profile_comment (
  company_id  BIGINT NOT NULL,
  `name`      VARCHAR(191) NOT NULL,
  `comment`   TEXT NOT NULL,
  PRIMARY KEY (company_id, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
