CREATE TABLE IF NOT EXISTS weizhu_discover_banner (
  banner_id    INT NOT NULL AUTO_INCREMENT,
  banner_name  VARCHAR(191) NOT NULL,
  image_name   VARCHAR(191) NOT NULL,
  item_id      BIGINT,
  create_time  INT NOT NULL,
  PRIMARY KEY (banner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_module (
  module_id         INT NOT NULL AUTO_INCREMENT,
  module_name       VARCHAR(191) NOT NULL,
  icon_name         VARCHAR(191) NOT NULL,
  is_recommend      TINYINT(1) NOT NULL,
  next_category_id  INT NOT NULL,
  PRIMARY KEY (module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_module_category (
  module_id      INT NOT NULL,
  category_id    INT NOT NULL,
  category_name  VARCHAR(191) NOT NULL,
  PRIMARY KEY (module_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_module_item_default (
  module_id     INT NOT NULL,
  category_id   INT NOT NULL,
  item_id       BIGINT NOT NULL,
  create_time   INT NOT NULL,
  PRIMARY KEY (module_id, category_id, item_id),
  KEY (module_id, category_id, create_time, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_item (
  item_id              BIGINT NOT NULL AUTO_INCREMENT,
  item_name            VARCHAR(191) NOT NULL,
  icon_name            VARCHAR(191),
  create_time          INT NOT NULL,
  item_desc            VARCHAR(191),
  enable_score         TINYINT(1),
  enable_comment       TINYINT(1),
  redirect_url         VARCHAR(191),
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_item_pv (
  item_id  BIGINT NOT NULL,
  pv       INT NOT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_item_score (
  item_id  BIGINT NOT NULL,
  user_id  BIGINT NOT NULL,
  score    INT NOT NULL,
  PRIMARY KEY (item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_discover_comment (
  comment_id   BIGINT NOT NULL AUTO_INCREMENT,
  comment_time INT NOT NULL,
  user_id      BIGINT NOT NULL,
  content      VARCHAR(191) NOT NULL,
  item_id      BIGINT,
  PRIMARY KEY (comment_id),
  KEY (item_id, comment_time, comment_id),
  KEY (item_id, user_id, comment_time, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;
