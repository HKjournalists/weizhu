CREATE TABLE IF NOT EXISTS weizhu_component_score (
  company_id       BIGINT NOT NULL,
  score_id         INT NOT NULL AUTO_INCREMENT,
  score_name       VARCHAR(191) NOT NULL,
  image_name       VARCHAR(191),
  `type`           VARCHAR(191),
  result_view      VARCHAR(191),
  start_time       INT,
  end_time         INT,
  allow_model_id   INT,
  state            VARCHAR(191),
  create_admin_id  BIGINT,
  create_time      INT,
  update_admin_id  BIGINT,
  update_time      INT,
  PRIMARY KEY (score_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_component_score_user (
  company_id   BIGINT NOT NULL,
  score_id     INT NOT NULL,
  user_id      BIGINT NOT NULL,
  score_value  INT NOT NULL,
  score_time   INT NOT NULL,
  PRIMARY KEY (company_id, score_id, user_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
