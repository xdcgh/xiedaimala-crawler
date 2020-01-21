-- MySQL的初始化和设置编码为utf-8
-- create database news;
-- alter database news character set = utf8mb4 collate = utf8mb4_unicode_ci;

CREATE TABLE LINKS_TO_BE_PROCESSED (LINK VARCHAR(10000));
CREATE TABLE LINKS_ALREADY_PROCESSED (LINK VARCHAR(10000));
CREATE TABLE NEWS (
    ID BIGINT PRIMARY KEY AUTO_INCREMENT,
    TITLE TEXT,
    CONTENT TEXT,
    URL VARCHAR(10000),
    CREATED_AT TIMESTAMP default now(),
    MODIFIED_AT TIMESTAMP default now()
);