CREATE TABLE IF NOT EXISTS weizhu_session (
  company_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  session_id  BIGINT NOT NULL,
  login_time  INT NOT NULL,
  active_time INT NOT NULL,
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_session_weizhu (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  session_id   BIGINT NOT NULL,
  platform     VARCHAR(191) NOT NULL,
  version_name VARCHAR(191) NOT NULL,
  version_code INT NOT NULL,
  stage        VARCHAR(191) NOT NULL,
  build_time   INT NOT NULL,
  build_hash   VARCHAR(191),
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_session_android (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  session_id   BIGINT NOT NULL,
  device       VARCHAR(191) NOT NULL,
  manufacturer VARCHAR(191) NOT NULL,
  brand        VARCHAR(191) NOT NULL,
  model        VARCHAR(191) NOT NULL,
  serial       VARCHAR(191) NOT NULL,
  `release`    VARCHAR(191) NOT NULL,
  sdk_int      INT NOT NULL,
  codename     VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_session_iphone (
  company_id      BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  session_id      BIGINT NOT NULL,
  name            VARCHAR(191) NOT NULL,
  system_name     VARCHAR(191) NOT NULL,
  system_version  VARCHAR(191) NOT NULL,
  model           VARCHAR(191) NOT NULL,
  localized_model VARCHAR(191) NOT NULL,
  device_token    VARCHAR(191) NOT NULL,
  mac             VARCHAR(191) NOT NULL,
  app_id          VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_session_web_mobile (
  company_id      BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  session_id      BIGINT NOT NULL,
  user_agent      TEXT,
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_session_web_login (
  company_id      BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  session_id      BIGINT NOT NULL,
  weblogin_id     BIGINT NOT NULL,
  login_time      INT NOT NULL,
  active_time     INT NOT NULL,
  user_agent      TEXT,
  PRIMARY KEY (company_id, user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;