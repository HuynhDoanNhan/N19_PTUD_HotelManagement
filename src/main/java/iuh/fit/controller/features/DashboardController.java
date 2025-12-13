package iuh.fit.controller.features;

import iuh.fit.controller.MainController;
import iuh.fit.dao.InvoiceDAO;
import iuh.fit.dao.ReservationDAO;
import iuh.fit.dao.RoomDAO;
import iuh.fit.models.Account;
import iuh.fit.models.Invoice;
import iuh.fit.models.Reservation;
import iuh.fit.models.Room;
import iuh.fit.models.enums.RoomStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    /* ========== KPI LABELS ========== */
    @FXML private Label totalRoomLabel;
    @FXML private Label usingRoomLabel;
    @FXML private Label availableRoomLabel;
    @FXML private Label occupancyRateLabel;
    @FXML private Label currentDateLabel;

    /* ========== CHARTS ========== */
    @FXML private LineChart<String, Number> revenueChart;

    @FXML private BarChart<String, Number> roomTypeBarChart;
    @FXML private CategoryAxis roomTypeBarXAxis;
    @FXML private NumberAxis roomTypeBarYAxis;

    /* ========== TABLES ========== */
    @FXML private TableView<Reservation> checkinTable;
    @FXML private TableView<Reservation> checkoutTable;

    @FXML private TableColumn<Reservation, String> colCheckinCustomer;
    @FXML private TableColumn<Reservation, String> colCheckinRoom;
    @FXML private TableColumn<Reservation, String> colCheckinTime;

    @FXML private TableColumn<Reservation, String> colCheckoutCustomer;
    @FXML private TableColumn<Reservation, String> colCheckoutRoom;
    @FXML private TableColumn<Reservation, String> colCheckoutTime;


    public void initialize() {
        loadCurrentDate();
        loadKPI();
        loadRevenueChart();
        loadRoomTypeBarChart();
        loadTodayCheckinCheckout();

        // Cột bảng check-in
        colCheckinCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colCheckinRoom.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colCheckinTime.setCellValueFactory(r ->
                new SimpleStringProperty(r.getValue().getTime().toLocalTime().toString())
        );

        // Cột bảng check-out
        colCheckoutCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colCheckoutRoom.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colCheckoutTime.setCellValueFactory(r ->
                new SimpleStringProperty(r.getValue().getTime().toLocalTime().toString())
        );
    }

    /* =====================================================
                        1. CURRENT DATE
       ===================================================== */
    private void loadCurrentDate() {
        currentDateLabel.setText(LocalDate.now().toString());
    }

    /* =====================================================
                        2. KPI NUMBERS
       ===================================================== */
    public void loadKPI() {   // <-- đổi private → public
        List<Room> allRooms = RoomDAO.getRoom();
        int total = allRooms.size();
        int using = (int) allRooms.stream()
        		.filter(r -> r.getRoomStatus() == RoomStatus.ON_USE)
                .count();
        int available = total - using;

        double occupancyRate = total == 0 ? 0 : ((double) using / total) * 100;

        totalRoomLabel.setText(String.valueOf(total));
        usingRoomLabel.setText(String.valueOf(using));
        availableRoomLabel.setText(String.valueOf(available));
        occupancyRateLabel.setText(String.format("%.1f%%", occupancyRate));
    }

    /* =====================================================
                        3. REVENUE LINE CHART
       ===================================================== */
    public void loadRevenueChart() {   // <-- đổi private → public
        List<Invoice> invoices = InvoiceDAO.getPaidInvoices();

        Map<String, Double> dailyRevenue = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        for (Invoice inv : invoices) {
            LocalDate date = inv.getInvoiceDate().toLocalDate();
            if (!date.isBefore(start) && !date.isAfter(today)) {
                dailyRevenue.merge(date.toString(), inv.getNetDue(), Double::sum);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            series.getData().add(
                    new XYChart.Data<>(d.toString(), dailyRevenue.getOrDefault(d.toString(), 0.0))
            );
        }

        revenueChart.getData().clear();
        revenueChart.getData().add(series);
    }

    /* =====================================================
                    4. ROOM TYPE BAR CHART
       ===================================================== */
    public void loadRoomTypeBarChart() {   // <-- đổi private → public
        List<Room> rooms = RoomDAO.getRoom();

        int thuongDon = 0;
        int thuongDoi = 0;
        int vipDon = 0;
        int vipDoi = 0;

        for (Room r : rooms) {
            String name = r.getRoomCategory().getRoomCategoryName().toLowerCase();

            if (name.contains("thường") && name.contains("đơn")) thuongDon++;
            else if (name.contains("thường") && name.contains("đôi")) thuongDoi++;
            else if (name.contains("vip") && name.contains("đơn")) vipDon++;
            else if (name.contains("vip") && name.contains("đôi")) vipDoi++;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lượt đặt");

        series.getData().add(new XYChart.Data<>("Thường - Đơn", thuongDon));
        series.getData().add(new XYChart.Data<>("Thường - Đôi", thuongDoi));
        series.getData().add(new XYChart.Data<>("VIP - Đơn",   vipDon));
        series.getData().add(new XYChart.Data<>("VIP - Đôi",   vipDoi));

        roomTypeBarChart.getData().clear();
        roomTypeBarChart.getData().add(series);
    }

    /* =====================================================
                5. TODAY CHECK-IN & CHECK-OUT
       ===================================================== */
    public void loadTodayCheckinCheckout() {   // <-- đổi private → public
        LocalDate today = LocalDate.now();

        List<Reservation> checkins = ReservationDAO.getCheckinToday(today);
        List<Reservation> checkouts = ReservationDAO.getCheckoutToday(today);

        checkinTable.getItems().clear();
        checkoutTable.getItems().clear();

        checkinTable.getItems().addAll(checkins);
        checkoutTable.getItems().addAll(checkouts);
    }

    /* =====================================================
                6. UPDATE DASHBOARD (HÀM MỚI)
       ===================================================== */
    public void updateDashboard() {
        loadKPI();
        loadTodayCheckinCheckout();
        loadRoomTypeBarChart();
        loadRevenueChart();
    }

    public void setupContext(Account account, MainController mainController) {
        // để sau
    }
}
