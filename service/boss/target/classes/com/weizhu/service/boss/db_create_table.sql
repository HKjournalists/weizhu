CREATE TABLE IF NOT EXISTS weizhu_boss (
  boss_id        VARCHAR(191) NOT NULL,
  boss_password  VARCHAR(191) NOT NULL,
  PRIMARY KEY (boss_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_boss_session (
  boss_id      VARCHAR(191) NOT NULL,
  session_id   BIGINT NOT NULL,
  login_time   INT NOT NULL,
  login_host   VARCHAR(191) NOT NULL,
  user_agent   TEXT NOT NULL,
  active_time  INT NOT NULL,      
  logout_time  INT,
  PRIMARY KEY (boss_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
