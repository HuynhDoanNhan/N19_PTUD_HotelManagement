package iuh.fit.controller.features.invoice;

import javafx.scene.control.Button;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.dlsc.gemsfx.DialogPane;   
import iuh.fit.dao.HistoryCheckOutDAO;
import iuh.fit.dao.HistoryCheckinDAO;
import iuh.fit.dao.RoomUsageServiceDAO;
import iuh.fit.models.HotelService;
import iuh.fit.models.Invoice;
import iuh.fit.models.RoomUsageService;
import iuh.fit.utils.QRCodeHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;
//Thêm qr
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import java.awt.image.BufferedImage;

//Thêm qr


public class ElectronicInvoiceController {

    @FXML
    private Label customerNameText, roomNumberText, checkInText, checkOutText, totalAmountText;

    @FXML
    private TableView<RoomUsageService> serviceTable;

    @FXML
    private TableColumn<RoomUsageService, String> colDetail;

    @FXML
    private TableColumn<RoomUsageService, Double> colAmount;

    @FXML
    private Button confirmBtn;
  //Thêm qr
    @FXML
    private ImageView qrImageView;
  //Thêm qr

    private Invoice invoice;

    private DialogPane dialogPane; 
    private DialogPane.Dialog dialog;

    public void setDialog(DialogPane.Dialog dialog) {
        this.dialog = dialog;
    }

    public void setDialogPane(DialogPane dialogPane) {
        this.dialogPane = dialogPane;
    }

    public void setupData(Invoice invoice) {
        this.invoice = invoice;

        // === QR CODE ===
        String qrContent =
                "InvoiceID: " + invoice.getInvoiceID() + "\n" +
                "Customer: " + invoice.getReservationForm().getCustomer().getFullName() + "\n" +
                "Total: " + invoice.getNetDue() + " VND";

        BufferedImage qr = QRCodeHelper.generateQRCode(qrContent, 150, 150);
        if (qr != null) {
            Image fxImg = SwingFXUtils.toFXImage(qr, null);
            qrImageView.setImage(fxImg);
        }

        customerNameText.setText(invoice.getReservationForm().getCustomer().getFullName());
        roomNumberText.setText(invoice.getReservationForm().getRoom().getRoomNumber());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        LocalDateTime checkIn = HistoryCheckinDAO.getActualCheckInDate(invoice.getReservationForm().getReservationID());
        LocalDateTime checkOut = HistoryCheckOutDAO.getActualCheckOutDate(invoice.getReservationForm().getReservationID());

        checkInText.setText(checkIn != null ? formatter.format(checkIn) :
                formatter.format(invoice.getReservationForm().getCheckInDate()));

        checkOutText.setText(checkOut != null ? formatter.format(checkOut) :
                formatter.format(invoice.getReservationForm().getCheckOutDate()));

        // 🎯 === FIX: TRỪ TIỀN CỌC ===
        double depositAmount = invoice.getReservationForm().getRoomBookingDeposit();
        double finalAmount = invoice.getNetDue() - depositAmount;

        if (finalAmount < 0) finalAmount = 0;

        totalAmountText.setText(String.format("%,.0f VND", finalAmount));

        // Table dịch vụ
        colDetail.setCellValueFactory(data -> {
            HotelService s = data.getValue().getHotelService();
            String name = (s != null && s.getServiceName() != null) ? s.getServiceName() : "Không có";
            return new SimpleStringProperty(name);
        });
        colAmount.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getQuantity() * data.getValue().getUnitPrice()
                ).asObject()
        );

        List<RoomUsageService> list =
                RoomUsageServiceDAO.getByReservationFormID(invoice.getReservationForm().getReservationID());

        if (list != null) {
            serviceTable.getItems().setAll(list);
        }

        // Close dialog
        confirmBtn.setOnAction(e -> {
            if (dialog != null) {
                dialogPane.getDialogs().remove(dialog);
            }
        });
    }

    public BufferedImage generateQRCode(String text, int width, int height) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }


}
