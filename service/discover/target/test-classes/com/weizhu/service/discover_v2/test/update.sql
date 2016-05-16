CREATE TABLE IF NOT EXISTS weizhu_discover_v2_home_tmp (
  company_id        BIGINT NOT NULL,
  banner_order_str  TEXT,
  module_order_str  TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_banner_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module_category_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_module_category_item_tmp (
  company_id       BIGINT NOT NULL,
  category_id      INT NOT NULL,
  item_id          BIGINT NOT NULL,
  create_admin_id  BIGINT,
  create_time      INT,
  PRIMARY KEY (company_id, category_id, item_id),
  KEY (company_id, category_id, create_time, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_base_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_learn_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_learn_log_tmp (
  company_id     BIGINT NOT NULL,
  log_id         BIGINT NOT NULL AUTO_INCREMENT,
  item_id        BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  learn_time     INT NOT NULL,
  learn_duration INT NOT NULL,
  is_report      TINYINT(1),
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_comment_tmp (
  company_id   BIGINT NOT NULL,
  comment_id   BIGINT NOT NULL AUTO_INCREMENT,
  item_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  comment_time INT NOT NULL,
  comment_text VARCHAR(191) NOT NULL,
  is_delete    TINYINT(1) NOT NULL,
  PRIMARY KEY (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_score_tmp (
  company_id   BIGINT NOT NULL,
  item_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  score_time   INT NOT NULL,
  score_number INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_like_tmp (
  company_id BIGINT NOT NULL,
  item_id    BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  like_time  INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_v2_item_share_tmp (
  company_id  BIGINT NOT NULL,
  item_id     BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  share_time  INT NOT NULL,
  PRIMARY KEY (company_id, item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

SET @COMPANY_ID=0;

INSERT INTO weizhu_discover_v2_home_tmp 
  (company_id, banner_order_str, module_order_str) 
SELECT @COMPANY_ID AS company_id, banner_order_str, module_order_str FROM weizhu_discover_v2_home;

INSERT INTO weizhu_discover_v2_banner_tmp 
  (company_id,  banner_id, banner_name, image_name,  allow_model_id, item_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, state, create_admin_id, create_time, update_admin_id, update_time) 
SELECT @COMPANY_ID AS company_id, banner_id, banner_name, image_name,  allow_model_id, item_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, state, create_admin_id, create_time, update_admin_id, update_time FROM weizhu_discover_v2_banner;

INSERT INTO weizhu_discover_v2_module_tmp 
  (company_id, module_id, module_name, image_name, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, prompt_dot_timestamp, category_order_str, state, create_admin_id, create_time, update_admin_id, update_time)
SELECT @COMPANY_ID AS company_id, module_id, module_name, image_name, allow_model_id, `web_url.web_url`, `web_url.is_weizhu`, `app_uri.app_uri`, prompt_dot_timestamp, category_order_str, state, create_admin_id, create_time, update_admin_id, update_time FROM weizhu_discover_v2_module;

INSERT INTO weizhu_discover_v2_module_category_tmp 
  (company_id, category_id, category_name, module_id, allow_model_id, state, create_admin_id, create_time, update_admin_id, update_time)
SELECT @COMPANY_ID AS company_id, category_id, category_name, module_id, allow_model_id, state, create_admin_id, create_time, update_admin_id, update_time FROM weizhu_discover_v2_module_category;

INSERT INTO weizhu_discover_v2_module_category_item_tmp 
  (company_id, category_id, item_id, create_admin_id, create_time)
SELECT @COMPANY_ID AS company_id, category_id, item_id, create_admin_id, create_time FROM weizhu_discover_v2_module_category_item;

INSERT INTO weizhu_discover_v2_item_base_tmp
  (company_id, item_id, item_name, item_desc, image_name, allow_model_id, enable_comment, enable_score, enable_remind, enable_like, enable_share, 
  `web_url.web_url`, `web_url.is_weizhu`, `document.document_url`, `document.document_type`, `document.document_size`,
  `document.check_md5`, `document.is_download`, `document.is_auth_url`,   `video.video_url`, `video.video_type`,
  `video.video_size`, `video.video_time`, `video.check_md5`, `video.is_download`,  `video.is_auth_url`, `audio.audio_url`, `audio.audio_type`, `audio.audio_size`,
  `audio.audio_time`, `audio.check_md5`, `audio.is_download`, `audio.is_auth_url`, `app_uri.app_uri`, state, create_admin_id,  create_time , update_admin_id, update_time) 
SELECT  @COMPANY_ID AS company_id, item_id, item_name, item_desc, image_name, allow_model_id, enable_comment, enable_score, enable_remind, enable_like, enable_share, 
  `web_url.web_url`, `web_url.is_weizhu`, `document.document_url`, `document.document_type`, `document.document_size`,
  `document.check_md5`, `document.is_download`, `document.is_auth_url`,   `video.video_url`, `video.video_type`,
  `video.video_size`, `video.video_time`, `video.check_md5`, `video.is_download`,  `video.is_auth_url`, `audio.audio_url`, `audio.audio_type`, `audio.audio_size`,
  `audio.audio_time`, `audio.check_md5`, `audio.is_download`, `audio.is_auth_url`, `app_uri.app_uri`, state, create_admin_id,  create_time , update_admin_id, update_time FROM weizhu_discover_v2_item_base;
  
INSERT INTO weizhu_discover_v2_item_learn_tmp 
  (company_id, item_id, user_id, learn_time, learn_duration, learn_cnt)
SELECT @COMPANY_ID AS company_id, item_id, user_id, learn_time, learn_duration, learn_cnt FROM weizhu_discover_v2_item_learn;

INSERT INTO weizhu_discover_v2_item_learn_log_tmp 
  (company_id, log_id, item_id, user_id, learn_time, learn_duration, is_report)
SELECT @COMPANY_ID AS company_id, log_id, item_id, user_id, learn_time, learn_duration, is_report FROM weizhu_discover_v2_item_learn_log;

INSERT INTO weizhu_discover_v2_item_comment_tmp 
  (company_id, comment_id, item_id, user_id, comment_time, comment_text, is_delete)
SELECT @COMPANY_ID AS company_id, comment_id, item_id, user_id, comment_time, comment_text, is_delete FROM weizhu_discover_v2_item_comment;

INSERT INTO weizhu_discover_v2_item_score_tmp 
  (company_id, item_id, user_id, score_time, score_number)
SELECT @COMPANY_ID AS company_id, item_id, user_id, score_time, score_number FROM weizhu_discover_v2_item_score;

INSERT INTO weizhu_discover_v2_item_like_tmp 
  (company_id, item_id, user_id, like_time)
SELECT @COMPANY_ID AS company_id, item_id, user_id, like_time FROM weizhu_discover_v2_item_like;

INSERT INTO weizhu_discover_v2_item_share_tmp 
  (company_id, item_id, user_id, share_time)
SELECT @COMPANY_ID AS company_id, item_id, user_id, share_time FROM weizhu_discover_v2_item_share;

ALTER TABLE weizhu_discover_v2_home RENAME TO weizhu_discover_v2_home_bak;
ALTER TABLE weizhu_discover_v2_banner RENAME TO weizhu_discover_v2_banner_bak;
ALTER TABLE weizhu_discover_v2_module RENAME TO weizhu_discover_v2_module_bak;
ALTER TABLE weizhu_discover_v2_module_category RENAME TO weizhu_discover_v2_module_category_bak;
ALTER TABLE weizhu_discover_v2_module_category_item RENAME TO weizhu_discover_v2_module_category_item_bak;
ALTER TABLE weizhu_discover_v2_item_base RENAME TO weizhu_discover_v2_item_base_bak;
ALTER TABLE weizhu_discover_v2_item_learn RENAME TO weizhu_discover_v2_item_learn_bak;
ALTER TABLE weizhu_discover_v2_item_learn_log RENAME TO weizhu_discover_v2_item_learn_log_bak;
ALTER TABLE weizhu_discover_v2_item_comment RENAME TO weizhu_discover_v2_item_comment_bak;
ALTER TABLE weizhu_discover_v2_item_score RENAME TO weizhu_discover_v2_item_score_bak;
ALTER TABLE weizhu_discover_v2_item_like RENAME TO weizhu_discover_v2_item_like_bak;
ALTER TABLE weizhu_discover_v2_item_share RENAME TO weizhu_discover_v2_item_share_bak;

ALTER TABLE weizhu_discover_v2_home_tmp RENAME TO weizhu_discover_v2_home;
ALTER TABLE weizhu_discover_v2_banner_tmp RENAME TO weizhu_discover_v2_banner;
ALTER TABLE weizhu_discover_v2_module_tmp RENAME TO weizhu_discover_v2_module;
ALTER TABLE weizhu_discover_v2_module_category_tmp RENAME TO weizhu_discover_v2_module_category;
ALTER TABLE weizhu_discover_v2_module_category_item_tmp RENAME TO weizhu_discover_v2_module_category_item;
ALTER TABLE weizhu_discover_v2_item_base_tmp RENAME TO weizhu_discover_v2_item_base;
ALTER TABLE weizhu_discover_v2_item_learn_tmp RENAME TO weizhu_discover_v2_item_learn;
ALTER TABLE weizhu_discover_v2_item_learn_log_tmp RENAME TO weizhu_discover_v2_item_learn_log;
ALTER TABLE weizhu_discover_v2_item_comment_tmp RENAME TO weizhu_discover_v2_item_comment;
ALTER TABLE weizhu_discover_v2_item_score_tmp RENAME TO weizhu_discover_v2_item_score;
ALTER TABLE weizhu_discover_v2_item_like_tmp RENAME TO weizhu_discover_v2_item_like;
ALTER TABLE weizhu_discover_v2_item_share_tmp RENAME TO weizhu_discover_v2_item_share;

SELECT DISTINCT company_id FROM weizhu_discover_v2_home;
SELECT DISTINCT company_id FROM weizhu_discover_v2_banner;
SELECT DISTINCT company_id FROM weizhu_discover_v2_module;
SELECT DISTINCT company_id FROM weizhu_discover_v2_module_category;
SELECT DISTINCT company_id FROM weizhu_discover_v2_module_category_item;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_base;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_learn;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_learn_log;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_comment;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_score;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_like;
SELECT DISTINCT company_id FROM weizhu_discover_v2_item_share;
