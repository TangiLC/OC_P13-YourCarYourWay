-- Création de la base
CREATE DATABASE IF NOT EXISTS YCYW DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE YCYW;

-- Table user_credentials
CREATE TABLE user_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'AGENT', 'ADMIN') NOT NULL
);

-- Table user_profiles
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    company VARCHAR(255),
    type ENUM('INDIVIDUAL', 'COMPANY', 'SUPPORT', 'AGENCY') DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES user_credentials(id) ON DELETE CASCADE
);

-- Table dialogs
CREATE TABLE dialogs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(255),
    status ENUM('OPEN', 'PENDING', 'CLOSED') NOT NULL,
    created_at DATETIME NOT NULL,
    closed_at DATETIME
);

-- Table messages
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dialog_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    content TEXT NOT NULL,
    sender_id BIGINT NOT NULL,
    FOREIGN KEY (dialog_id) REFERENCES dialogs(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user_profiles(id) ON DELETE CASCADE
);

-- Table rel_user_dialog
CREATE TABLE rel_user_dialog (
    dialog_id BIGINT NOT NULL,
    user_profile_id BIGINT NOT NULL,
    PRIMARY KEY (dialog_id, user_profile_id),
    FOREIGN KEY (dialog_id) REFERENCES dialogs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id) ON DELETE CASCADE
);

-- Indexes pour les performances
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_messages_dialog_id ON messages(dialog_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_dialogs_status ON dialogs(status);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
