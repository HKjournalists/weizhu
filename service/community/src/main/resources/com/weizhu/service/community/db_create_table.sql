CREATE TABLE IF NOT EXISTS weizhu_community (
  company_id         BIGINT NOT NULL,
  community_name     VARCHAR(191) NOT NULL,
  board_id_order_str TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_board (
  company_id       BIGINT NOT NULL,
  board_id         INT NOT NULL AUTO_INCREMENT,
  board_name       VARCHAR(191) NOT NULL,
  board_icon       VARCHAR(191) NOT NULL,
  board_desc       VARCHAR(191) NOT NULL,
  parent_board_id  INT,
  is_leaf_board    TINYINT(1) NOT NULL,
  is_hot           TINYINT(1) NOT NULL,
  allow_model_id   INT,
  PRIMARY KEY (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post (
  company_id          BIGINT NOT NULL,
  post_id             INT NOT NULL AUTO_INCREMENT,
  post_title          VARCHAR(191) NOT NULL,
  board_id            INT NOT NULL,
  create_user_id      BIGINT NOT NULL,
  create_time         INT NOT NULL,
  is_hot              TINYINT(1) NOT NULL,
  state               VARCHAR(191) NOT NULL,
  comment_id_max      INT NOT NULL,
  is_sticky           TINYINT(1),
  sticky_time         INT,
  is_recommend        TINYINT(1),
  recommend_time      INT,
  PRIMARY KEY (post_id),
  KEY (company_id, board_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_part (
  company_id      BIGINT NOT NULL,
  part_id         INT NOT NULL AUTO_INCREMENT,
  post_id         INT NOT NULL,
  `text`          TEXT,
  image_name      VARCHAR(191),
  PRIMARY KEY (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_comment (
  company_id        BIGINT NOT NULL,
  post_id           INT NOT NULL,
  comment_id        INT NOT NULL,
  reply_comment_id  INT,
  content           TEXT,
  create_user_id    BIGINT NOT NULL,
  create_time       INT NOT NULL,
  state             VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, post_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_like (
  company_id   BIGINT NOT NULL,
  post_id      INT NOT NULL,
  user_id      BIGINT NOT NULL,
  create_time  INT NOT NULL,
  PRIMARY KEY (company_id, post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_hot (
  company_id      BIGINT NOT NULL,
  post_id         INT NOT NULL,
  board_id        INT NOT NULL,
  update_time     INT NOT NULL,
  cur_view_cnt    INT NOT NULL,
  cur_comment_cnt INT NOT NULL,
  cur_like_cnt    INT NOT NULL,
  pre_view_cnt    DOUBLE NOT NULL,
  pre_comment_cnt DOUBLE NOT NULL,
  pre_like_cnt    DOUBLE NOT NULL,
  PRIMARY KEY (company_id, post_id),
  KEY (company_id, board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_comment_like (
  company_id   BIGINT NOT NULL,
  post_id      INT NOT NULL,
  comment_id   INT NOT NULL,
  user_id      BIGINT NOT NULL,
  create_time  INT NOT NULL,
  PRIMARY KEY (company_id, post_id, comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_board_tag (
  company_id          BIGINT NOT NULL,
  board_id            INT NOT NULL,
  tag                 VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id,board_id,tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_tag (
  company_id          BIGINT NOT NULL,
  post_id             INT NOT NULL,
  tag                 VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id,post_id,tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;