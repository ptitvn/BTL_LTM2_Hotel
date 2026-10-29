-- Import bằng phpMyAdmin (XAMPP) nếu muốn tạo DB thủ công.
-- (Server cũng tự tạo DB + bảng khi khởi động nên bước này KHÔNG bắt buộc.)
CREATE DATABASE IF NOT EXISTS hotel_db CHARACTER SET utf8 COLLATE utf8_unicode_ci;
USE hotel_db;

CREATE TABLE IF NOT EXISTS hotel (
  id VARCHAR(20) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  stars INT NOT NULL,
  description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS room (
  id VARCHAR(20) PRIMARY KEY,
  hotel_id VARCHAR(20) NOT NULL,
  room_number VARCHAR(20) NOT NULL,
  type VARCHAR(50) NOT NULL,
  price DECIMAL(15,0) NOT NULL,
  FOREIGN KEY (hotel_id) REFERENCES hotel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
