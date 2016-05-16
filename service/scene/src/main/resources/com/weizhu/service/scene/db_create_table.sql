
-- 创建场景数据库表

CREATE TABLE IF NOT EXISTS weizhu_scene_home (
  company_id          BIGINT NOT NULL,
  scene_id_order_str  TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_scene_scene (
  company_id          BIGINT NOT NULL,
  scene_id            INT NOT NULL AUTO_INCREMENT,
  scene_name          VARCHAR(191) NOT NULL,
  image_name          VARCHAR(191) NOT NULL,
  scene_desc          VARCHAR(191) NOT NULL,
  parent_scene_id     INT,
  is_leaf_scene       TINYINT(1) NOT NULL,
  item_id_order_str   TEXT,
  state               VARCHAR(191) NOT NULL,
  create_admin_id     BIGINT NOT NULL,
  create_time         INT NOT NULL,
  update_admin_id     BIGINT,
  update_time         INT,
  PRIMARY KEY (scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_scene_item_index (
  company_id        BIGINT NOT NULL,
  item_id           INT NOT NULL AUTO_INCREMENT,
  scene_id          INT NOT NULL,
  discover_item_id  BIGINT,
  community_item_id INT,
  state             VARCHAR(191) NOT NULL,
  create_admin_id   BIGINT NOT NULL,
  create_time       INT NOT NULL,
  update_admin_id   BIGINT,
  update_time       INT,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


-- 创建工具中的盖帽神器（超值推荐）数据库表

CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_home (
  company_id             BIGINT NOT NULL,
  home_name              VARCHAR(191) NOT NULL,
  category_id_order_str  TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_category (
  company_id                BIGINT NOT NULL,
  category_id               INT NOT NULL AUTO_INCREMENT,
  category_name             VARCHAR(191) NOT NULL,
  image_name                VARCHAR(191),
  category_desc             VARCHAR(191),
  is_leaf_category          TINYINT(1) NOT NULL,
  parent_category_id        INT,
  state                     VARCHAR(191) NOT NULL,
  create_admin_id           BIGINT NOT NULL,
  create_time               INT NOT NULL,
  update_admin_id           BIGINT,
  update_time               INT,
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_competitor_product (
  company_id                BIGINT NOT NULL,
  competitor_product_id     INT NOT NULL AUTO_INCREMENT,
  competitor_product_name   VARCHAR(191) NOT NULL,
  image_name                VARCHAR(191) NOT NULL,
  category_id               INT NOT NULL,
  allow_model_id            INT,
  state                     VARCHAR(191) NOT NULL,
  create_admin_id           BIGINT NOT NULL,
  create_time               INT NOT NULL,
  update_admin_id           BIGINT,
  update_time               INT,
  PRIMARY KEY (competitor_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;
 
CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_recommend_product (
  company_id                BIGINT NOT NULL,
  recommend_product_id      INT NOT NULL AUTO_INCREMENT,
  recommend_product_name    VARCHAR(191) NOT NULL,
  recommend_product_desc    VARCHAR(191) NOT NULL,
  image_name                VARCHAR(191) NOT NULL,
  allow_model_id            INT,
  `web_url.web_url`         VARCHAR(191),
  `web_url.is_weizhu`       TINYINT(1),
  `document.document_url`   VARCHAR(191),
  `document.document_type`  VARCHAR(191),
  `document.document_size`  INT,
  `document.check_md5`      VARCHAR(191),
  `document.is_download`    TINYINT(1),
  `document.is_auth_url`    TINYINT(1),
  `video.video_url`         VARCHAR(191),
  `video.video_type`        VARCHAR(191),
  `video.video_size`        INT,
  `video.video_time`        INT,
  `video.check_md5`         VARCHAR(191),
  `video.is_download`       TINYINT(1),
  `video.is_auth_url`       TINYINT(1),
  `audio.audio_url`         VARCHAR(191),
  `audio.audio_type`        VARCHAR(191),
  `audio.audio_size`        INT,
  `audio.audio_time`        INT,
  `audio.check_md5`         VARCHAR(191),
  `audio.is_download`       TINYINT(1),
  `audio.is_auth_url`       TINYINT(1),
  `app_uri.app_uri`         VARCHAR(191),
  state                     VARCHAR(191) NOT NULL,
  create_admin_id           BIGINT NOT NULL,
  create_time               INT NOT NULL,
  update_admin_id           BIGINT,
  update_time               INT,
  PRIMARY KEY (recommend_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_competitor_recommend_product (
  company_id                BIGINT NOT NULL,
  competitor_product_id     INT NOT NULL,
  recommend_product_id      INT NOT NULL,
  create_admin_id           BIGINT NOT NULL,
  create_time               INT NOT NULL,
  PRIMARY KEY (company_id, competitor_product_id,recommend_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_tool_recommender_price_url (
  company_id            BIGINT NOT NULL,
  url_id                INT NOT NULL AUTO_INCREMENT,
  recommend_product_id  INT NOT NULL,
  url_name              VARCHAR(191) NOT NULL,
  url_content           VARCHAR(191) NOT NULL,
  image_name            VARCHAR(191) NOT NULL,
  is_weizhu             TINYINT(1),
  create_admin_id       BIGINT NOT NULL,
  create_time           INT NOT NULL,
  PRIMARY KEY (url_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

