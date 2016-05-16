CREATE TABLE IF NOT EXISTS weizhu_exam_question (
  company_id               BIGINT NOT NULL,
  question_id              INT NOT NULL AUTO_INCREMENT,
  question_name            VARCHAR(191) NOT NULL,
  `type`                   VARCHAR(191) NOT NULL,
  create_question_admin_id BIGINT,
  create_question_time     INT,
  PRIMARY KEY              (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_option (
  company_id   BIGINT NOT NULL,
  option_id    INT NOT NULL AUTO_INCREMENT,
  question_id  INT NOT NULL,
  option_name  VARCHAR(191) NOT NULL,
  is_right     TINYINT(1) NOT NULL,
  PRIMARY KEY  (option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_exam (
  company_id             BIGINT NOT NULL,
  exam_id                INT NOT NULL AUTO_INCREMENT,
  exam_name              VARCHAR(191) NOT NULL,  
  image_name             VARCHAR(191),
  start_time             INT NOT NULL,
  end_time               INT NOT NULL,
  question_order_str     TEXT,
  is_submit_execute      TINYINT(1),
  create_exam_admin_id   BIGINT,
  create_exam_time       INT,
  pass_mark              INT,
  allow_model_id         INT,
  show_result            VARCHAR(191) NOT NULL,
  `type`                 VARCHAR(191) NOT NULL,
  is_load_all_user       TINYINT(1),
  PRIMARY KEY (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_exam_question (
  company_id            BIGINT NOT NULL,
  exam_id               INT NOT NULL,
  question_id           INT NOT NULL,
  score                 INT NOT NULL,
  PRIMARY KEY (company_id, exam_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_user_answer (
  company_id        BIGINT NOT NULL,
  user_id           BIGINT NOT NULL, 
  exam_id           INT NOT NULL,
  question_id       INT NOT NULL,
  answer_option_id  INT NOT NULL,
  answer_time       INT,
  PRIMARY KEY (company_id, user_id, exam_id, question_id, answer_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_user_result (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  exam_id      INT NOT NULL,
  start_time   INT,
  submit_time  INT,
  score        INT,
  team_id_1    INT,
  team_id_2    INT,
  team_id_3    INT,
  team_id_4    INT,
  team_id_5    INT,
  team_id_6    INT,
  team_id_7    INT,
  team_id_8    INT,
  position_id  INT,
  level_id     INT,
  PRIMARY KEY (company_id, user_id, exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_question_category (
  company_id         BIGINT NOT NULL,
  category_id        INT NOT NULL AUTO_INCREMENT,
  category_name      VARCHAR(191) NOT NULL,
  parent_category_id INT,
  create_admin_id    BIGINT NOT NULL,
  create_time        INT,
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_question_category_join_question (
  company_id  BIGINT NOT NULL,
  category_id INT NOT NULL,
  question_id INT NOT NULL,
  PRIMARY KEY (company_id, category_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_exam_join_category (
  company_id BIGINT NOT NULL,
  exam_id    INT    NOT NULL,
  question_category_str TEXT,
  question_num INT,
  PRIMARY KEY (company_id, exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_exam_user_question (
  company_id  BIGINT NOT NULL,
  exam_id     INT NOT NULL,
  user_id     BIGINT NOT NULL,
  question_id INT NOT NULL,
  score       INT NOT NULL,
  PRIMARY KEY (company_id, exam_id, user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;