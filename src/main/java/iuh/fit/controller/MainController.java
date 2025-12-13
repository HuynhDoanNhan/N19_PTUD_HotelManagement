package iuh.fit.controller;

import iuh.fit.controller.features.DashboardController;
import iuh.fit.controller.features.MenuController;

import iuh.fit.controller.features.NotificationButtonController;
import iuh.fit.controller.features.TopController;
import iuh.fit.controller.features.customer.CustomerManagerController;
import iuh.fit.controller.features.customer.CustomerSearchingController;
import iuh.fit.controller.features.employee.EmployeeManagerController;
import iuh.fit.controller.features.employee.EmployeeSearchingController;
import iuh.fit.controller.features.employee.ShiftManagerController;

import iuh.fit.controller.features.employee_information.EmployeeInformationController;
import iuh.fit.controller.features.invoice.InvoiceManagerController;

import iuh.fit.controller.features.room.RoomBookingController;
import iuh.fit.controller.features.room.RoomManagerController;
import iuh.fit.controller.features.room.RoomSearchingController;
import iuh.fit.controller.features.room.creating_reservation_form_controllers.CreateReservationFormController;
import iuh.fit.controller.features.service.HotelServiceManagerController;
import iuh.fit.controller.features.service.HotelServiceSearchingController;
import iuh.fit.dao.EmployeeDAO;
import iuh.fit.models.*;
import iuh.fit.models.enums.Position;
import iuh.fit.models.wrapper.RoomWithReservation;
import iuh.fit.utils.RoomManagementService;
import iuh.fit.utils.TimelineManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import static iuh.fit.dao.misc.ShiftDetailDAO.createStartingPoint;

public class MainController {
    private Account account;
    private Shift shift;

    @FXML
    private AnchorPane menuBar;
    @FXML
    private AnchorPane mainPanel;
    @FXML
    private AnchorPane topPanel;
    private Stage mainStage;

    private Button informationBtn;

    private MenuController menuController;

    private static boolean ROOM_BOOKING_LOADED = true;

    private NotificationButtonController notificationButtonController;

    private int shiftDetailID;
 
    
    // Không xóa
    public void initialize(Account account, Stage stage, Shift shift) {
        if (account == null) throw new IllegalArgumentException("Tài khoản không tồn tại");

        this.account = account;
        this.mainStage = stage;
        this.shift = shift;

        Locale locale = new Locale("vi", "VN");
        Locale.setDefault(locale);

        initializeTopBar();
        initializeDashboard();
        initializeMenuBar();

        shiftDetailID = createStartingPoint();
    }

    public void initializeDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/iuh/fit/view/features/DashboardPanel.fxml"));
            AnchorPane dashboardLayout = loader.load();

            DashboardController dashboardController = loader.getController();
            dashboardController.setupContext(account, this);

            mainPanel.getChildren().setAll(dashboardLayout);
            AnchorPane.setTopAnchor(dashboardLayout, 0.0);
            AnchorPane.setBottomAnchor(dashboardLayout, 0.0);
            AnchorPane.setLeftAnchor(dashboardLayout, 0.0);
            AnchorPane.setRightAnchor(dashboardLayout, 0.0);
 // ✔ ADD ROOT NODE

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void initializeMenuBar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/iuh/fit/view/features/MenuPanel.fxml"));
            AnchorPane menuLayout = loader.load();

            menuController = loader.getController();

            menuController.loadData(account);
            setupMenuButtons();

            menuBar.getChildren().setAll(menuLayout);
	    AnchorPane.setTopAnchor(menuLayout, 0.0);
	    AnchorPane.setBottomAnchor(menuLayout, 0.0);
	    AnchorPane.setLeftAnchor(menuLayout, 0.0);
	    AnchorPane.setRightAnchor(menuLayout, 0.0);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initializeTopBar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/iuh/fit/view/features/TopPanel.fxml"));
            AnchorPane topLayout = loader.load();

            TopController topController = loader.getController();
            topController.initialize(this, mainStage);
            this.notificationButtonController = topController.getNotificationButtonController();

            RoomManagementService.startAutoCheckoutScheduler(notificationButtonController, this);

            topPanel.getChildren().setAll(topLayout);
            AnchorPane.setTopAnchor(topLayout, 0.0);
            AnchorPane.setBottomAnchor(topLayout, 0.0);
            AnchorPane.setLeftAnchor(topLayout, 0.0);
            AnchorPane.setRightAnchor(topLayout, 0.0);


            mainStage.setOnCloseRequest(e -> {
                e.consume();

                try {
                    topController.logout(true);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMenuButtons() {
        Position position = account.getEmployee().getPosition();
        informationBtn = menuController.getInformationBtn();
        handleTooltips();

        // Tắt các button menu không thuộc về lễ tân
        if (position.equals(Position.RECEPTIONIST)) {
            menuController.getEmployeeManagerButton().setDisable(true);
            menuController.getAccountOfEmployeeManagerButton().setDisable(true);
            menuController.getShiftManagerButton().setDisable(true);
            menuController.getEmployeeSearchingButton().setDisable(true);
            menuController.getPricingManagerButton().setDisable(true);
            menuController.getRoomCategoryManagerButton().setDisable(true);
            menuController.getRoomManagerButton().setDisable(true);
            menuController.getServiceCategoryManagerButton().setDisable(true);
            menuController.getHotelServiceManagerButton().setDisable(true);
//            menuController.getBackupBtn().setDisable(true);
        }
        
        if (position.equals(Position.MANAGER)) {
        	menuController.getRoomBookingButton().setDisable(true);
        } else {
        	menuController.getRoomBookingButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/RoomBookingPanel.fxml", this, account));
        }

        // xử lý sự kiện hiện giao diện cho cả lễ tân và quản lý
        // Dashboard
        menuController.getDashBoardBtn().setOnAction( e -> loadPanel("/iuh/fit/view/features/DashboardPanel.fxml", this, account) );
        // Room
        menuController.getRoomSearchingButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/RoomSearchingPanel.fxml", this, account));
        menuController.getRoomBookingButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/RoomBookingPanel.fxml", this, account));
        // Invoice
        menuController.getInvoiceBtn().setOnAction(event -> loadPanel("/iuh/fit/view/features/invoice/InvoiceManagerPanel.fxml", this, account));
        // Hotel Service
        menuController.getHotelServiceSearchingButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/service/HotelServiceSearchingPanel.fxml", this, account));
        // Customer
        menuController.getCustomerSearchingButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/customer/CustomerSearchingPanel.fxml", this, account));
        menuController.getCustomerManagerButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/customer/CustomerManagerPanel.fxml", this, account));
        // Statistics
        menuController.getRevenueStatisticsButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/statistics/RevenueStatisticalPanel.fxml", this, account));
        menuController.getRateUsingRoomButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/statistics/RateUsingRoomStatisticsTab.fxml", this, account));
        // Employee Information
        menuController.getEmployeeInformationContainer().setOnMouseClicked(event -> loadPanel("/iuh/fit/view/features/employee_information/EmployeeInformationPanel.fxml", this, account));

        informationBtn.setOnAction(event -> loadPanelInformation("/iuh/fit/view/features/InformationPanel.fxml"));


        // Thêm các sự kiện xử lý giao diện cho quản lý
        if (position.equals(Position.MANAGER)) {
            // Employee
            menuController.getEmployeeManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/employee/EmployeeManagerPanel.fxml", this, account));
            menuController.getAccountOfEmployeeManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/employee/AccountManagerPanel.fxml", this, account));
            menuController.getShiftManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/employee/ShiftManagerPanel.fxml", this, account));
            menuController.getEmployeeSearchingButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/employee/EmployeeSearchingPanel.fxml", this, account));
            // Room
            menuController.getPricingManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/PricingManagerPanel.fxml", this, account));
            menuController.getRoomCategoryManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/RoomCategoryManagerPanel.fxml", this, account));
            menuController.getRoomManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/room/RoomManagerPanel.fxml", this, account));
            // Hotel Service
            menuController.getServiceCategoryManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/service/ServiceCategoryManagerPanel.fxml", this, account));
            menuController.getHotelServiceManagerButton().setOnAction(event -> loadPanel("/iuh/fit/view/features/service/HotelServiceManagerPanel.fxml", this, account));
            // Statistics
            menuController.getRevenueStatisticsButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/statistics/RevenueStatisticalPanel.fxml", this, account));
            menuController.getRateUsingRoomButton().setOnAction(e -> loadPanel("/iuh/fit/view/features/statistics/RateUsingRoomStatisticsTab.fxml", this, account));
            // Settings

//            menuController.getBackupBtn().setOnAction(event -> loadPanel("/iuh/fit/view/features/backup_restore_database/Backup_Restore_Panel.fxml", this, account));
        }
    }

    private void handleTooltips() {
        // Tạo Tooltip
        Tooltip tooltip = new Tooltip("Về phần mềm");
        Tooltip.install(informationBtn, tooltip); // Gắn Tooltip vào Button

        // Thêm Tooltip bằng cách setTooltip
        informationBtn.setTooltip(tooltip);
        tooltip.setShowDelay(javafx.util.Duration.millis(400));
    }

    public void loadPanel(String fxmlPath, MainController mainController, Account account) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            Object controller = loader.getController();

            switch (controller) {
                case RoomBookingController roomBookingController ->
                        roomBookingController.setupContext(mainController, account.getEmployee(), notificationButtonController);

                case InvoiceManagerController invoiceManagerController -> {
                    Employee employee = EmployeeDAO.getEmployeeByAccountID(account.getAccountID());
                    invoiceManagerController.setupContext(mainController, employee, notificationButtonController);
                }

                case ShiftManagerController shiftManagerController -> {
                    Employee employee = EmployeeDAO.getEmployeeByAccountID(account.getAccountID());
                    shiftManagerController.setupContext(employee);
                    shiftManagerController.setUpCurrentShift(shift);
                }

                case EmployeeInformationController employeeInformationController ->
                        employeeInformationController.setupContext(account.getEmployee(), mainController);

                case DashboardController dashboardController ->
                        dashboardController.setupContext(account, mainController);

                case EmployeeSearchingController employeeSearchingController ->
                        employeeSearchingController.setupContext(this);

                case HotelServiceSearchingController hotelServiceSearchingController ->
                        hotelServiceSearchingController.setupContext(this, account);

                case CustomerSearchingController customerSearchingController ->
                        customerSearchingController.setupContext(this, account);

                case RoomSearchingController roomSearchingController ->
                        roomSearchingController.setupContext(this, account);

                default -> {}
            }

            // Remove weird timeline
            if (!fxmlPath.contains("RoomBookingPanel") && !fxmlPath.contains("DashBoardPanel")) {
                TimelineManager.getInstance().removeTimeline("REALTIME_DASHBOARD");
                TimelineManager.getInstance().stopAllTimelines();
            }
            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");

            // 🔥 FIX CSS HERE
            layout.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/iuh/fit/styles/Dashboard.css")).toExternalForm()
            );

            // 🔥 ALWAYS ADD ROOT (not children)
            mainPanel.getChildren().setAll(layout);
            AnchorPane.setTopAnchor(layout, 0.0);
            AnchorPane.setBottomAnchor(layout, 0.0);
            AnchorPane.setLeftAnchor(layout, 0.0);
            AnchorPane.setRightAnchor(layout, 0.0);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void loadPanelEmployeeManagerController(String fxmlPath, Employee emp){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            EmployeeManagerController controller = loader.getController();

            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");

            mainPanel.getChildren().setAll(layout);
            AnchorPane.setTopAnchor(layout, 0.0);
            AnchorPane.setBottomAnchor(layout, 0.0);
            AnchorPane.setLeftAnchor(layout, 0.0);
            AnchorPane.setRightAnchor(layout, 0.0);


            Platform.runLater(() -> controller.setInformation(emp));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPanelRoomManagerController(String fxmlPath, Room room){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            RoomManagerController controller = loader.getController();

            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");

            mainPanel.getChildren().clear();
            mainPanel.getChildren().addAll(layout.getChildren());

            Platform.runLater(() -> controller.setInformation(room));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPanelCreateReservationFormController(String fxmlPath, MainController mainController, Account account, RoomWithReservation room){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            CreateReservationFormController controller = loader.getController();

            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");

            mainPanel.getChildren().clear();
            mainPanel.getChildren().addAll(layout.getChildren());
            MainController.setRoomBookingLoaded(false);
            Platform.runLater(() -> controller.setupContext(
                    mainController, account.getEmployee(), room,
                    null, null, null, notificationButtonController
            ));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPanelHotelServiceManagerController(String fxmlPath, HotelService service){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            HotelServiceManagerController controller = loader.getController();

            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");
            mainPanel.getChildren().clear();
            mainPanel.getChildren().addAll(layout.getChildren());
            Platform.runLater(() -> controller.setInformation(service));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPanelCustomerManagerController(String fxmlPath, Customer customer){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            CustomerManagerController controller = loader.getController();

            ROOM_BOOKING_LOADED = fxmlPath.contains("RoomBookingPanel");
            mainPanel.getChildren().clear();
            mainPanel.getChildren().addAll(layout.getChildren());
            Platform.runLater(() -> controller.setInformation(customer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPanelInformation(String fxmlPath){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane layout = loader.load();

            mainPanel.getChildren().clear();
            mainPanel.getChildren().addAll(layout.getChildren());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AnchorPane getMainPanel() {
        return mainPanel;
    }

    public Account getAccount() {
        return account;
    }

    public Shift getShift(){
        return shift;
    }

    public int getShiftDetailID(){
        return shiftDetailID;
    }

    public static boolean isRoomBookingLoaded() {
        return ROOM_BOOKING_LOADED;
    }

    public static void setRoomBookingLoaded(boolean roomBookingLoaded) {
        ROOM_BOOKING_LOADED = roomBookingLoaded;
    }

}
