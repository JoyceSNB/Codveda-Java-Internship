-- Level 3 (Advanced) - Task 1: Library Management System with JDBC
-- Codveda Technology - Java Development Internship
-- Run this once before starting the application:
-- mysql -u root -p < schema.sql

DROP DATABASE IF EXISTS library_db;
CREATE DATABASE library_db;
USE library_db;

-- Books
-- available_copies is the source of truth for whether a book can be
-- borrowed. It is decremented on borrow and incremented on return,
-- always inside a transaction together with the transactions row.

CREATE TABLE books (
    book_id          INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    author           VARCHAR(120) NOT NULL,
    isbn             VARCHAR(20)  NOT NULL UNIQUE,
    total_copies     INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,

    CONSTRAINT chk_copies_non_negative CHECK (available_copies >= 0),
    CONSTRAINT chk_copies_within_total CHECK (available_copies <= total_copies)
);

-- Users
CREATE TABLE users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(120) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    joined_at DATE NOT NULL
);

-- Transactions (borrow / return records)
-- returned_at IS NULL means the book is still out on loan.
CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id        INT NOT NULL,
    user_id        INT NOT NULL,
    borrowed_at    DATETIME NOT NULL,
    returned_at    DATETIME NULL,

    CONSTRAINT fk_txn_book FOREIGN KEY (book_id) REFERENCES books(book_id),
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_txn_active ON transactions (returned_at);

-- Seed data so the application has something to show on first run
INSERT INTO books (title, author, isbn, total_copies, available_copies) VALUES
    ('Clean Code',                     'Robert C. Martin',  '9780132350884', 3, 3),
    ('Effective Java',                 'Joshua Bloch',      '9780134685991', 2, 2),
    ('Head First Design Patterns',     'Eric Freeman',      '9780596007126', 2, 2),
    ('Introduction to Algorithms',     'Thomas H. Cormen',  '9780262046305', 1, 1),
    ('The Pragmatic Programmer',       'Andrew Hunt',       '9780135957059', 2, 2);

INSERT INTO users (name, email, joined_at) VALUES
    ('Joyce Stephanie Naibaho',         'joyce@example.com',  '2026-02-20'),
    ('Jeremy Ruben Siregar',  'jeremy@example.com', '2026-02-03');
