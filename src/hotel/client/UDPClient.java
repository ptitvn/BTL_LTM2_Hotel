package hotel.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/** Client UDP (Swing) - Quản lý khách sạn & phòng. */
public class UDPClient extends JFrame {

    static final String HOST = "localhost";
    static final int PORT = 9999;

    // >>> SỬA 2 DÒNG NÀY THÀNH THÔNG TIN CỦA BẠN <<<
    static final String MSSV  = "B24DTCN419";
    static final String HOTEN = "Le Trung Dong";

    // Khách sạn
    JTextField hId = new JTextField(), hName = new JTextField(), hStars = new JTextField(), hDesc = new JTextField(), hSearch = new JTextField();
    DefaultTableModel hModel = new DefaultTableModel(new String[]{"Mã", "Tên", "Số sao", "Mô tả"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    JTable hTable = new JTable(hModel);

    // Phòng
    JComboBox<String> cbHotel = new JComboBox<>();
    JTextField rId = new JTextField(), rNum = new JTextField(), rType = new JTextField(), rPrice = new JTextField(),
            sType = new JTextField(), sMin = new JTextField(), sMax = new JTextField();
    DefaultTableModel rModel = new DefaultTableModel(new String[]{"Mã phòng", "Mã KS", "Số phòng", "Loại", "Giá"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    JTable rTable = new JTable(rModel);

    // Khu vực hiển thị kết quả
    JTextArea log = new JTextArea(6, 40);

    public UDPClient() {
        super("Quản lý khách sạn - UDP Client - " + MSSV + " - " + HOTEN);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Khách sạn", buildHotelTab());
        tabs.addTab("Phòng", buildRoomTab());
        log.setEditable(false);
        JScrollPane lp = new JScrollPane(log);
        lp.setBorder(BorderFactory.createTitledBorder("Kết quả thực hiện"));
        // Thông tin sinh viên thực hiện (yêu cầu BT3)
        JLabel info = new JLabel("MSSV: " + MSSV + "   -   Họ tên SV: " + HOTEN, SwingConstants.CENTER);
        info.setFont(info.getFont().deriveFont(Font.BOLD, 15f));
        info.setForeground(new Color(180, 0, 0));
        info.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        add(info, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(lp, BorderLayout.SOUTH);
        setSize(850, 620);
        setLocationRelativeTo(null);
        loadHotels();
        searchRooms();
    }

    // ================= GIAO DIỆN =================
    JPanel buildHotelTab() {
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Mã khách sạn:")); form.add(hId);
        form.add(new JLabel("Tên:")); form.add(hName);
        form.add(new JLabel("Số sao (1-5):")); form.add(hStars);
        form.add(new JLabel("Mô tả:")); form.add(hDesc);
        form.add(new JLabel("Từ khóa tìm kiếm (mã/tên):")); form.add(hSearch);

        JButton add = new JButton("Thêm"), upd = new JButton("Sửa"), del = new JButton("Xóa"), find = new JButton("Tìm kiếm");
        JPanel btns = new JPanel();
        btns.add(add); btns.add(upd); btns.add(del); btns.add(find);

        add.addActionListener(e -> doCmd(validHotel(), "HOTEL_ADD|" + hId.getText() + "|" + hName.getText() + "|" + hStars.getText() + "|" + hDesc.getText(), this::afterHotelChange));
        upd.addActionListener(e -> doCmd(validHotel(), "HOTEL_UPDATE|" + hId.getText() + "|" + hName.getText() + "|" + hStars.getText() + "|" + hDesc.getText(), this::afterHotelChange));
        del.addActionListener(e -> doCmd(needId(hId, "Mã khách sạn"), "HOTEL_DELETE|" + hId.getText(), this::afterHotelChange));
        find.addActionListener(e -> { if (noBad(hSearch, "Từ khóa")) fillTable(hModel, "HOTEL_SEARCH|" + hSearch.getText()); });

        hTable.getSelectionModel().addListSelectionListener(e -> {
            int r = hTable.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                hId.setText(hModel.getValueAt(r, 0).toString());
                hName.setText(hModel.getValueAt(r, 1).toString());
                hStars.setText(hModel.getValueAt(r, 2).toString());
                hDesc.setText(hModel.getValueAt(r, 3).toString());
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(hTable), BorderLayout.CENTER);
        return p;
    }

    JPanel buildRoomTab() {
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Khách sạn:")); form.add(cbHotel);
        form.add(new JLabel("Mã phòng:")); form.add(rId);
        form.add(new JLabel("Số phòng:")); form.add(rNum);
        form.add(new JLabel("Loại phòng:")); form.add(rType);
        form.add(new JLabel("Giá:")); form.add(rPrice);
        form.add(new JLabel("Tìm theo loại phòng:")); form.add(sType);
        form.add(new JLabel("Giá từ:")); form.add(sMin);
        form.add(new JLabel("Giá đến:")); form.add(sMax);

        JButton add = new JButton("Thêm phòng"), upd = new JButton("Cập nhật"), del = new JButton("Xóa phòng"), find = new JButton("Tìm & hiển thị");
        JPanel btns = new JPanel();
        btns.add(add); btns.add(upd); btns.add(del); btns.add(find);

        add.addActionListener(e -> {
            if (selectedHotelId().isEmpty()) { log("[Lỗi] Hãy chọn khách sạn từ danh sách"); return; }
            doCmd(validRoom(), roomMsg("ROOM_ADD"), this::afterRoomChange);
        });
        upd.addActionListener(e -> {
            if (selectedHotelId().isEmpty()) { log("[Lỗi] Hãy chọn khách sạn từ danh sách"); return; }
            doCmd(validRoom(), roomMsg("ROOM_UPDATE"), this::afterRoomChange);
        });
        del.addActionListener(e -> doCmd(needId(rId, "Mã phòng"), "ROOM_DELETE|" + rId.getText(), this::afterRoomChange));
        find.addActionListener(e -> searchRooms());

        rTable.getSelectionModel().addListSelectionListener(e -> {
            int r = rTable.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                rId.setText(rModel.getValueAt(r, 0).toString());
                String hid = rModel.getValueAt(r, 1).toString();
                for (int i = 0; i < cbHotel.getItemCount(); i++)
                    if (cbHotel.getItemAt(i).startsWith(hid + " - ")) cbHotel.setSelectedIndex(i);
                rNum.setText(rModel.getValueAt(r, 2).toString());
                rType.setText(rModel.getValueAt(r, 3).toString());
                rPrice.setText(rModel.getValueAt(r, 4).toString());
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(rTable), BorderLayout.CENTER);
        return p;
    }

    // ================= KIỂM TRA DỮ LIỆU NHẬP =================
    static final String ID_REGEX = "[A-Za-z0-9_-]{1,20}";
    static final String NUM_REGEX = "[0-9]{1,13}";

    boolean fail(String msg, JTextField f) {
        log("[Lỗi] " + msg);
        f.requestFocus();
        return false;
    }

    /** Ký tự '|' và ký tự điều khiển (xuống dòng, tab...) làm hỏng giao thức. */
    boolean noBad(JTextField f, String label) {
        if (f.getText().matches("(?s).*[|\\p{Cntrl}].*"))
            return fail(label + " không được chứa ký tự '|' hoặc ký tự xuống dòng", f);
        return true;
    }

    boolean needText(JTextField f, String label, int max) {
        String v = f.getText().trim();
        if (v.isEmpty()) return fail(label + " không được để trống", f);
        if (v.length() > max) return fail(label + " tối đa " + max + " ký tự", f);
        return noBad(f, label);
    }

    boolean needId(JTextField f, String label) {
        String v = f.getText().trim();
        if (v.isEmpty()) return fail("Chưa nhập " + label.toLowerCase(), f);
        if (!v.matches(ID_REGEX))
            return fail(label + " chỉ gồm chữ không dấu, số, '_' hoặc '-' (tối đa 20 ký tự, không có khoảng trắng)", f);
        return true;
    }

    boolean validHotel() {
        if (!needId(hId, "Mã khách sạn")) return false;
        if (!needText(hName, "Tên khách sạn", 100)) return false;
        if (!hStars.getText().trim().matches("[1-5]")) return fail("Số sao phải là số nguyên từ 1 đến 5", hStars);
        if (hDesc.getText().trim().length() > 255) return fail("Mô tả tối đa 255 ký tự", hDesc);
        return noBad(hDesc, "Mô tả");
    }

    boolean validRoom() {
        if (selectedHotelId().isEmpty()) {
            log("[Lỗi] Hãy chọn khách sạn từ danh sách");
            cbHotel.requestFocus();
            return false;
        }
        if (!needId(rId, "Mã phòng")) return false;
        if (!needText(rNum, "Số phòng", 20)) return false;
        if (!needText(rType, "Loại phòng", 50)) return false;
        String p = rPrice.getText().trim();
        if (!p.matches(NUM_REGEX) || Long.parseLong(p) <= 0)
            return fail("Giá phải là số nguyên dương, không có dấu chấm/phẩy (VD: 800000)", rPrice);
        return true;
    }

    boolean validSearch() {
        if (!noBad(sType, "Loại phòng")) return false;
        String a = sMin.getText().trim(), b = sMax.getText().trim();
        if (!a.isEmpty() && !a.matches(NUM_REGEX)) return fail("Giá từ phải là số nguyên không âm", sMin);
        if (!b.isEmpty() && !b.matches(NUM_REGEX)) return fail("Giá đến phải là số nguyên không âm", sMax);
        if (!a.isEmpty() && !b.isEmpty() && Long.parseLong(a) > Long.parseLong(b))
            return fail("Giá từ không được lớn hơn giá đến", sMin);
        return true;
    }

    /** Chỉ gửi lệnh khi dữ liệu hợp lệ. */
    void doCmd(boolean valid, String msg, Runnable onOk) {
        if (valid) doCmd(msg, onOk);
    }

    // ================= XÓA TRẮNG Ô NHẬP SAU KHI THÀNH CÔNG =================
    void afterHotelChange() {
        loadHotels();
        hId.setText(""); hName.setText(""); hStars.setText(""); hDesc.setText("");
        hId.requestFocus();
    }

    void afterRoomChange() {
        searchRooms();
        rId.setText(""); rNum.setText(""); rType.setText(""); rPrice.setText("");
        rId.requestFocus();
    }

    // ================= XỬ LÝ =================
    String selectedHotelId() {
        String s = (String) cbHotel.getSelectedItem();
        if (s == null || s.startsWith("(Tất cả)")) return "";
        return s.substring(0, s.indexOf(" - "));
    }

    String roomMsg(String cmd) {
        return cmd + "|" + rId.getText() + "|" + selectedHotelId() + "|" + rNum.getText() + "|" + rType.getText() + "|" + rPrice.getText();
    }

    void searchRooms() {
        if (!validSearch()) return;
        fillTable(rModel, "ROOM_SEARCH|" + selectedHotelId() + "|" + sType.getText() + "|" + sMin.getText() + "|" + sMax.getText());
    }

    /** Tải danh sách khách sạn vào bảng và combobox. */
    void loadHotels() {
        List<String> r = send("HOTEL_SEARCH|");
        if (r == null || !r.get(0).equals("OK")) return;
        hModel.setRowCount(0);
        String old = (String) cbHotel.getSelectedItem();
        cbHotel.removeAllItems();
        cbHotel.addItem("(Tất cả)");
        for (int i = 2; i < r.size(); i++) {
            if (r.get(i).isEmpty()) continue;
            String[] f = r.get(i).split("\\|", -1);
            hModel.addRow(f);
            cbHotel.addItem(f[0] + " - " + f[1]);
        }
        if (old != null) cbHotel.setSelectedItem(old);
    }

    /** Gửi lệnh thêm/sửa/xóa, hiển thị kết quả, nếu thành công thì chạy onOk. */
    void doCmd(String msg, Runnable onOk) {
        List<String> r = send(msg);
        if (r == null) return;
        boolean ok = r.get(0).equals("OK");
        log((ok ? "[Thành công] " : "[Lỗi] ") + r.get(1));
        if (ok && onOk != null) onOk.run();
    }

    /** Gửi lệnh tìm kiếm và đổ dữ liệu vào bảng. */
    void fillTable(DefaultTableModel m, String msg) {
        List<String> r = send(msg);
        if (r == null) return;
        boolean ok = r.get(0).equals("OK");
        log((ok ? "[Thành công] " : "[Lỗi] ") + r.get(1));
        if (!ok) return;
        m.setRowCount(0);
        for (int i = 2; i < r.size(); i++)
            if (!r.get(i).isEmpty()) m.addRow(r.get(i).split("\\|", -1));
    }

    void log(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    /** Gửi 1 datagram UDP và nhận phản hồi. */
    List<String> send(String msg) {
        try (DatagramSocket s = new DatagramSocket()) {
            s.setSoTimeout(3000);
            byte[] b = msg.getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(b, b.length, InetAddress.getByName(HOST), PORT));
            byte[] buf = new byte[65000];
            DatagramPacket in = new DatagramPacket(buf, buf.length);
            s.receive(in);
            String res = new String(in.getData(), 0, in.getLength(), StandardCharsets.UTF_8);
            return Arrays.asList(res.split("\n"));
        } catch (SocketTimeoutException e) {
            log("[Lỗi] Server không phản hồi (hết thời gian chờ). Server đã chạy chưa?");
        } catch (Exception e) {
            log("[Lỗi] " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UDPClient().setVisible(true));
    }
}