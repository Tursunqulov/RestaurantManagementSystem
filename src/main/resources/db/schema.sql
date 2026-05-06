-- ============================================================
-- Restaurant Management System — Database Schema
-- Target  : MySQL 8.x / MariaDB (XAMPP or standalone)
-- Usage   : Run this entire script in phpMyAdmin or MySQL CLI
-- ============================================================

CREATE DATABASE IF NOT EXISTS restaurant_ms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE restaurant_ms;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  1.  USERS  (login + role-based routing)                    ║
-- ║      Roles: MANAGER, RECEPTIONIST, WAITER, CASHIER, CHEF    ║
-- ║      MANAGER is the top-level role — no separate ADMIN.     ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150),
    email       VARCHAR(200),
    phone       VARCHAR(30),
    role        ENUM('MANAGER','RECEPTIONIST','WAITER','CASHIER','CHEF') NOT NULL,
    branch_id   INT,                          -- NULL = system-wide manager
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  2.  RESTAURANT & BRANCHES                                  ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE restaurant (
    restaurant_id INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE branch (
    branch_id       INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id   INT          NOT NULL,
    name            VARCHAR(150) NOT NULL,
    street_address  VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(100),
    zipcode         VARCHAR(20),
    country         VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurant(restaurant_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Now add the FK from users → branch (deferred because branch didn't exist yet)
ALTER TABLE users
    ADD CONSTRAINT fk_users_branch
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id)
    ON DELETE SET NULL;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  3.  MENU  →  MENU SECTIONS  →  MENU ITEMS                 ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE menu (
    menu_id     INT AUTO_INCREMENT PRIMARY KEY,
    branch_id   INT          NOT NULL,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE menu_section (
    section_id  INT AUTO_INCREMENT PRIMARY KEY,
    menu_id     INT          NOT NULL,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    FOREIGN KEY (menu_id) REFERENCES menu(menu_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE menu_item (
    item_id     INT AUTO_INCREMENT PRIMARY KEY,
    section_id  INT            NOT NULL,
    title       VARCHAR(200)   NOT NULL,
    description TEXT,
    price       DECIMAL(12,2)  NOT NULL,
    is_available BOOLEAN       NOT NULL DEFAULT TRUE,
    FOREIGN KEY (section_id) REFERENCES menu_section(section_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  4.  TABLES  &  TABLE SEATS                                 ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE restaurant_table (
    table_id            INT AUTO_INCREMENT PRIMARY KEY,
    branch_id           INT NOT NULL,
    table_number        VARCHAR(30)  NOT NULL,
    max_capacity        INT          NOT NULL,
    location_identifier VARCHAR(60),
    status              ENUM('FREE','RESERVED','OCCUPIED','OTHER')
                            NOT NULL DEFAULT 'FREE',
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE table_seat (
    seat_id     INT AUTO_INCREMENT PRIMARY KEY,
    table_id    INT NOT NULL,
    seat_number INT NOT NULL,
    seat_type   ENUM('REGULAR','KID','ACCESSIBLE','OTHER')
                    NOT NULL DEFAULT 'REGULAR',
    FOREIGN KEY (table_id) REFERENCES restaurant_table(table_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  5.  CUSTOMERS  (registered by Receptionist)                ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE customer (
    customer_id  INT AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(200),
    phone        VARCHAR(30),
    last_visited DATE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  6.  RESERVATIONS                                           ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE reservation (
    reservation_id      INT AUTO_INCREMENT PRIMARY KEY,
    table_id            INT      NOT NULL,
    customer_id         INT,
    reserved_by_user_id INT,                       -- the receptionist
    time_of_reservation DATETIME NOT NULL,
    people_count        INT      NOT NULL,
    status              ENUM('REQUESTED','PENDING','CONFIRMED',
                             'CHECKED_IN','CANCELED','ABANDONED')
                             NOT NULL DEFAULT 'CONFIRMED',
    notes               TEXT,
    checkin_time        DATETIME,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id)            REFERENCES restaurant_table(table_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id)         REFERENCES customer(customer_id)      ON DELETE SET NULL,
    FOREIGN KEY (reserved_by_user_id) REFERENCES users(user_id)             ON DELETE SET NULL
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  7.  ORDERS  →  MEALS  →  MEAL ITEMS                       ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE `order` (
    order_id    INT AUTO_INCREMENT PRIMARY KEY,
    table_id    INT NOT NULL,
    waiter_id   INT,
    status      ENUM('RECEIVED','PREPARING','COMPLETE','CANCELED')
                    NOT NULL DEFAULT 'RECEIVED',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id)  REFERENCES restaurant_table(table_id) ON DELETE CASCADE,
    FOREIGN KEY (waiter_id) REFERENCES users(user_id)              ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE meal (
    meal_id   INT AUTO_INCREMENT PRIMARY KEY,
    order_id  INT NOT NULL,
    seat_id   INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES `order`(order_id)   ON DELETE CASCADE,
    FOREIGN KEY (seat_id)  REFERENCES table_seat(seat_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE meal_item (
    meal_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    meal_id       INT NOT NULL,
    menu_item_id  INT NOT NULL,
    quantity      INT NOT NULL DEFAULT 1,
    FOREIGN KEY (meal_id)      REFERENCES meal(meal_id)      ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_item(item_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  8.  BILL  &  PAYMENT  (polymorphic: CC / Check / Cash)     ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE bill (
    bill_id    INT AUTO_INCREMENT PRIMARY KEY,
    order_id   INT            NOT NULL UNIQUE,
    amount     DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    tax        DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    tip        DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    is_paid    BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES `order`(order_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE payment (
    payment_id    INT AUTO_INCREMENT PRIMARY KEY,
    bill_id       INT            NOT NULL,
    amount        DECIMAL(12,2)  NOT NULL,
    payment_type  ENUM('CREDIT_CARD','CHECK','CASH') NOT NULL,
    -- Credit Card fields (nullable)
    name_on_card  VARCHAR(150),
    card_last_four VARCHAR(4),
    -- Check fields (nullable)
    bank_name     VARCHAR(150),
    check_number  VARCHAR(50),
    -- Cash fields (nullable)
    cash_tendered DECIMAL(12,2),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bill(bill_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  9.  NOTIFICATIONS  (sent by background daemon thread)      ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE notification (
    notification_id   INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id    INT,
    customer_id       INT,
    content           TEXT         NOT NULL,
    notification_type ENUM('EMAIL','SMS','SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    is_sent           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id) ON DELETE SET NULL,
    FOREIGN KEY (customer_id)    REFERENCES customer(customer_id)       ON DELETE SET NULL
) ENGINE=InnoDB;


-- ╔══════════════════════════════════════════════════════════════╗
-- ║  INITIAL DATA — TWO MANAGER ACCOUNTS                        ║
-- ╚══════════════════════════════════════════════════════════════╝

INSERT INTO users (username, password, role)
VALUES ('samandar_karimov', '12345678', 'MANAGER');

INSERT INTO users (username, password, role)
VALUES ('nursulton_tursunqulov', '87654321', 'MANAGER');

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  MIGRATION — run if the DB already exists                   ║
-- ╚══════════════════════════════════════════════════════════════╝
-- Step 1: promote existing ADMIN accounts to MANAGER
-- UPDATE users SET role = 'MANAGER' WHERE role = 'ADMIN';
--
-- Step 2: drop ADMIN from the role enum
-- ALTER TABLE users
--   MODIFY role ENUM('MANAGER','RECEPTIONIST','WAITER','CASHIER','CHEF') NOT NULL;


SELECT '✓ Schema created. Two manager accounts inserted.' AS status;
