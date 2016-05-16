CREATE TABLE IF NOT EXISTS weizhu_apns_cert (
  app_id         VARCHAR(191) NOT NULL,
  is_production  TINYINT(1) NOT NULL,
  cert_p12       BLOB,
  cert_pass      VARCHAR(191) NOT NULL,
  expired_time   INT NOT NULL,
  PRIMARY KEY (app_id, is_production)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_apns_device_token (
  company_id    BIGINT NOT NULL,
  user_id       BIGINT NOT NULL,
  session_id    BIGINT NOT NULL,
  app_id        VARCHAR(191) NOT NULL,
  is_production TINYINT(1) NOT NULL,
  device_token  VARCHAR(191) NOT NULL,
  badge_number  INT NOT NULL,
  PRIMARY KEY (company_id, user_id, session_id),
  KEY (app_id, is_production, device_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;
