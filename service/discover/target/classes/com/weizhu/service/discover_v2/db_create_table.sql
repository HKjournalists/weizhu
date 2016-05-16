CREATE TABLE IF NOT EXISTS weizhu_discover_v2_home (
  company_id        BIGINT NOT NULL,
  banner_order_str  TEXT,
  module_order_str  TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_banner (
  company_id          BIGINT NOT NULL,
  banner_id           INT NOT NULL AUTO_INCREMENT,
  banner_name         VARCHAR(191) NOT NULL,
  image_name          VARCHAR(191) NOT NULL,
  allow_model_id      INT,
  item_id             BIGINT,
  `web_url.web_url`   VARCHAR(191),
  `web_url.is_weizhu` TINYINT(1),
  `app_uri.app_uri`   VARCHAR(191),
  
  state               VARCHAR(191),
  create_admin_id     BIGINT,
  create_time         INT,
  update_admin_id     BIGINT,
  update_time         INT,
  PRIMARY KEY (banner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module (
  company_id            BIGINT NOT NULL,
  module_id             INT NOT NULL AUTO_INCREMENT,
  module_name           VARCHAR(191) NOT NULL,
  image_name            VARCHAR(191) NOT NULL,
  allow_model_id        INT,
  `web_url.web_url`     VARCHAR(191),
  `web_url.is_weizhu`   TINYINT(1),
  `app_uri.app_uri`     VARCHAR(191),
  prompt_dot_timestamp  BIGINT,
  category_order_str    VARCHAR(191),
  
  state                 VARCHAR(191),
  create_admin_id       BIGINT,
  create_time           INT,
  update_admin_id       BIGINT,
  update_time           INT,
  PRIMARY KEY (module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module_category (
  company_id       BIGINT NOT NULL,
  category_id      INT NOT NULL AUTO_INCREMENT,
  category_name    VARCHAR(191) NOT NULL,
  module_id        INT NOT NULL,
  allow_model_id   INT,
  
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module_category_item (
  company_id       BIGINT NOT NULL,
  category_id      INT NOT NULL,
  item_id          BIGINT NOT NULL,
  create_admin_id  BIGINT,
  create_time      INT,
  PRIMARY KEY (company_id, category_id, item_id),
  KEY (company_id, category_id, create_time, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_base (
  company_id               BIGINT NOT NULL,
  item_id                  BIGINT NOT NULL AUTO_INCREMENT,
  item_name                VARCHAR(191) NOT NULL,
  item_desc                VARCHAR(191) NOT NULL,
  image_name               VARCHAR(191) NOT NULL,
  allow_model_id           INT,
  enable_comment           TINYINT(1) NOT NULL,
  enable_score             TINYINT(1) NOT NULL,
  enable_remind            TINYINT(1) NOT NULL,
  enable_like              TINYINT(1) NOT NULL,
  enable_share             TINYINT(1) NOT NULL,
  enable_external_share    TINYINT(1),
  
  `web_url.web_url`        VARCHAR(191),
  `web_url.is_weizhu`      TINYINT(1),
  `document.document_url`  VARCHAR(191),
  `document.document_type` VARCHAR(191),
  `document.document_size` INT,
  `document.check_md5`     VARCHAR(191),
  `document.is_download`   TINYINT(1),
  `document.is_auth_url`   TINYINT(1),
  `video.video_url`        VARCHAR(191),
  `video.video_type`       VARCHAR(191),
  `video.video_size`       INT,
  `video.video_time`       INT,
  `video.check_md5`        VARCHAR(191),
  `video.is_download`      TINYINT(1),
  `video.is_auth_url`      TINYINT(1),
  `audio.audio_url`        VARCHAR(191),
  `audio.audio_type`       VARCHAR(191),
  `audio.audio_size`       INT,
  `audio.audio_time`       INT,
  `audio.check_md5`        VARCHAR(191),
  `audio.is_download`      TINYINT(1),
  `audio.is_auth_url`      TINYINT(1),
  `app_uri.app_uri`        VARCHAR(191),
  
  state                    VARCHAR(191),
  create_admin_id          BIGINT,
  create_time              INT,
  update_admin_id          BIGINT,
  update_time              INT,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_learn (
  company_id     BIGINT NOT NULL,
  item_id        BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  learn_time     INT NOT NULL,
  learn_duration INT NOT NULL,
  learn_cnt      INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id),
  KEY (company_id, item_id, learn_time, user_id),
  KEY (company_id, user_id, learn_time, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_learn_log (
  company_id     BIGINT NOT NULL,
  log_id         BIGINT NOT NULL AUTO_INCREMENT,
  item_id        BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  learn_time     INT NOT NULL,
  learn_duration INT NOT NULL,
  is_report      TINYINT(1),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_comment (
  company_id   BIGINT NOT NULL,
  comment_id   BIGINT NOT NULL AUTO_INCREMENT,
  item_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  comment_time INT NOT NULL,
  comment_text VARCHAR(191) NOT NULL,
  is_delete    TINYINT(1) NOT NULL,
  PRIMARY KEY (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_score (
  company_id   BIGINT NOT NULL,
  item_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  score_time   INT NOT NULL,
  score_number INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_like (
  company_id BIGINT NOT NULL,
  item_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  like_time  INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_share (
  company_id  BIGINT NOT NULL,
  item_id     BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  share_time  INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;
