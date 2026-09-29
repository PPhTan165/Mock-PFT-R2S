CREATE TABLE roles (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL,
                       PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category_icons (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                category_name VARCHAR(100) DEFAULT NULL,
                                emoji VARCHAR(10) DEFAULT NULL,
                                icon_url VARCHAR(255) DEFAULT NULL,
                                created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (id),
                                UNIQUE KEY category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       full_name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       avatar VARCHAR(512) DEFAULT NULL,
                       created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                       failed_login_attempts INT NOT NULL DEFAULT 0,
                       locked_until DATETIME DEFAULT NULL,
                       two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0,

                       PRIMARY KEY (id),
                       UNIQUE KEY email (email)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            category_icon_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            type ENUM('INCOME', 'EXPENSE') NOT NULL,
                            created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

                            PRIMARY KEY (id),

                            KEY fk_categories_category_icons (category_icon_id),
                            KEY fk_categories_users (user_id),

                            CONSTRAINT fk_categories_category_icons
                                FOREIGN KEY (category_icon_id)
                                    REFERENCES category_icons (id),

                            CONSTRAINT fk_categories_users
                                FOREIGN KEY (user_id)
                                    REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE budgets (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         amount DECIMAL(12, 2) NOT NULL,
                         category_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         month TINYINT NOT NULL,
                         year SMALLINT NOT NULL,
                         created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

                         PRIMARY KEY (id),

                         KEY fk_budgets_categories (category_id),
                         KEY fk_budgets_users (user_id),

                         CONSTRAINT fk_budgets_categories
                             FOREIGN KEY (category_id)
                                 REFERENCES categories (id),

                         CONSTRAINT fk_budgets_users
                             FOREIGN KEY (user_id)
                                 REFERENCES users (id),

                         CONSTRAINT chk_budgets_amount
                             CHECK (amount >= 0),

                         CONSTRAINT chk_budgets_month
                             CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_budgets_year
        CHECK (year >= 2000)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE transactions (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              amount DECIMAL(12, 2) NOT NULL,
                              note VARCHAR(255) DEFAULT NULL,
                              category_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              date DATE NOT NULL,
                              created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

                              PRIMARY KEY (id),

                              KEY fk_transactions_users (user_id),
                              KEY fk_transactions_categories (category_id),

                              CONSTRAINT fk_transactions_categories
                                  FOREIGN KEY (category_id)
                                      REFERENCES categories (id),

                              CONSTRAINT fk_transactions_users
                                  FOREIGN KEY (user_id)
                                      REFERENCES users (id),

                              CONSTRAINT chk_transactions_amount
                                  CHECK (amount >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            KEY fk_user_roles_roles (role_id),

                            CONSTRAINT fk_user_roles_roles
                                FOREIGN KEY (role_id)
                                    REFERENCES roles (id),

                            CONSTRAINT fk_user_roles_users
                                FOREIGN KEY (user_id)
                                    REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_settings (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       user_id BIGINT NOT NULL,
                                       daily_reminder TINYINT(1) DEFAULT 0,
                                       tip_enabled TINYINT(1) DEFAULT 0,
                                       budget_alert TINYINT(1) DEFAULT 1,
                                       created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

                                       PRIMARY KEY (id),

                                       KEY fk_notification_settings_users (user_id),

                                       CONSTRAINT fk_notification_settings_users
                                           FOREIGN KEY (user_id)
                                               REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE two_factor_challenges (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       user_id BIGINT NOT NULL,
                                       challenge_id VARCHAR(255) NOT NULL,
                                       otp_code VARCHAR(20) NOT NULL,
                                       expires_at DATETIME NOT NULL,
                                       attempt_count INT NOT NULL DEFAULT 0,
                                       last_sent_at DATETIME NOT NULL,
                                       used TINYINT(1) NOT NULL DEFAULT 0,
                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       PRIMARY KEY (id),

                                       UNIQUE KEY challenge_id (challenge_id),

                                       KEY fk_two_factor_user (user_id),

                                       CONSTRAINT fk_two_factor_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;