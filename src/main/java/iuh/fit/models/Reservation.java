package iuh.fit.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reservation {
    private String customerName;
    private String roomName;
    private LocalDateTime time;

    public Reservation(String customerName, String roomName, LocalDateTime time) {
        this.customerName = customerName;
        this.roomName = roomName;
        this.time = time;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getRoomName() {
        return roomName;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
