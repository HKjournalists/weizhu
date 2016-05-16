CREATE TABLE IF NOT EXISTS weizhu_company (
  company_id    BIGINT NOT NULL,
  company_name  VARCHAR(191) NOT NULL,
  server_name   VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_company_key (
  company_id   BIGINT NOT NULL,
  company_key  VARCHAR(191) NOT NULL,
  PRIMARY KEY (company_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS weizhu_company_server_address (
  server_name VARCHAR(191) NOT NULL,
  host        VARCHAR(191) NOT NULL,
  port        INT NOT NULL,
  PRIMARY KEY (host, port)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;