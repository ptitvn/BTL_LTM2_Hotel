package hotel.server;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;


public class UDPServer {

    static final int PORT = 9999;
    static final String ID_REGEX = "[A-Za-z0-9_-]{1,20}";
    static final String NUM_REGEX = "[0-9]{1,13}";
    static final String ID_MSG = " chỉ gồm chữ không dấu, số, '_' hoặc '-' (tối đa 20 ký tự, không có khoảng trắng)";

    public static void main(String[] args) throws Exception {
        try {
            DBConnection.init();
            System.out.println("Ket noi MySQL thanh cong (database: " + DBConnection.DB + ")");
        } catch (SQLException e) {
            System.out.println("KHONG ket noi duoc MySQL: " + e.getMessage());
            System.out.println("-> Hay bat MySQL trong XAMPP Control Panel roi chay lai.");
            return;
        }
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP Server dang chay tai cong " + PORT);
            byte[] buf = new byte[65000];
            while (true) {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                socket.receive(in);
                String req = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
                System.out.println("<< " + req);
                String res;
                try {
                    res = handle(req);
                } catch (SQLException e) {
                    res = err("Lỗi CSDL: " + e.getMessage());
                } catch (Exception e) {
                    res = err("Dữ liệu không hợp lệ: " + e.getMessage());
                }
                byte[] out = res.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(out, out.length, in.getAddress(), in.getPort()));
            }
        }
    }

    static String ok(String msg) { return "OK\n" + msg; }
    static String err(String msg) { return "ERR\n" + msg; }

    static String handle(String req) throws SQLException {
        if (req.matches("(?s).*[\\p{Cntrl}].*"))
            return err("Dữ liệu chứa ký tự điều khiển (xuống dòng, tab...) không hợp lệ");
        String[] p = req.split("\\|", -1);
        for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
        // Số trường phải đúng chuẩn; dư trường nghĩa là dữ liệu có chứa ký tự '|'
        int expected;
        switch (p[0]) {
            case "HOTEL_ADD": case "HOTEL_UPDATE": expected = 5; break;
            case "ROOM_ADD":  case "ROOM_UPDATE":  expected = 6; break;
            case "ROOM_SEARCH":                     expected = 5; break;
            case "HOTEL_DELETE": case "HOTEL_SEARCH": case "ROOM_DELETE": expected = 2; break;
            default: return err("Lệnh không hợp lệ: " + p[0]);
        }
        if (p.length != expected) return err("Dữ liệu không đúng định dạng (không được chứa ký tự '|')");

        switch (p[0]) {
            case "HOTEL_ADD":    return hotelSave(p, true);
            case "HOTEL_UPDATE": return hotelSave(p, false);
            case "HOTEL_DELETE": return hotelDelete(p[1]);
            case "HOTEL_SEARCH": return hotelSearch(p[1]);
            case "ROOM_ADD":     return roomSave(p, true);
            case "ROOM_UPDATE":  return roomSave(p, false);
            case "ROOM_DELETE":  return roomDelete(p[1]);
            default:             return roomSearch(p);
        }
    }

    // ---------- tiện ích ----------
    static boolean exists(Connection c, String sql, String... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Escape ký tự đặc biệt của LIKE để từ khóa chứa % hoặc _ không khớp bừa. */
    static String like(String s) {
        return "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    // ---------- KHÁCH SẠN ----------
    static String hotelSave(String[] p, boolean add) throws SQLException {
        String id = p[1], name = p[2], desc = p[4];
        if (!id.matches(ID_REGEX)) return err("Mã khách sạn" + ID_MSG);
        if (name.isEmpty() || name.length() > 100) return err("Tên khách sạn không được để trống và tối đa 100 ký tự");
        if (!p[3].matches("[1-5]")) return err("Số sao phải là số nguyên từ 1 đến 5");
        if (desc.length() > 255) return err("Mô tả tối đa 255 ký tự");
        int stars = Integer.parseInt(p[3]);

        try (Connection c = DBConnection.get()) {
            boolean has = exists(c, "SELECT COUNT(*) FROM hotel WHERE id=?", id);
            if (add && has) return err("Mã khách sạn đã tồn tại");
            if (!add && !has) return err("Không tìm thấy khách sạn");
            String sql = add ? "INSERT INTO hotel(name,stars,description,id) VALUES(?,?,?,?)"
                    : "UPDATE hotel SET name=?, stars=?, description=? WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setInt(2, stars);
                ps.setString(3, desc);
                ps.setString(4, id);
                ps.executeUpdate();
            }
        }
        return ok(add ? "Thêm khách sạn thành công: " + id + " - " + name
                : "Cập nhật khách sạn thành công: " + id);
    }

    static String hotelDelete(String id) throws SQLException {
        if (id.isEmpty()) return err("Chưa nhập mã khách sạn");
        try (Connection c = DBConnection.get()) {
            if (!exists(c, "SELECT COUNT(*) FROM hotel WHERE id=?", id)) return err("Không tìm thấy khách sạn");
            if (exists(c, "SELECT COUNT(*) FROM room WHERE hotel_id=?", id))
                return err("Khách sạn vẫn còn phòng, phải xóa hết phòng trước");
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM hotel WHERE id=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        }
        return ok("Xóa khách sạn thành công: " + id);
    }

    static String hotelSearch(String kw) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id,name,stars,description FROM hotel WHERE id LIKE ? OR name LIKE ? ORDER BY id")) {
            ps.setString(1, like(kw));
            ps.setString(2, like(kw));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append("\n").append(rs.getString(1)).append("|").append(rs.getString(2)).append("|")
                            .append(rs.getInt(3)).append("|").append(nz(rs.getString(4)));
                    n++;
                }
            }
        }
        return ok("Tìm thấy " + n + " khách sạn") + sb;
    }

    // ---------- PHÒNG ----------
    static String roomSave(String[] p, boolean add) throws SQLException {
        String id = p[1], hotelId = p[2], number = p[3], type = p[4];
        if (!id.matches(ID_REGEX)) return err("Mã phòng" + ID_MSG);
        if (hotelId.isEmpty()) return err("Chưa chọn khách sạn");
        if (!hotelId.matches(ID_REGEX)) return err("Mã khách sạn không hợp lệ");
        if (number.isEmpty() || number.length() > 20) return err("Số phòng không được để trống và tối đa 20 ký tự");
        if (type.isEmpty() || type.length() > 50) return err("Loại phòng không được để trống và tối đa 50 ký tự");
        if (!p[5].matches(NUM_REGEX)) return err("Giá phải là số nguyên dương, không có dấu chấm/phẩy (VD: 800000)");
        long price = Long.parseLong(p[5]);
        if (price <= 0) return err("Giá phải lớn hơn 0");

        try (Connection c = DBConnection.get()) {
            if (!exists(c, "SELECT COUNT(*) FROM hotel WHERE id=?", hotelId)) return err("Khách sạn không tồn tại");
            boolean has = exists(c, "SELECT COUNT(*) FROM room WHERE id=?", id);
            if (add && has) return err("Mã phòng đã tồn tại");
            if (!add && !has) return err("Không tìm thấy phòng");
            if (exists(c, "SELECT COUNT(*) FROM room WHERE hotel_id=? AND room_number=? AND id<>?", hotelId, number, id))
                return err("Số phòng " + number + " đã tồn tại trong khách sạn " + hotelId);
            String sql = add ? "INSERT INTO room(hotel_id,room_number,type,price,id) VALUES(?,?,?,?,?)"
                    : "UPDATE room SET hotel_id=?, room_number=?, type=?, price=? WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, hotelId);
                ps.setString(2, number);
                ps.setString(3, type);
                ps.setLong(4, price);
                ps.setString(5, id);
                ps.executeUpdate();
            }
        }
        return ok(add ? "Thêm phòng thành công: " + number + " (khách sạn " + hotelId + ")"
                : "Cập nhật phòng thành công: " + id);
    }

    static String roomDelete(String id) throws SQLException {
        if (id.isEmpty()) return err("Chưa nhập mã phòng");
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM room WHERE id=?")) {
            ps.setString(1, id);
            if (ps.executeUpdate() == 0) return err("Không tìm thấy phòng");
        }
        return ok("Xóa phòng thành công: " + id);
    }

    static String roomSearch(String[] p) throws SQLException {
        String hotelId = p[1], type = p[2];
        if (!p[3].isEmpty() && !p[3].matches(NUM_REGEX)) return err("Giá từ phải là số nguyên không âm");
        if (!p[4].isEmpty() && !p[4].matches(NUM_REGEX)) return err("Giá đến phải là số nguyên không âm");
        long min = p[3].isEmpty() ? 0 : Long.parseLong(p[3]);
        long max = p[4].isEmpty() ? Long.MAX_VALUE : Long.parseLong(p[4]);
        if (min > max) return err("Giá từ không được lớn hơn giá đến");

        StringBuilder sql = new StringBuilder("SELECT id,hotel_id,room_number,type,price FROM room WHERE price>=? AND price<=?");
        if (!hotelId.isEmpty()) sql.append(" AND hotel_id=?");
        if (!type.isEmpty()) sql.append(" AND type LIKE ?");
        sql.append(" ORDER BY hotel_id, room_number");

        StringBuilder sb = new StringBuilder();
        int n = 0;
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setLong(i++, min);
            ps.setLong(i++, max);
            if (!hotelId.isEmpty()) ps.setString(i++, hotelId);
            if (!type.isEmpty()) ps.setString(i++, like(type));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append("\n").append(rs.getString(1)).append("|").append(rs.getString(2)).append("|")
                            .append(rs.getString(3)).append("|").append(rs.getString(4)).append("|")
                            .append(rs.getLong(5));
                    n++;
                }
            }
        }
        return ok("Tìm thấy " + n + " phòng") + sb;
    }

    static String nz(String s) { return s == null ? "" : s; }
}