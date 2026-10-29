BTL LTM - Quan ly khach san (UDP + MySQL/XAMPP)

1. Mo XAMPP Control Panel -> Start "MySQL" (khong can Apache).
2. NetBeans: File > Open Project > chon thu muc BTL_LTM_Hotel.
   (Neu bao thieu thu vien: chuot phai Project > Properties > Libraries > Add JAR/Folder > lib/mysql-connector-java-5.1.45.jar)
3. Chuot phai src/hotel/server/UDPServer.java > Run File  (chay truoc)
4. Chuot phai src/hotel/client/UDPClient.java > Run File
   Server tu tao database "hotel_db" va cac bang. Co the import db/hotel_db.sql bang phpMyAdmin neu muon.
5. Neu MySQL co mat khau / cong khac: sua trong src/hotel/server/DBConnection.java.
