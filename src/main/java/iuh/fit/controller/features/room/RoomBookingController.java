package iuh.fit.controller.features.room;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import java.util.ArrayList;
import java.util.Map;
import com.dlsc.gemsfx.DialogPane;
import iuh.fit.controller.MainController;
import iuh.fit.controller.features.NotificationButtonController;
import iuh.fit.controller.features.room.creating_reservation_form_controllers.RoomAvailableItemController;
import iuh.fit.controller.features.room.creating_reservation_form_controllers.RoomOnUseItemController;
import iuh.fit.controller.features.room.creating_reservation_form_controllers.RoomOverDueController;
import iuh.fit.dao.RoomCategoryDAO;
import iuh.fit.dao.RoomDAO;
import iuh.fit.dao.RoomWithReservationDAO;
import iuh.fit.models.Employee;
import iuh.fit.models.Room;
import iuh.fit.models.enums.RoomStatus;
import iuh.fit.models.wrapper.RoomWithReservation;
import iuh.fit.utils.RoomManagementService;
import iuh.fit.utils.TimelineManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static iuh.fit.utils.GlobalConstants.*;

public class RoomBookingController {
    @FXML
    private GridPane roomGridPane;

    @FXML
    private ComboBox<String> roomCategoryCBox, roomFloorNumberCBox;

    @FXML
    private Button allBtn, availableBtn,
            onUseBtn, overDueBtn;

    @FXML
    private DialogPane dialogPane;
    
    @FXML
    private TextField guestCountField;

    @FXML
    private Button suggestButton;

    @FXML
    private RadioButton priorityVipRadio, priorityNormalRadio, priorityAllRadio;

    // Lưu map Pane phòng ↔ RoomWithReservation để tiện tô viền
    private Map<Pane, RoomWithReservation> roomPaneMap = new HashMap<>();
    
    private enum RoomPriority {
        VIP, NORMAL, ALL
    }

    private RoomPriority getCurrentPriority() {
        if (priorityVipRadio != null && priorityVipRadio.isSelected()) {
            return RoomPriority.VIP;
        }
        if (priorityNormalRadio != null && priorityNormalRadio.isSelected()) {
            return RoomPriority.NORMAL;
        }
        return RoomPriority.ALL;
    }

    private boolean isVip(Room room) {
        String name = room.getRoomCategory().getRoomCategoryName().toUpperCase();
        return name.contains("VIP");
    }

    private boolean matchesPriority(Room room, RoomPriority priority) {
        return switch (priority) {
            case VIP -> isVip(room);
            case NORMAL -> !isVip(room); // coi tất cả phòng không chứa "VIP" là thường
            case ALL -> true;
        };
    }

    // Sức chứa của phòng: 1 giường = 2 người, 2 giường = 4 người
    private int getCapacity(Room room) {
        int beds = room.getRoomCategory().getNumberOfBed();
        return beds * 2;
    }

    private void clearSuggestionsHighlight() {
        for (Pane pane : roomPaneMap.keySet()) {
            pane.getStyleClass().remove("room-suggested");
        }
    }


    private List<RoomWithReservation> roomWithReservations;
    private MainController mainController;
    private Employee employee;

    private Button activeButton;
    private RoomStatus selectedStatus = null;

    private NotificationButtonController notificationButtonController;

    public void initialize() {
        dialogPane.toFront();
        activeButton = allBtn;
        setActiveButtonStyle(allBtn);
        MainController.setRoomBookingLoaded(true);
        if (priorityAllRadio != null) {
            priorityAllRadio.setSelected(true);
        }

        if (priorityVipRadio != null) {
            priorityVipRadio.setOnAction(e -> {
                priorityVipRadio.setSelected(true);
                priorityNormalRadio.setSelected(false);
                priorityAllRadio.setSelected(false);
            });
        }
        if (priorityNormalRadio != null) {
            priorityNormalRadio.setOnAction(e -> {
                priorityVipRadio.setSelected(false);
                priorityNormalRadio.setSelected(true);
                priorityAllRadio.setSelected(false);
            });
        }
        if (priorityAllRadio != null) {
            priorityAllRadio.setOnAction(e -> {
                priorityVipRadio.setSelected(false);
                priorityNormalRadio.setSelected(false);
                priorityAllRadio.setSelected(true);
            });
        }

        if (suggestButton != null) {
            suggestButton.setOnAction(e -> suggestRooms());
        }
        if (guestCountField != null) {
            guestCountField.setOnAction(e -> suggestRooms()); // nhấn Enter
        }

    }

    public void setupContext(MainController mainController, Employee employee,
                             NotificationButtonController notificationButtonController) {
        this.mainController = mainController;
        this.employee = employee;
        this.notificationButtonController = notificationButtonController;
        loadData();
        setupEventHandlers();

    }

    private List<String> getRoomCategories() {
        List<String> roomCategoryList = RoomCategoryDAO.getRoomCategory().stream()
                .map(rc -> rc.getRoomCategoryID() + " " + rc.getRoomCategoryName())
                .collect(Collectors.toList());
        roomCategoryList.addFirst("TẤT CẢ");
        return roomCategoryList;
    }

    private List<String> getFloorNumbers() {
        return List.of("TẤT CẢ", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    }

    private void loadData() {
        Task<List<RoomWithReservation>> loadDataTask = new Task<>() {
            @Override
            protected List<RoomWithReservation> call() {
                List<String> roomCategories = getRoomCategories();
                List<String> floorNumbers = getFloorNumbers();

                Platform.runLater(() -> {
                    roomCategoryCBox.getItems().setAll(roomCategories);
                    roomCategoryCBox.getSelectionModel().selectFirst();

                    roomFloorNumberCBox.getItems().setAll(floorNumbers);
                    roomFloorNumberCBox.getSelectionModel().selectFirst();
                });

                RoomManagementService.autoCheckoutOverdueRooms(notificationButtonController, mainController);
                return RoomWithReservationDAO.getRoomWithReservation().stream()
                        .sorted(Comparator.comparing(r -> r.getRoom().getRoomNumber()))
                        .toList();
            }
        };

        loadDataTask.setOnSucceeded(event -> {
            roomWithReservations = loadDataTask.getValue();
            displayFilteredRooms(roomWithReservations);
            loadDataForBtn();
            TimelineManager.getInstance().printAllTimelines();
        });

        loadDataTask.setOnFailed(event -> {
            throw new IllegalArgumentException(loadDataTask.getException().getMessage());
        });

        new Thread(loadDataTask).start();
    }

    private void loadDataForBtn() {
        HashMap<RoomStatus, Integer> roomStatusCount = RoomDAO.getRoomStatusCount();
        availableBtn.setText(ROOM_BOOKING_AVAIL_BTN + "("+ roomStatusCount.get(RoomStatus.AVAILABLE) + ")");
        onUseBtn.setText(ROOM_BOOKING_ON_USE_BTN + "("+ roomStatusCount.get(RoomStatus.ON_USE) + ")");
        overDueBtn.setText(ROOM_BOOKING_OVER_DUE_BTN + "("+ roomStatusCount.get(RoomStatus.OVERDUE) + ")");
        allBtn.setText(ROOM_BOOKING_ALL_BTN + "("+roomWithReservations.size()+")");
    }

//    private void displayFilteredRooms(List<RoomWithReservation> roomsWithReservations) {
//        roomGridPane.getChildren().clear();
//
//        int row = 0, col = 0;
//
//        try {
//            for (RoomWithReservation roomWithReservation : roomsWithReservations) {
//                Pane roomItem = loadRoomItem(roomWithReservation);
//
//                roomGridPane.add(roomItem, col, row);
//
//                col++;
//                if (col == 3) {
//                    col = 0;
//                    row++;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    
    private void displayFilteredRooms(List<RoomWithReservation> roomsWithReservations) {
        roomGridPane.getChildren().clear();
        roomPaneMap.clear();          // reset map cũ
        clearSuggestionsHighlight();  // bỏ viền cũ nếu còn

        int row = 0, col = 0;

        try {
            for (RoomWithReservation roomWithReservation : roomsWithReservations) {
                Pane roomItem = loadRoomItem(roomWithReservation);

                // Lưu map Pane ↔ RoomWithReservation
                roomPaneMap.put(roomItem, roomWithReservation);

                roomGridPane.add(roomItem, col, row);

                col++;
                if (col == 3) {
                    col = 0;
                    row++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Pane loadRoomItem(RoomWithReservation roomWithReservation) throws IOException {
        FXMLLoader loader;
        Pane roomItem;

        Room room = roomWithReservation.getRoom();

        switch (room.getRoomStatus()) {
            case AVAILABLE -> {
                loader = new FXMLLoader(getClass().getResource(
                        "/iuh/fit/view/features/room/creating_reservation_form_panels/RoomAvailableItem.fxml"));
                roomItem = loader.load();

                RoomAvailableItemController controller = loader.getController();
                controller.setupContext(mainController, employee, roomWithReservation, notificationButtonController);
            }
            case ON_USE -> {
                loader = new FXMLLoader(getClass().getResource(
                        "/iuh/fit/view/features/room/creating_reservation_form_panels/RoomOnUseItem.fxml"));
                roomItem = loader.load();

                RoomOnUseItemController controller = loader.getController();
                controller.setupContext(mainController, employee, roomWithReservation, notificationButtonController);
            }
            case OVERDUE -> {
                loader = new FXMLLoader(getClass().getResource(
                        "/iuh/fit/view/features/room/creating_reservation_form_panels/RoomOverDueItem.fxml"));
                roomItem = loader.load();

                RoomOverDueController controller = loader.getController();
                controller.setupContext(mainController, employee, roomWithReservation, notificationButtonController);
            }
            default -> throw new IllegalStateException("Unexpected value: " + room.getRoomStatus());
        }
        return roomItem;
    }
    private void suggestRooms() {
        if (roomPaneMap.isEmpty()) return;

        String text = guestCountField.getText().trim();
        if (text.isBlank()) {
            dialogPane.showInformation("Thông báo", "Vui lòng nhập số lượng khách.");
            return;
        }

        int guestCount;
        try {
            guestCount = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            dialogPane.showInformation("Thông báo", "Số lượng khách phải là số nguyên dương.");
            return;
        }

        if (guestCount <= 0) {
            dialogPane.showInformation("Thông báo", "Số lượng khách phải lớn hơn 0.");
            return;
        }

        clearSuggestionsHighlight();

        RoomPriority priority = getCurrentPriority();

        List<Map.Entry<Pane, RoomWithReservation>> rooms = new ArrayList<>(roomPaneMap.entrySet().stream()
                .filter(e -> e.getValue().getRoom().getRoomStatus() == RoomStatus.AVAILABLE)
                .filter(e -> matchesPriority(e.getValue().getRoom(), priority))
                .toList());

        if (rooms.isEmpty()) {
            dialogPane.showInformation("Thông báo", "Không có phòng trống phù hợp.");
            return;
        }

        List<Pane> selected = new ArrayList<>();
        int remaining = guestCount;

        // Tách riêng phòng đôi và phòng đơn
        List<Map.Entry<Pane, RoomWithReservation>> doubleRooms = new ArrayList<>();
        List<Map.Entry<Pane, RoomWithReservation>> singleRooms = new ArrayList<>();

        for (var e : rooms) {
            int cap = getCapacity(e.getValue().getRoom());
            if (cap == 4) doubleRooms.add(e);
            else if (cap == 2) singleRooms.add(e);
        }

        boolean hasUpgrade = false; // Đánh dấu có nâng cấp không

        // === 1-2 khách: ưu tiên đơn → hết thì nâng cấp lên đôi ===
        if (remaining <= 2) {
            if (!singleRooms.isEmpty()) {
                selected.add(singleRooms.remove(0).getKey());
            } else if (!doubleRooms.isEmpty()) {
                selected.add(doubleRooms.remove(0).getKey());
                hasUpgrade = true;
            }
        }
        // === 3-4 khách: chỉ dùng đôi ===
        else if (remaining <= 4) {
            if (!doubleRooms.isEmpty()) {
                selected.add(doubleRooms.remove(0).getKey());
            }
        }
        // === Nhiều hơn 4 khách: ưu tiên dùng đôi tối đa ===
        else {
            // Dùng càng nhiều phòng đôi càng tốt
            while (remaining >= 3 && !doubleRooms.isEmpty()) {
                selected.add(doubleRooms.remove(0).getKey());
                remaining -= 4;
            }

            // Còn dư 1-2 người → ưu tiên đơn, hết thì nâng cấp lên đôi
            if (remaining > 0) {
                if (!singleRooms.isEmpty()) {
                    selected.add(singleRooms.remove(0).getKey());
                } else if (!doubleRooms.isEmpty()) {
                    selected.add(doubleRooms.remove(0).getKey());
                    hasUpgrade = true;
                }
            }
        }

        // Kiểm tra cuối cùng
        int totalCapacity = selected.stream()
                .mapToInt(p -> getCapacity(roomPaneMap.get(p).getRoom()))
                .sum();

        if (totalCapacity < guestCount) {
            dialogPane.showInformation("Thông báo", "Không đủ phòng trống để chứa " + guestCount + " khách.");
            clearSuggestionsHighlight();
            return;
        }

        // Tô viền vàng cho các phòng được chọn
        selected.forEach(pane -> pane.getStyleClass().add("room-suggested"));

        // === CHỈ HIỆN DUY NHẤT 1 THÔNG BÁO ĐẸP NHƯ 7 SAO ===
        String title = hasUpgrade ? "Gợi ý thành công!" : "Gợi ý thành công!";
        String message = "Đã chọn " + selected.size() + " phòng cho " + guestCount +
                         " khách (tổng " + totalCapacity + " chỗ).";

        if (hasUpgrade) {
            message += "\n\nHết phòng đơn → hệ thống đã tự động chọn phòng đôi để phục vụ tốt nhất!";
        }

        dialogPane.showInformation(title, message);
    }
    
//    private void suggestRooms() {
//        if (roomPaneMap.isEmpty()) {
//            return;
//        }
//
//        String text = guestCountField.getText();
//        if (text == null || text.isBlank()) {
//            dialogPane.showInformation("Thông báo", "Vui lòng nhập số lượng khách.");
//            return;
//        }
//
//        int guestCount;
//        try {
//            guestCount = Integer.parseInt(text.trim());
//        } catch (NumberFormatException e) {
//            dialogPane.showInformation("Thông báo", "Số lượng khách phải là số nguyên dương.");
//            return;
//        }
//
//        if (guestCount <= 0) {
//            dialogPane.showInformation("Thông báo", "Số lượng khách phải lớn hơn 0.");
//            return;
//        }
//
//        clearSuggestionsHighlight();
//
//        RoomPriority priority = getCurrentPriority();
//
//        // Lọc ra các phòng trống phù hợp ưu tiên
//        List<Map.Entry<Pane, RoomWithReservation>> candidates = new ArrayList<>();
//        for (Map.Entry<Pane, RoomWithReservation> entry : roomPaneMap.entrySet()) {
//            Room room = entry.getValue().getRoom();
//            if (room.getRoomStatus() != RoomStatus.AVAILABLE) {
//                continue; // chỉ lấy phòng trống
//            }
//            if (!matchesPriority(room, priority)) {
//                continue;
//            }
//            candidates.add(entry);
//        }
//
//        if (candidates.isEmpty()) {
//            dialogPane.showInformation("Thông báo",
//                    "Không tìm thấy phòng trống phù hợp với lựa chọn ưu tiên.");
//            return;
//        }
//
//        // Sắp xếp theo sức chứa giảm dần (phòng nhiều người đứng trước)
//        candidates.sort((e1, e2) ->
//                Integer.compare(getCapacity(e2.getValue().getRoom()),
//                                getCapacity(e1.getValue().getRoom())));
//
//        // Chọn ít phòng nhất nhưng đủ sức chứa
//        List<Pane> selectedPanes = new ArrayList<>();
//        int totalCapacity = 0;
//        for (Map.Entry<Pane, RoomWithReservation> entry : candidates) {
//            selectedPanes.add(entry.getKey());
//            totalCapacity += getCapacity(entry.getValue().getRoom());
//            if (totalCapacity >= guestCount) {
//                break;
//            }
//        }
//
//        if (totalCapacity < guestCount) {
//            dialogPane.showInformation("Thông báo",
//                    "Không đủ phòng trống để chứa " + guestCount + " khách theo tiêu chí đã chọn.");
//            clearSuggestionsHighlight();
//            return;
//        }
//
//        // Tô viền các phòng được chọn
//        for (Pane pane : selectedPanes) {
//            if (!pane.getStyleClass().contains("room-suggested")) {
//                pane.getStyleClass().add("room-suggested");
//            }
//        }
//    }


    private void handleSearch() {
        if (roomWithReservations == null) {
            return;
        }

        List<RoomWithReservation> filteredRooms = roomWithReservations;

        String selectedCategory = roomCategoryCBox.getSelectionModel().getSelectedItem();
        if (selectedCategory != null && !selectedCategory.equals("TẤT CẢ")) {
            String categoryID = selectedCategory.split(" ")[0];
            filteredRooms = filteredRooms.stream()
                    .filter(r -> r.getRoom().getRoomCategory().getRoomCategoryID().equals(categoryID))
                    .toList();
        }

        String selectedFloor = roomFloorNumberCBox.getSelectionModel().getSelectedItem();
        if (selectedFloor != null && !selectedFloor.equals("TẤT CẢ")) {
            filteredRooms = filteredRooms.stream()
                    .filter(r -> r.getRoom().getRoomID().charAt(2) == selectedFloor.charAt(0))
                    .toList();
        }

        if (selectedStatus != null) {
            filteredRooms = filteredRooms.stream()
                    .filter(r -> r.getRoom().getRoomStatus() == selectedStatus)
                    .toList();
        }

        filteredRooms = filteredRooms.stream()
                .sorted(Comparator.comparing(r -> r.getRoom().getRoomNumber()))
                .toList();

        displayFilteredRooms(filteredRooms);
        loadDataForBtn();
    }

    private void setupEventHandlers() {
        setupButtonAction(allBtn, null);
        setupButtonAction(availableBtn, RoomStatus.AVAILABLE);
        setupButtonAction(onUseBtn, RoomStatus.ON_USE);
        setupButtonAction(overDueBtn, RoomStatus.OVERDUE);

        roomCategoryCBox.setOnAction(e -> handleSearch());
        roomFloorNumberCBox.setOnAction(e -> handleSearch());
    }

    private void setupButtonAction(Button button, RoomStatus status) {
        button.setOnAction(e -> {
            if (activeButton == button) {
                resetButtonStyle(button);
                setActiveButtonStyle(allBtn);
                activeButton = allBtn;
                selectedStatus = null;
            } else {
                if (activeButton != null) resetButtonStyle(activeButton);
                setActiveButtonStyle(button);
                activeButton = button;
                selectedStatus = status;
            }
            handleSearch();
        });
    }

    private void setActiveButtonStyle(Button button) {
        switch (button.getId()) {
            case "allBtn" -> button.getStyleClass().add("button-All-selected");
            case "availableBtn" -> button.getStyleClass().add("button-Available-selected");
            case "onUseBtn" -> button.getStyleClass().add("button-OnUse-selected");
            case "overDueBtn" -> button.getStyleClass().add("button-OverDue-selected");
            default -> throw new IllegalArgumentException("Không tìm thấy button ID");
        }
    }

    private void resetButtonStyle(Button button) {
        switch (button.getId()) {
            case "allBtn" -> button.getStyleClass().remove("button-All-selected");
            case "availableBtn" -> button.getStyleClass().remove("button-Available-selected");
            case "onUseBtn" -> button.getStyleClass().remove("button-OnUse-selected");
            case "overDueBtn" -> button.getStyleClass().remove("button-OverDue-selected");
            default -> throw new IllegalArgumentException("Không tìm thấy button ID");
        }
    }


    public DialogPane getDialogPane() {
        return dialogPane;
    }

}
