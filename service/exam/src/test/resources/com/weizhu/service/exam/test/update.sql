SET @COMPANY_ID=14;

CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_question (
  company_id               BIGINT NOT NULL,
  question_id              INT NOT NULL AUTO_INCREMENT,
  question_name            VARCHAR(191) NOT NULL,
  type                     VARCHAR(191) NOT NULL,
  create_question_admin_id BIGINT,
  create_question_time     INT,
  PRIMARY KEY              (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_option (
  company_id   BIGINT NOT NULL,
  option_id    INT NOT NULL AUTO_INCREMENT,
  question_id  INT NOT NULL,
  option_name  VARCHAR(191) NOT NULL,
  is_right     TINYINT(1) NOT NULL,
  PRIMARY KEY  (option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_exam (
  company_id           BIGINT NOT NULL,
  exam_id              INT NOT NULL AUTO_INCREMENT,
  exam_name            VARCHAR(191) NOT NULL,  
  start_time           INT NOT NULL,
  end_time             INT NOT NULL,
  question_order_str   TEXT,
  is_submit_execute    TINYINT(1),
  create_exam_admin_id BIGINT,
  create_exam_time     INT,
  pass_mark            INT,
  allow_model_id       INT,
  PRIMARY KEY (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_exam_question (
  company_id   BIGINT NOT NULL,
  exam_id      INT NOT NULL,
  question_id  INT NOT NULL,
  score        INT NOT NULL,
  PRIMARY KEY (company_id, exam_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_user_answer (
  company_id        BIGINT NOT NULL,
  user_id           BIGINT NOT NULL, 
  exam_id           INT NOT NULL,
  question_id       INT NOT NULL,
  answer_option_id  INT NOT NULL,
  answer_time       INT,
  PRIMARY KEY (company_id, user_id, exam_id, question_id, answer_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_user_result (
  company_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  exam_id      INT NOT NULL,
  start_time   INT NOT NULL,
  submit_time  INT,
  score        INT,
  PRIMARY KEY (company_id, user_id, exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_question_category (
  company_id         BIGINT NOT NULL,
  category_id        INT NOT NULL AUTO_INCREMENT,
  category_name      VARCHAR(191) NOT NULL,
  parent_category_id INT,
  create_admin_id    BIGINT NOT NULL,
  create_time        INT,
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


CREATE TABLE IF NOT EXISTS tmp_weizhu_exam_question_category_join_question (
  company_id  BIGINT NOT NULL,
  category_id INT NOT NULL,
  question_id INT NOT NULL,
  PRIMARY KEY (company_id, category_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;


INSERT INTO tmp_weizhu_exam_question
  (company_id, question_id, question_name, type, create_question_admin_id, create_question_time) 
SELECT @COMPANY_ID, question_id, question_name, type, create_question_admin_id, create_question_time FROM weizhu_exam_question;


INSERT INTO tmp_weizhu_exam_option
  (company_id, option_id, question_id, option_name, is_right) 
SELECT @COMPANY_ID, option_id, question_id, option_name, is_right FROM weizhu_exam_option;


INSERT INTO tmp_weizhu_exam_exam
  (company_id, exam_id, exam_name, start_time, end_time, question_order_str, is_submit_execute, create_exam_admin_id, create_exam_time, pass_mark, allow_model_id) 
SELECT @COMPANY_ID, exam_id, exam_name, start_time, end_time, question_order_str, is_submit_execute, create_exam_admin_id, create_exam_time, pass_mark, allow_model_id FROM weizhu_exam_exam;


INSERT INTO tmp_weizhu_exam_exam_question
  (company_id, exam_id, question_id, score) 
SELECT @COMPANY_ID, exam_id, question_id, score FROM weizhu_exam_exam_question;


INSERT INTO tmp_weizhu_exam_user_answer
  (company_id, user_id, exam_id, question_id, answer_option_id, answer_time) 
SELECT @COMPANY_ID, user_id, exam_id, question_id, answer_option_id, answer_time FROM weizhu_exam_user_answer;


INSERT INTO tmp_weizhu_exam_user_result
  (company_id, user_id, exam_id, start_time, submit_time, score) 
SELECT @COMPANY_ID, user_id, exam_id, start_time, submit_time, score FROM weizhu_exam_user_result;


INSERT INTO tmp_weizhu_exam_question_category
  (company_id, category_id, category_name, parent_category_id, create_admin_id, create_time) 
SELECT @COMPANY_ID, category_id, category_name, parent_category_id, create_admin_id, create_time FROM weizhu_exam_question_category;


INSERT INTO tmp_weizhu_exam_question_category_join_question
  (company_id, category_id, question_id) 
SELECT @COMPANY_ID, category_id, question_id FROM weizhu_exam_question_category_join_question;


RENAME TABLE weizhu_exam_question TO bak_weizhu_exam_question;
RENAME TABLE tmp_weizhu_exam_question TO weizhu_exam_question;

RENAME TABLE weizhu_exam_option TO bak_weizhu_exam_option;
RENAME TABLE tmp_weizhu_exam_option TO weizhu_exam_option;

RENAME TABLE weizhu_exam_exam TO bak_weizhu_exam_exam;
RENAME TABLE tmp_weizhu_exam_exam TO weizhu_exam_exam;

RENAME TABLE weizhu_exam_exam_join_user TO bak_weizhu_exam_exam_join_user;

RENAME TABLE weizhu_exam_exam_join_team TO bak_weizhu_exam_exam_join_team;

RENAME TABLE weizhu_exam_exam_question TO bak_weizhu_exam_exam_question;
RENAME TABLE tmp_weizhu_exam_exam_question TO weizhu_exam_exam_question;

RENAME TABLE weizhu_exam_user_answer TO bak_weizhu_exam_user_answer;
RENAME TABLE tmp_weizhu_exam_user_answer TO weizhu_exam_user_answer;

RENAME TABLE weizhu_exam_user_result TO bak_weizhu_exam_user_result;
RENAME TABLE tmp_weizhu_exam_user_result TO weizhu_exam_user_result;

RENAME TABLE weizhu_exam_question_category TO bak_weizhu_exam_question_category;
RENAME TABLE tmp_weizhu_exam_question_category TO weizhu_exam_question_category;

RENAME TABLE weizhu_exam_question_category_join_question TO bak_weizhu_exam_question_category_join_question;
RENAME TABLE tmp_weizhu_exam_question_category_join_question TO weizhu_exam_question_category_join_question;

SELECT distinct(company_id) FROM weizhu_exam_question;
SELECT distinct(company_id) FROM weizhu_exam_option;
SELECT distinct(company_id) FROM weizhu_exam_exam;
SELECT distinct(company_id) FROM weizhu_exam_exam_question;
SELECT distinct(company_id) FROM weizhu_exam_user_answer;
SELECT distinct(company_id) FROM weizhu_exam_user_result;
SELECT distinct(company_id) FROM weizhu_exam_question_category;
SELECT distinct(company_id) FROM weizhu_exam_question_category_join_question;