CREATE TABLE IF NOT EXISTS weizhu_survey (
  company_id         BIGINT NOT NULL,
  survey_id          INT NOT NULL AUTO_INCREMENT,
  survey_name        VARCHAR(191) NOT NULL, -- 调研名称
  survey_desc        TEXT NOT NULL,         -- 调研描述
  image_name         VARCHAR(191) NOT NULL, -- 调研图标
  start_time         INT NOT NULL,          -- 开始时间
  end_time           INT NOT NULL,          -- 结束时间
  show_result_type   VARCHAR(191) NOT NULL, -- 客户端展示消息结果（0：客户端不展示调研结果，1：提交之后显示统计结果，2：提交之后显示详细信息，3；总是显示统计结果，4：总是显示详细信息）
  allow_model_id     INT,                   -- 访问模型
  question_order_str VARCHAR(191),          -- 问题排序序号
  state              VARCHAR(191) NOT NULL, -- 状态
  create_time        INT,                   -- 创建时间
  create_admin_id    BIGINT,                -- 创建管理员id
  update_time        INT,                   -- 更新时间
  update_admin_id    BIGINT,                -- 更新管理员id
  PRIMARY KEY (survey_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_join_question (
  company_id         BIGINT NOT NULL,
  survey_id          INT NOT NULL,          -- 调研id
  question_id        INT NOT NULL,          -- 题目id
  PRIMARY KEY (company_id, survey_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_question (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL AUTO_INCREMENT,
  question_name      VARCHAR(191) NOT NULL, -- 问题
  image_name         VARCHAR(191),          -- 图片
  is_optional        TINYINT(1) NOT NULL,   -- 是否选做
  type               VARCHAR(191) NOT NULL, -- 类型（投票，多选，单选，主观，下拉框）
  state              VARCHAR(191) NOT NULL, -- 问题状态
  create_time        INT,                   -- 创建时间
  create_admin_id    BIGINT,                -- 创建管理员id
  update_time        INT,                   -- 更新时间
  update_admin_id    BIGINT,                -- 更新管理员id
  PRIMARY KEY (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_result (
  company_id         BIGINT NOT NULL,
  survey_id          INT NOT NULL,
  user_id            BIGINT NOT NULL,
  submit_time        INT NOT NULL,  
  PRIMARY KEY (survey_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_vote_question (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL,
  check_number       INT NOT NULL,          -- 最多可选选项数
  PRIMARY KEY (company_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_vote_option (
  company_id         BIGINT NOT NULL,
  option_id          INT NOT NULL AUTO_INCREMENT,
  option_name        VARCHAR(191) NOT NULL, -- 选项名称
  question_id        INT NOT NULL,          -- 问题id
  image_name         VARCHAR(191),          -- 图片名称
  PRIMARY KEY (option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_vote_answer (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL,          -- 问题id
  user_id            BIGINT NOT NULL,       -- 用户id
  option_id          INT NOT NULL,
  answer_time        INT,
  PRIMARY KEY (company_id, question_id, user_id, option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_input_text_question (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL AUTO_INCREMENT,
  input_prompt       VARCHAR(191),          -- 提示信息
  PRIMARY KEY (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_input_text_answer (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL,          -- 问题id
  user_id            BIGINT NOT NULL,       -- 用户id
  result_text        VARCHAR(191) NOT NULL, -- 问题结果
  answer_time        INT,
  PRIMARY KEY (company_id, question_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_input_select_option (
  company_id         BIGINT NOT NULL,
  option_id          INT NOT NULL AUTO_INCREMENT,
  option_name        VARCHAR(191) NOT NULL, -- 选项名称
  question_id        INT NOT NULL,          -- 问题id
  PRIMARY KEY (option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_survey_input_select_answer (
  company_id         BIGINT NOT NULL,
  question_id        INT NOT NULL,          -- 问题id
  user_id            BIGINT NOT NULL,       -- 用户id
  option_id          INT NOT NULL,
  answer_time        INT,
  PRIMARY KEY (company_id, question_id, user_id, option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_bin;

