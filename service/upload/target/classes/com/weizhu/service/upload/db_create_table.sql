CREATE TABLE IF NOT EXISTS weizhu_upload_image (
  name    VARCHAR(191) NOT NULL,
  `type`  VARCHAR(191) NOT NULL,
  size    INT NOT NULL,
  md5     VARCHAR(191) NOT NULL,
  width   INT NOT NULL,
  hight   INT NOT NULL,
  PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_upload_image_action (
  company_id       BIGINT NOT NULL,
  action_id        BIGINT NOT NULL AUTO_INCREMENT,
  image_name       VARCHAR(191) NOT NULL,
  upload_time      INT NOT NULL,
  upload_admin_id  BIGINT,
  upload_user_id   BIGINT,
  PRIMARY KEY (action_id),
  KEY (company_id, action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_upload_image_tag (
  company_id  VARCHAR(191) NOT NULL,
  image_name  VARCHAR(191) NOT NULL,
  tag         VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id, image_name, tag),
  KEY (company_id, tag, image_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;