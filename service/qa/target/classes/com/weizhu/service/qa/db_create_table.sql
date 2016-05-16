CREATE TABLE IF NOT EXISTS weizhu_qa_question_category(
  company_id              BIGINT NOT NULL,
  category_id             INT NOT NULL AUTO_INCREMENT,
  category_name           VARCHAR(191) NOT NULL,
  user_id                 BIGINT, 
  admin_id                BIGINT,
  create_time             INT NOT NULL, 
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ;

CREATE TABLE IF NOT EXISTS weizhu_qa_question (
  company_id              BIGINT NOT NULL,
  question_id             INT NOT NULL AUTO_INCREMENT,
  question_content        TEXT,
  user_id                 BIGINT, 
  admin_id                BIGINT,
  category_id             INT NOT NULL, 
  create_time             INT NOT NULL,
  PRIMARY KEY (question_id) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin; 

CREATE TABLE IF NOT EXISTS weizhu_qa_answer (
  company_id               BIGINT NOT NULL,
  answer_id              INT NOT NULL AUTO_INCREMENT,
  question_id            INT NOT NULL,
  user_id                BIGINT, 
  admin_id               BIGINT,
  answer_content         TEXT,
  create_time            INT NOT NULL,
  PRIMARY KEY (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_qa_answer_like (
  company_id              BIGINT NOT NULL,
  user_id                 BIGINT NOT NULL, 
  answer_id               INT NOT NULL,
  PRIMARY KEY (company_id, user_id, answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
