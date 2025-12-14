package iuh.fit.controller.features.invoice;

import com.dlsc.gemsfx.DialogPane;
import iuh.fit.controller.MainController;
import iuh.fit.controller.features.NotificationButtonController;
import iuh.fit.dao.HistoryCheckOutDAO;
import iuh.fit.dao.HistoryCheckinDAO;
import iuh.fit.dao.RoomUsageServiceDAO;
import iuh.fit.models.*;
import iuh.fit.utils.Calculator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class InvoiceDetailsController {

    @FXML
    private Label roomNumberLabel, roomCategoryLabel, checkInDateLabel,
            checkOutDateLabel, stayLengthLabel;

    @FXML
    private Label customerIDLabel, customerFullnameLabel,
            cusomerPhoneNumberLabel, customerEmailLabel,
            customerIDCardNumberLabel;

    @FXML
    private Text totalServiceChargeText, totalRoomChargeText,
            totalRoomDepositeText, totalDueText, taxText,
            netDueText, invoiceTitleText, remaningDueText;

    @FXML
    private TableView<RoomUsageService> roomUsageServiceTableView;
    @FXML
    private TableColumn<RoomUsageService, String> roomUsageServiceIDColumn;
    @FXML
    private TableColumn<RoomUsageService, String> serviceNameColumn;
    @FXML
    private TableColumn<RoomUsageService, Integer> quantityColumn;
    @FXML
    private TableColumn<RoomUsageService, Double> unitPriceColumn;
    @FXML
    private TableColumn<RoomUsageService, Double> totalPriceColumn;
    @FXML
    private TableColumn<RoomUsageService, String> dateAddedColumn;
    @FXML
    private TableColumn<RoomUsageService, String> employeeAddedColumn;
    @FXML
    private TitledPane titledPane;
    @FXML
    private DialogPane dialogPane;
    @FXML
    private Button exportPDFBtn;
    @FXML
    private Button backBtn;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm", Locale.forLanguageTag("vi-VN"));

    // Context
    private MainController mainController;
    private Employee employee;
    private Invoice invoice;
    private NotificationButtonController notificationButtonController;

    // ==================================================================================================================
    // 2. Khởi tạo và nạp dữ liệu vào giao diện
    // ==================================================================================================================
    public void initialize() {
        setupRoomUsageServiceTableView();
    }

    public void setupContext(MainController mainController, Employee employee,
            Invoice invoice, NotificationButtonController notificationButtonController) {

	this.mainController = mainController;
	this.employee = employee;
	this.invoice = invoice;
	this.notificationButtonController = notificationButtonController;
	
	titledPane.setText("Quản Lý Hóa Đơn " + invoice.getInvoiceID());
	
	setupButtonActions();
	setupReservationForm();
	loadData();
	setupPaymentSummary();
	
	// 🎯 FIX NÚT QUAY LẠI
	backBtn.setOnAction(e -> navigateToInvoiceManagerPanel());
	}


    private void loadData() {
        List<RoomUsageService> roomUsageServices = RoomUsageServiceDAO.getByReservationFormID(invoice.getReservationForm().getReservationID());
        ObservableList<RoomUsageService> roomUsageServicesData = FXCollections.observableArrayList(roomUsageServices);
        roomUsageServiceTableView.setItems(roomUsageServicesData);
        roomUsageServiceTableView.refresh();
    }

    //In Hóa Đơn 
    private void setupButtonActions() {
        exportPDFBtn.setOnAction(event -> openElectronicInvoice());
    }

    
    //Xem hóa đơn điện tử
    private void openElectronicInvoice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/iuh/fit/view/features/invoice/ElectronicInvoiceView.fxml")
            );
            AnchorPane layout = loader.load();

            ElectronicInvoiceController controller = loader.getController();

            DialogPane.Dialog dialog = dialogPane.showNode(
                    DialogPane.Type.BLANK,
                    "Hóa Đơn Điện Tử",
                    layout
            );

            controller.setDialog(dialog);
            controller.setDialogPane(dialogPane);  // << BẮT BUỘC PHẢI CÓ
            controller.setupData(invoice);

        } catch (Exception e) {
            dialogPane.showError("LỖI", "Không thể mở hóa đơn điện tử:\n" + e.getMessage());
            e.printStackTrace();
        }
    }



    private void setupReservationForm() {
        ReservationForm reservationForm = invoice.getReservationForm();

        Room reservationFormRoom = reservationForm.getRoom();
        Customer reservationFormCustomer = reservationForm.getCustomer();

        LocalDateTime actualCheckInDate = HistoryCheckinDAO.getActualCheckInDate(reservationForm.getReservationID());
        LocalDateTime actualCheckOutDate = HistoryCheckOutDAO.getActualCheckOutDate(reservationForm.getReservationID());

        roomNumberLabel.setText(reservationFormRoom.getRoomNumber());
        roomCategoryLabel.setText(reservationFormRoom.getRoomCategory().getRoomCategoryName());
        checkInDateLabel.setText(dateTimeFormatter.format(actualCheckInDate != null ? actualCheckInDate : reservationForm.getCheckInDate()));
        checkOutDateLabel.setText(dateTimeFormatter.format(actualCheckOutDate != null ? actualCheckOutDate : reservationForm.getCheckOutDate()));
        stayLengthLabel.setText(Calculator.calculateStayLengthToString(
                reservationForm.getCheckInDate(),
                reservationForm.getCheckOutDate()
        ));
        customerIDLabel.setText(reservationFormCustomer.getCustomerID());
        customerFullnameLabel.setText(reservationFormCustomer.getFullName());
        cusomerPhoneNumberLabel.setText(reservationFormCustomer.getPhoneNumber());
        customerEmailLabel.setText(reservationFormCustomer.getEmail());
        customerIDCardNumberLabel.setText(reservationFormCustomer.getIdCardNumber());
    }

    private void setupPaymentSummary() {
        totalServiceChargeText.setText(String.format("%,.0f", invoice.getServicesCharge()));
        totalRoomChargeText.setText(String.format("%,.0f", invoice.getRoomCharge()));
        double depositAmount = invoice.getReservationForm().getRoomBookingDeposit();
        totalRoomDepositeText.setText("-" + String.format("%,.0f", depositAmount));
        totalDueText.setText(String.format("%,.0f", invoice.getTotalDue()));
        double taxAmount = invoice.getTotalDue() * 0.1;
        invoiceTitleText.setText("Thuế (10%)" );
        taxText.setText(String.format("%,.0f", taxAmount));
        netDueText.setText(String.format("%,.0f", invoice.getNetDue()));
        remaningDueText.setText(String.format("%,.0f", invoice.getNetDue() - depositAmount));
    }

    // ==================================================================================================================
    // 2. Xử lý chức năng hiển thị panel khác
    // ==================================================================================================================
    private void navigateToInvoiceManagerPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/iuh/fit/view/features/invoice/InvoiceManagerPanel.fxml"));
            AnchorPane layout = loader.load();

            InvoiceManagerController invoiceManagerController = loader.getController();
            invoiceManagerController.setupContext(mainController, employee, notificationButtonController);


            mainController.getMainPanel().getChildren().clear();
            mainController.getMainPanel().getChildren().addAll(layout.getChildren());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================================================================================================================
    // 3. Setup table lịch sử dùng dịch vụ
    // ==================================================================================================================
    private void setupRoomUsageServiceTableView() {
        roomUsageServiceIDColumn.setCellValueFactory(new PropertyValueFactory<>("roomUsageServiceId"));
        serviceNameColumn.setCellValueFactory(data -> {
            HotelService service = data.getValue().getHotelService();
            String serviceName = (service != null && service.getServiceName() != null) ? service.getServiceName() : "KHÔNG CÓ";
            return new SimpleStringProperty(serviceName);
        });
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalPriceColumn.setCellValueFactory(data -> {
            double totalPrice = data.getValue().getQuantity() * data.getValue().getUnitPrice();
            return new SimpleDoubleProperty(totalPrice).asObject();
        });
        dateAddedColumn.setCellValueFactory(data -> {
            LocalDateTime dateAdded = data.getValue().getDateAdded();
            String formattedDate = (dateAdded != null) ? dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "Không có";
            return new SimpleStringProperty(formattedDate);
        });

        employeeAddedColumn.setCellValueFactory(data -> {
            Employee employee = data.getValue().getEmployee();
            String employeeName = (employee != null && employee.getFullName() != null) ? employee.getFullName() : "Không có";
            return new SimpleStringProperty(employeeName);
        });
    }

}
