CREATE TABLE IF NOT EXISTS weizhu_community_tmp (
  company_id         BIGINT NOT NULL,
  community_name     VARCHAR(191) NOT NULL,
  board_id_order_str TEXT,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_board_tmp (
  company_id       BIGINT NOT NULL,
  board_id         INT NOT NULL AUTO_INCREMENT,
  board_name       VARCHAR(191) NOT NULL,
  board_icon       VARCHAR(191) NOT NULL,
  board_desc       VARCHAR(191) NOT NULL,
  parent_board_id  INT,
  is_leaf_board    TINYINT(1) NOT NULL,
  is_hot           TINYINT(1) NOT NULL,
  PRIMARY KEY (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_community_post_part_tmp (
  company_id      BIGINT NOT NULL,
  part_id         INT NOT NULL AUTO_INCREMENT,
  post_id         INT NOT NULL,
  `text`          TEXT,
  image_name      VARCHAR(191),
  PRIMARY KEY (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_comment_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_community_post_like_tmp (
  company_id   BIGINT NOT NULL,
  post_id      INT NOT NULL,
  user_id      BIGINT NOT NULL,
  create_time  INT NOT NULL,
  PRIMARY KEY (company_id, post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_community_post_hot_tmp (
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

CREATE TABLE IF NOT EXISTS weizhu_community_comment_like_tmp (
  company_id   BIGINT NOT NULL,
  post_id      INT NOT NULL,
  comment_id   INT NOT NULL,
  user_id      BIGINT NOT NULL,
  create_time  INT NOT NULL,
  PRIMARY KEY (company_id, post_id, comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

SET @COMPANY_ID=0;

INSERT INTO weizhu_community_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community A;
INSERT INTO weizhu_community_board_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_board A;
INSERT INTO weizhu_community_post_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_post A;
INSERT INTO weizhu_community_post_part_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_post_part A;
INSERT INTO weizhu_community_post_comment_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_post_comment A;
INSERT INTO weizhu_community_post_like_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_post_like A;
INSERT INTO weizhu_community_post_hot_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_post_hot A;
INSERT INTO weizhu_community_comment_like_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_community_comment_like A;

ALTER TABLE weizhu_community RENAME TO weizhu_community_bak;
ALTER TABLE weizhu_community_board RENAME TO weizhu_community_board_bak;
ALTER TABLE weizhu_community_post RENAME TO weizhu_community_post_bak;
ALTER TABLE weizhu_community_post_part RENAME TO weizhu_community_post_part_bak;
ALTER TABLE weizhu_community_post_comment RENAME TO weizhu_community_post_comment_bak;
ALTER TABLE weizhu_community_post_like RENAME TO weizhu_community_post_like_bak;
ALTER TABLE weizhu_community_post_hot RENAME TO weizhu_community_post_hot_bak;
ALTER TABLE weizhu_community_comment_like RENAME TO weizhu_community_comment_like_bak;

ALTER TABLE weizhu_community_tmp RENAME TO weizhu_community;
ALTER TABLE weizhu_community_board_tmp RENAME TO weizhu_community_board;
ALTER TABLE weizhu_community_post_tmp RENAME TO weizhu_community_post;
ALTER TABLE weizhu_community_post_part_tmp RENAME TO weizhu_community_post_part;
ALTER TABLE weizhu_community_post_comment_tmp RENAME TO weizhu_community_post_comment;
ALTER TABLE weizhu_community_post_like_tmp RENAME TO weizhu_community_post_like;
ALTER TABLE weizhu_community_post_hot_tmp RENAME TO weizhu_community_post_hot;
ALTER TABLE weizhu_community_comment_like_tmp RENAME TO weizhu_community_comment_like;

SELECT DISTINCT company_id FROM weizhu_community;
SELECT DISTINCT company_id FROM weizhu_community_board;
SELECT DISTINCT company_id FROM weizhu_community_post;
SELECT DISTINCT company_id FROM weizhu_community_post_part;
SELECT DISTINCT company_id FROM weizhu_community_post_comment;
SELECT DISTINCT company_id FROM weizhu_community_post_like;
SELECT DISTINCT company_id FROM weizhu_community_post_hot;
SELECT DISTINCT company_id FROM weizhu_community_comment_like;
