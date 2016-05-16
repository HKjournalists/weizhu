SET @COMPANY_ID=2;

CREATE TABLE IF NOT EXISTS tmp_weizhu_im_p2p (
  company_id      BIGINT NOT NULL,
  user_id_most    BIGINT NOT NULL,
  user_id_least   BIGINT NOT NULL,
  latest_msg_seq  BIGINT NOT NULL,
  latest_msg_time INT NOT NULL,
  PRIMARY KEY (company_id, user_id_most, user_id_least)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tmp_weizhu_im_p2p_msg (
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
  PRIMARY KEY (company_id, user_id_most, user_id_least, msg_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tmp_weizhu_im_group (
  company_id      BIGINT NOT NULL,
  group_id        BIGINT NOT NULL AUTO_INCREMENT,
  group_name      VARCHAR(191) NOT NULL,
  latest_msg_seq  BIGINT NOT NULL,
  latest_msg_time INT NOT NULL,
  PRIMARY KEY (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tmp_weizhu_im_group_member (
  company_id  BIGINT NOT NULL,
  group_id    BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  msg_seq     BIGINT NOT NULL,
  is_join     TINYINT(1) NOT NULL,
  PRIMARY KEY (company_id, group_id, user_id),
  KEY (company_id, user_id, group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS tmp_weizhu_im_group_msg (
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
  PRIMARY KEY (company_id, group_id, msg_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


INSERT INTO tmp_weizhu_im_p2p 
  (company_id, user_id_most, user_id_least, latest_msg_seq, latest_msg_time) 
SELECT @COMPANY_ID, user_id_most, user_id_least, latest_msg_seq, latest_msg_time FROM weizhu_im_p2p;

INSERT INTO tmp_weizhu_im_p2p_msg 
  (company_id, user_id_most, user_id_least, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, discover_item_item_id) 
SELECT @COMPANY_ID, user_id_most, user_id_least, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, discover_item_item_id FROM weizhu_im_p2p_msg;

INSERT INTO tmp_weizhu_im_group 
  (company_id, group_id, group_name, latest_msg_seq, latest_msg_time) 
SELECT @COMPANY_ID, group_id, group_name, latest_msg_seq, latest_msg_time FROM weizhu_im_group;

INSERT INTO tmp_weizhu_im_group_member 
  (company_id, group_id, user_id, msg_seq, is_join) 
SELECT @COMPANY_ID, group_id, user_id, msg_seq, is_join FROM weizhu_im_group_member;

INSERT INTO tmp_weizhu_im_group_msg 
  (company_id, group_id, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, group_group_name, group_join_user_id, group_leave_user_id, discover_item_item_id) 
SELECT @COMPANY_ID, group_id, msg_seq, msg_time, from_user_id, text_content, voice_data, voice_duration, image_name, user_user_id, group_group_name, group_join_user_id, group_leave_user_id, discover_item_item_id FROM weizhu_im_group_msg;


RENAME TABLE weizhu_im_p2p TO bak_weizhu_im_p2p;
RENAME TABLE tmp_weizhu_im_p2p TO weizhu_im_p2p;

RENAME TABLE weizhu_im_p2p_msg TO bak_weizhu_im_p2p_msg;
RENAME TABLE tmp_weizhu_im_p2p_msg TO weizhu_im_p2p_msg;

RENAME TABLE weizhu_im_group TO bak_weizhu_im_group;
RENAME TABLE tmp_weizhu_im_group TO weizhu_im_group;

RENAME TABLE weizhu_im_group_member TO bak_weizhu_im_group_member;
RENAME TABLE tmp_weizhu_im_group_member TO weizhu_im_group_member;

RENAME TABLE weizhu_im_group_msg TO bak_weizhu_im_group_msg;
RENAME TABLE tmp_weizhu_im_group_msg TO weizhu_im_group_msg;

SELECT DISTINCT company_id FROM weizhu_im_p2p;
SELECT DISTINCT company_id FROM weizhu_im_p2p_msg;
SELECT DISTINCT company_id FROM weizhu_im_group;
SELECT DISTINCT company_id FROM weizhu_im_group_member;
SELECT DISTINCT company_id FROM weizhu_im_group_msg;
