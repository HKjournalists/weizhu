CREATE TABLE IF NOT EXISTS weizhu_qa_question_category_tmp(
  company_id              BIGINT NOT NULL,
  category_id             INT NOT NULL AUTO_INCREMENT,
  category_name           VARCHAR(191) NOT NULL,
  user_id                 BIGINT, 
  admin_id                BIGINT,
  create_time             INT NOT NULL, 
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ;

CREATE TABLE IF NOT EXISTS weizhu_qa_question_tmp (
  company_id              BIGINT NOT NULL,
  question_id             INT NOT NULL AUTO_INCREMENT,
  question_content        TEXT,
  user_id                 BIGINT, 
  admin_id                BIGINT,
  category_id             INT NOT NULL, 
  create_time             INT NOT NULL,
  PRIMARY KEY (question_id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin; 

CREATE TABLE IF NOT EXISTS weizhu_qa_answer_tmp (
  company_id               BIGINT NOT NULL,
  answer_id              INT NOT NULL AUTO_INCREMENT,
  question_id            INT NOT NULL,
  user_id                BIGINT, 
  admin_id               BIGINT,
  answer_content         TEXT,
  create_time            INT NOT NULL,
  PRIMARY KEY (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_qa_answer_like_tmp (
  company_id              BIGINT NOT NULL,
  user_id                 BIGINT NOT NULL, 
  answer_id               INT NOT NULL,
  PRIMARY KEY (company_id, user_id, answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

SET @COMPANY_ID=0;

INSERT INTO weizhu_qa_question_category_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_qa_question_category A;
INSERT INTO weizhu_qa_question_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_qa_question A;
INSERT INTO weizhu_qa_answer_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_qa_answer A;
INSERT INTO weizhu_qa_answer_like_tmp SELECT @COMPANY_ID AS company_id, A.* FROM weizhu_qa_answer_like A;

ALTER TABLE weizhu_qa_question_category RENAME TO weizhu_qa_question_category_bak;
ALTER TABLE weizhu_qa_question RENAME TO weizhu_qa_question_bak;
ALTER TABLE weizhu_qa_answer RENAME TO weizhu_qa_answer_bak;
ALTER TABLE weizhu_qa_answer_like RENAME TO weizhu_qa_answer_like_bak;

ALTER TABLE weizhu_qa_question_category_tmp RENAME TO weizhu_qa_question_category;
ALTER TABLE weizhu_qa_question_tmp RENAME TO weizhu_qa_question;
ALTER TABLE weizhu_qa_answer_tmp RENAME TO weizhu_qa_answer;
ALTER TABLE weizhu_qa_answer_like_tmp RENAME TO weizhu_qa_answer_like;