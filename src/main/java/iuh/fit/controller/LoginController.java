package iuh.fit.controller;

import com.dlsc.gemsfx.DialogPane;
import iuh.fit.dao.AccountDAO;
import iuh.fit.dao.EmployeeDAO;
import iuh.fit.dao.ShiftDAO;
import iuh.fit.models.Account;
import iuh.fit.models.Employee;
import iuh.fit.models.Shift;
import iuh.fit.models.enums.AccountStatus;
import iuh.fit.models.enums.Position;
import iuh.fit.utils.*;
import iuh.fit.security.PasswordHashing;
import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.Objects;

public class LoginController {

    @FXML private TextField userNameField;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button ShowPasswordBtn;
    @FXML private Text errorMessage;
    @FXML private Button signInButton;
    @FXML private ImageView showPassButton;
    @FXML private DialogPane dialogPane;
    @FXML private Text forgotPasswordBtn;
    @FXML private Label loginBtn;
    @FXML private Button confirmBtn;
    @FXML private Button resetBtn;

    @FXML private GridPane loginGrid;
    @FXML private GridPane forgotPasswordGrid;

    @FXML private TextField employeeIDTextField;
    @FXML private TextField fullNameTextField;
    @FXML private TextField phoneNumberTextField;
    @FXML private TextField cardIDTextField;
    @FXML private TextField emailTextField;
    @FXML private TextField usernameTextField;

    private boolean isDefaultIcon = true;
    private Stage mainStage;

    @FXML
    public void initialize() {
        dialogPane.toFront();

        hiddenPasswordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());

        ShowPasswordBtn.setOnAction(event -> {
            PasswordVisibility();
            changeButtonIconForShowPasswordBtn();
        });

        Image defaultIcon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/iuh/fit/icons/login_panel_icons/ic_show_password.png")));
        showPassButton.setImage(defaultIcon);

        forgotPasswordBtn.setOnMouseClicked(event -> forgotPass());
        loginBtn.setOnMouseClicked(event -> login());
        confirmBtn.setOnAction(event -> changePassword());
        resetBtn.setOnAction(event -> resetAction());
    }

    public void setupContext(Stage mainStage) {
        this.mainStage = mainStage;

        signInButton.setOnAction(event -> {
            try {
                signIn(this.mainStage);
            } catch (SQLException ignored) {}
        });

        registerEventEnterKey();
    }

    private void registerEventEnterKey() {
        userNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) try { signIn(mainStage); } catch (SQLException ignored) {}
        });
        hiddenPasswordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) try { signIn(mainStage); } catch (SQLException ignored) {}
        });
        visiblePasswordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) try { signIn(mainStage); } catch (SQLException ignored) {}
        });
    }

    @FXML
    private void changeButtonIconForShowPasswordBtn() {
        if (isDefaultIcon) {
            Image newIcon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/iuh/fit/icons/login_panel_icons/ic_hidden_password.png")));
            showPassButton.setImage(newIcon);
        } else {
            Image defaultIcon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/iuh/fit/icons/login_panel_icons/ic_show_password.png")));
            showPassButton.setImage(defaultIcon);
        }
        isDefaultIcon = !isDefaultIcon;
    }

    private void PasswordVisibility() {
        if (hiddenPasswordField.isVisible()) {
            hiddenPasswordField.setVisible(false);
            hiddenPasswordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
        } else {
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            hiddenPasswordField.setVisible(true);
            hiddenPasswordField.setManaged(true);
        }
    }

    private void signIn(Stage mainStage) throws SQLException {

        if (!RestoreDatabase.isDatabaseExist(DBHelper.getDatabaseName())) {
            errorMessage.setText(ErrorMessages.DATABASE_NOT_FOUND);
            return;
        }

        String userName = userNameField.getText();
        String password = hiddenPasswordField.getText();

        if (userName.isEmpty()) {
            errorMessage.setText(ErrorMessages.LOGIN_INVALID_USERNAME);
            return;
        }
        if (password.isEmpty()) {
            errorMessage.setText(ErrorMessages.LOGIN_INVALID_PASSWORD);
            return;
        }

        Account account = AccountDAO.getLogin(userName, password);
        if (account == null) {
            errorMessage.setText(ErrorMessages.LOGIN_INVALID_ACCOUNT);
            return;
        }

        if (account.getAccountStatus().equals(AccountStatus.INACTIVE)
                || account.getAccountStatus().equals(AccountStatus.LOCKED)) {
            dialogPane.showInformation(
                    "Thông báo",
                    "Tài khoản bị khóa hoặc không có hiệu lực.\nVui lòng báo quản lý."
            );
            return;
        }

        Position position = account.getEmployee().getPosition();
        Shift currentShift = ShiftDAO.getCurrentShiftForLogin(account.getEmployee());

        if (position.equals(Position.RECEPTIONIST)) {
            if (currentShift == null) {
                dialogPane.showInformation("Thông báo", "Không thuộc ca làm việc hiện tại\nKhông thể đăng nhập.");
            } else {
                loadMainUI(account, currentShift, mainStage);
            }
        } else {
            loadMainUI(account, currentShift, mainStage);
        }
    }

    private void loadMainUI(Account account, Shift currentShift, Stage mainStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/iuh/fit/view/ui/MainUI.fxml"));
            AnchorPane mainPanel = loader.load();

            MainController mainController = loader.getController();
            mainController.initialize(account, mainStage, currentShift);

            Scene scene = new Scene(mainPanel);
            mainStage.setWidth(1200);
            mainStage.setHeight(680);
            mainStage.setScene(scene);
            mainStage.setMaximized(true);
            mainStage.show();

        } catch (Exception e) {
            errorMessage.setText(e.getMessage());
        }
    }

    private void forgotPass() {
        slideOutGridFromBot(loginGrid, forgotPasswordGrid);
    }

    private void login() {
        slideOutGridFromTop(forgotPasswordGrid, loginGrid);
    }

    public void slideOutGridFromBot(GridPane out, GridPane in) {
        in.setVisible(true);
        in.setTranslateY(out.getHeight());
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(500), out);
        slideOut.setToY(-out.getHeight());
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), in);
        slideIn.setFromY(out.getHeight());
        slideIn.setToY(0);
        new ParallelTransition(slideOut, slideIn).play();
        slideOut.setOnFinished(e -> out.setVisible(false));
    }

    public void slideOutGridFromTop(GridPane out, GridPane in) {
        in.setVisible(true);
        in.setTranslateY(-out.getHeight());
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(500), out);
        slideOut.setToY(out.getHeight());
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), in);
        slideIn.setFromY(-out.getHeight());
        slideIn.setToY(0);
        new ParallelTransition(slideOut, slideIn).play();
        slideOut.setOnFinished(e -> out.setVisible(false));
    }

    private void changePassword() {
        String employeeID = employeeIDTextField.getText();
        String fullName = fullNameTextField.getText();
        String phoneNumber = phoneNumberTextField.getText();
        String cardID = cardIDTextField.getText();
        String email = emailTextField.getText();
        String username = usernameTextField.getText();

        if (employeeID.isBlank() || fullName.isBlank() || phoneNumber.isBlank()
                || cardID.isBlank() || email.isBlank() || username.isBlank()) {
            dialogPane.showWarning("Cảnh báo", "Bạn phải nhập đầy đủ thông tin.");
            return;
        }

        Employee employee = EmployeeDAO.getEmployeeByEmployeeID(employeeID);
        Account account = AccountDAO.getAccountByEmployeeID(employeeID);

        if (employee == null || account == null) {
            dialogPane.showWarning("Cảnh báo", "Thông tin chưa chính xác.");
            return;
        }

        if (!employee.getFullName().equals(fullName)
                || !employee.getPhoneNumber().equals(phoneNumber)
                || !employee.getIdCardNumber().equals(cardID)
                || !employee.getEmail().equals(email)
                || !account.getUserName().equals(username)) {

            dialogPane.showWarning("Cảnh báo", "Thông tin chưa chính xác.");
            return;
        }

        // Dialog đổi mật khẩu
        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 10, 10, 10));
        Label successLabel = new Label("Xác thực thành công");
        successLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nhập mật khẩu mới");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Xác nhận mật khẩu");
        content.getChildren().addAll(
                successLabel,
                new VBox(5, new Label("Mật khẩu mới:"), newPasswordField),
                new VBox(5, new Label("Xác nhận mật khẩu:"), confirmPasswordField)
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật mật khẩu");
        dialog.getDialogPane().setContent(content);

        ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(confirmButton);

        Button confirmBtnDialog = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        confirmBtnDialog.addEventFilter(ActionEvent.ACTION, e -> {
            String newPass = newPasswordField.getText();
            String confirmPass = confirmPasswordField.getText();

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng nhập đầy đủ thông tin").showAndWait();
                e.consume();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                new Alert(Alert.AlertType.WARNING, "Mật khẩu xác nhận không khớp").showAndWait();
                e.consume();
                return;
            }

            if (!RegexChecker.isValidPassword(newPass)) {
                new Alert(Alert.AlertType.WARNING,
                        "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ, số và ký tự đặc biệt.")
                        .showAndWait();
                e.consume();
                return;
            }

            String hashed = PasswordHashing.hashPassword(newPass);
            if (hashed.equals(account.getPassword())) {
                new Alert(Alert.AlertType.WARNING, "Mật khẩu mới phải khác mật khẩu cũ").showAndWait();
                e.consume();
                return;
            }

            account.setPassword(hashed);
            AccountDAO.updateData(account);
            new Alert(Alert.AlertType.INFORMATION, "Cập nhật thành công!").showAndWait();
            resetAction();
        });

        dialog.showAndWait();
    }

    private void resetAction() {
        employeeIDTextField.setText("");
        fullNameTextField.setText("");
        phoneNumberTextField.setText("");
        cardIDTextField.setText("");
        emailTextField.setText("");
        usernameTextField.setText("");
    }
}
