package hotel.server;

import java.sql.*;

public class DBConnection {

    public static final String HOST = "localhost";
    public static final String PORT = "3306";
    public static final String DB   = "hotel_db";
    public static final String USER = "root";
    public static final String PASS = "";

    static final String OPTS = "?useUnicode=true&characterEncoding=UTF-8&useSSL=false";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DB + OPTS, USER, PASS);
    }

    /** Tự tạo database và các bảng nếu chưa có. */
    public static void init() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTS, USER, PASS);
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB + " CHARACTER SET utf8 COLLATE utf8_unicode_ci");
        }
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS hotel ("
                    + "id VARCHAR(20) PRIMARY KEY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "stars INT NOT NULL,"
                    + "description VARCHAR(255)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS room ("
                    + "id VARCHAR(20) PRIMARY KEY,"
                    + "hotel_id VARCHAR(20) NOT NULL,"
                    + "room_number VARCHAR(20) NOT NULL,"
                    + "type VARCHAR(50) NOT NULL,"
                    + "price DECIMAL(15,0) NOT NULL,"
                    + "FOREIGN KEY (hotel_id) REFERENCES hotel(id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");
        }
    }
}
