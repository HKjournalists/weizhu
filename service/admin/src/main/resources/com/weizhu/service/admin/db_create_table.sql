
CREATE TABLE IF NOT EXISTS weizhu_admin_session (
  admin_id     BIGINT NOT NULL,
  session_id   BIGINT NOT NULL,
  login_time   INT NOT NULL,
  login_host   VARCHAR(191) NOT NULL,
  user_agent   TEXT NOT NULL,
  active_time  INT NOT NULL,
  logout_time  INT,
  PRIMARY KEY (admin_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin (
  admin_id              BIGINT NOT NULL AUTO_INCREMENT,
  admin_name            VARCHAR(191) NOT NULL,
  admin_email           VARCHAR(191) NOT NULL,
  admin_email_unique    VARCHAR(191),
  admin_password        VARCHAR(191) NOT NULL,
  force_reset_password  TINYINT(1) NOT NULL,
  state                 VARCHAR(191),
  create_time           INT,
  create_admin_id       BIGINT,
  update_time           INT,
  update_admin_id       BIGINT,
  PRIMARY KEY (admin_id),
  UNIQUE KEY (admin_email_unique)
) AUTO_INCREMENT = 100001 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin_company (
  admin_id            BIGINT NOT NULL,
  company_id          BIGINT NOT NULL,
  enable_team_permit  TINYINT(1) NOT NULL,
  PRIMARY KEY (admin_id, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin_company_role (
  admin_id    BIGINT NOT NULL,
  company_id  BIGINT NOT NULL,
  role_id     INT NOT NULL,
  PRIMARY KEY (admin_id, company_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin_company_team_permit (
  admin_id        BIGINT NOT NULL,
  company_id      BIGINT NOT NULL,
  permit_team_id  INT NOT NULL,
  PRIMARY KEY (admin_id, company_id, permit_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin_role (
  company_id       BIGINT,
  role_id          INT NOT NULL AUTO_INCREMENT,
  role_name        VARCHAR(191) NOT NULL,
  state            VARCHAR(191),
  create_time      INT,
  create_admin_id  BIGINT,
  update_time      INT,
  update_admin_id  BIGINT,
  PRIMARY KEY (role_id)
) AUTO_INCREMENT = 1001 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_admin_role_permission (
  role_id        INT NOT NULL,
  permission_id  VARCHAR(191) NOT NULL,
  PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
