-- Personal Document Expiry Tracker - MySQL schema
-- Run once:  mysql -u root -p < sql/schema.sql
-- (The app also creates the table automatically on first start.)

CREATE DATABASE IF NOT EXISTS doc_tracker
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE doc_tracker;

CREATE TABLE IF NOT EXISTS documents (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(120) NOT NULL,
    doc_type         VARCHAR(30)  NOT NULL,          -- PASSPORT, DRIVING_LICENSE, INSURANCE, WARRANTY, ...
    doc_number       VARCHAR(60),
    issuer           VARCHAR(120),                   -- authority / insurer / shop
    issue_date       DATE,
    expiry_date      DATE         NOT NULL,
    reminder_days    INT          NOT NULL DEFAULT 30, -- start reminding N days before expiry
    notes            VARCHAR(500),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_expiry (expiry_date)
);

-- Sample data (optional)
INSERT INTO documents (title, doc_type, doc_number, issuer, issue_date, expiry_date, reminder_days, notes) VALUES
 ('My Passport',          'PASSPORT',        'P1234567',   'Passport Seva, MEA',  '2017-03-10', DATE_ADD(CURDATE(), INTERVAL 75 DAY), 180, 'Renew at least 6 months before travel'),
 ('Driving Licence',      'DRIVING_LICENSE', 'PB10 20190012345', 'RTO Patiala',   '2019-06-01', DATE_ADD(CURDATE(), INTERVAL 20 DAY), 30,  NULL),
 ('Car Insurance',        'INSURANCE',       'POL-889922', 'ICICI Lombard',       '2025-12-01', DATE_SUB(CURDATE(), INTERVAL 5 DAY),  15,  'Comprehensive cover'),
 ('Laptop Warranty',      'WARRANTY',        'SN-5CD1234', 'ASUS India',          '2024-08-15', DATE_ADD(CURDATE(), INTERVAL 300 DAY), 30, 'Bill in Google Drive');
