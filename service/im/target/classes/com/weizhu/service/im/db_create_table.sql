CREATE TABLE IF NOT EXISTS weizhu_im_p2p (
  company_id      BIGINT NOT NULL,
  user_id_most    BIGINT NOT NULL,
  user_id_least   BIGINT NOT NULL,
  latest_msg_seq  BIGINT NOT NULL,
  latest_msg_time INT NOT NULL,
  PRIMARY KEY (company_id, user_id_most, user_id_least)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_im_p2p_msg (
  company_id            BIGINT NOT NULL,
  user_id_most          BIGINT NOT NULL,
  user_id_least         BIGINT NOT NULL,
  msg_seq               BIGINT NOT NULL,
  msg_time              INT NOT NULL,
  from_user_id          BIGINT NOT NULL,
  text_content          TEXT,
  voice_data            BLOB,
  voice_duration        INT,
  image_name            VARCHAR(191),
  user_user_id          BIGINT,
  discover_item_item_id BIGINT,
  video_name            VARCHAR(191),
  video_type            VARCHAR(191),
  video_size            INT,
  video_time            INT,
  video_image_name      VARCHAR(191),
  PRIMARY KEY (company_id, user_id_most, user_id_least, msg_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_im_group (
  company_id      BIGINT NOT NULL,
  group_id        BIGINT NOT NULL AUTO_INCREMENT,
  group_name      VARCHAR(191) NOT NULL,
  latest_msg_seq  BIGINT NOT NULL,
  latest_msg_time INT NOT NULL,
  PRIMARY KEY (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_im_group_member (
  company_id  BIGINT NOT NULL,
  group_id    BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  msg_seq     BIGINT NOT NULL,
  is_join     TINYINT(1) NOT NULL,
  PRIMARY KEY (company_id, group_id, user_id),
  KEY (company_id, user_id, group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_im_group_msg (
  company_id            BIGINT NOT NULL,
  group_id              BIGINT NOT NULL,
  msg_seq               BIGINT NOT NULL,
  msg_time              INT NOT NULL,
  from_user_id          BIGINT NOT NULL,
  text_content          TEXT,
  voice_data            BLOB,
  voice_duration        INT,
  image_name            VARCHAR(191),
  user_user_id          BIGINT,
  group_group_name      VARCHAR(191),
  group_join_user_id    TEXT, -- XXXX,YYYY,ZZZZ
  group_leave_user_id   TEXT, -- XXXX,YYYY,ZZZZ
  discover_item_item_id BIGINT,
  video_name            VARCHAR(191),
  video_type            VARCHAR(191),
  video_size            INT,
  video_time            INT,
  video_image_name      VARCHAR(191),
  PRIMARY KEY (company_id, group_id, msg_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ALTER TABLE weizhu_im_p2p_msg ADD video_name VARCHAR(191), ADD video_type VARCHAR(191), ADD video_size INT, ADD video_time INT, ADD video_image_name VARCHAR(191);
-- ALTER TABLE weizhu_im_group_msg ADD video_name VARCHAR(191), ADD video_type VARCHAR(191), ADD video_size INT, ADD video_time INT, ADD video_image_name VARCHAR(191);