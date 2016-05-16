CREATE TABLE IF NOT EXISTS weizhu_home_user_info (
  user_info_id INT NOT NULL AUTO_INCREMENT,
  user_name    VARCHAR(191) NOT NULL,
  position     VARCHAR(191),
  email        VARCHAR(191),
  phone        VARCHAR(191) NOT NULL,
  company      VARCHAR(191) NOT NULL,
  province     VARCHAR(191),
  city         VARCHAR(191),            
  remark       VARCHAR(191),
  PRIMARY KEY (user_info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;