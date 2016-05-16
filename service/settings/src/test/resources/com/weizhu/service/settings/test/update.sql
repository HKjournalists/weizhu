CREATE TABLE IF NOT EXISTS weizhu_settings_tmp (
  company_id                BIGINT NOT NULL,
  user_id                   BIGINT NOT NULL,
  do_not_disturb_enable     TINYINT(1),
  do_not_disturb_begin_time INT,
  do_not_disturb_end_time   INT,
  PRIMARY KEY (company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

SET @COMPANY_ID=0;

INSERT INTO weizhu_settings_tmp 
   (company_id, user_id, do_not_disturb_enable, do_not_disturb_begin_time, do_not_disturb_end_time) 
SELECT @COMPANY_ID AS company_id, user_id, do_not_disturb_enable, do_not_disturb_begin_time, do_not_disturb_end_time FROM weizhu_settings;

ALTER TABLE weizhu_settings RENAME TO weizhu_settings_bak;

ALTER TABLE weizhu_settings_tmp RENAME TO weizhu_settings;

SELECT DISTINCT company_id FROM weizhu_settings;