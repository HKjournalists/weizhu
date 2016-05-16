CREATE TABLE IF NOT EXISTS weizhu_team (
  company_id       BIGINT NOT NULL,
  team_id          INT NOT NULL AUTO_INCREMENT,
  team_name        VARCHAR(191) NOT NULL,
  parent_team_id   INT,
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_position (
  company_id       BIGINT NOT NULL,
  position_id      INT NOT NULL AUTO_INCREMENT,
  position_name    VARCHAR(191) NOT NULL,
  position_desc    VARCHAR(191) NOT NULL,
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_level (
  company_id       BIGINT NOT NULL,
  level_id         INT NOT NULL AUTO_INCREMENT,
  level_name       VARCHAR(191) NOT NULL,
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS weizhu_user_base (
  company_id       BIGINT NOT NULL,
  user_id          BIGINT NOT NULL AUTO_INCREMENT,
  raw_id           VARCHAR(191) NOT NULL,
  raw_id_unique    VARCHAR(191),
  user_name        VARCHAR(191) NOT NULL,
  gender           VARCHAR(191),
  avatar           VARCHAR(191),
  email            VARCHAR(191),
  signature        VARCHAR(191),
  interest         VARCHAR(191),
  is_expert        TINYINT(1),
  level_id         INT,
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (user_id),
  UNIQUE KEY (company_id, raw_id_unique)
) AUTO_INCREMENT = 10000000001 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_base_mobile_no (
  company_id        BIGINT NOT NULL,
  user_id           BIGINT NOT NULL,
  mobile_no         VARCHAR(191) NOT NULL,
  mobile_no_unique  VARCHAR(191),
  PRIMARY KEY (company_id, user_id, mobile_no),
  UNIQUE KEY (company_id, mobile_no_unique)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_base_phone_no (
  company_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  phone_no    VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, user_id, phone_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_mark (
  company_id  BIGINT NOT NULL,
  marker_id   BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  is_star     TINYINT(1) NOT NULL,
  star_time   INT,
  mark_name   VARCHAR(191),
  PRIMARY KEY (company_id, marker_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_team (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  team_id      INT NOT NULL,
  position_id  INT,
  PRIMARY KEY (company_id, user_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_extends (
  company_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  `name`      VARCHAR(191) NOT NULL,
  `value`     VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, user_id, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_experience (
  company_id          BIGINT NOT NULL,
  user_id             BIGINT NOT NULL,
  experience_id       INT NOT NULL,
  experience_content  VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, user_id, experience_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_ability_tag (
  company_id      BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  tag_name        VARCHAR(191) NOT NULL,
  create_user_id  BIGINT,
  create_time     INT,
  PRIMARY KEY (company_id, user_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_user_ability_tag_user (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  tag_user_id  BIGINT NOT NULL,
  tag_name     VARCHAR(191) NOT NULL,
  tag_time     INT NOT NULL,
  PRIMARY KEY (company_id, user_id, tag_user_id, tag_name),
  KEY (company_id, user_id, tag_name, tag_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;