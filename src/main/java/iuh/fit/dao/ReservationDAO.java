package iuh.fit.dao;

import iuh.fit.models.Reservation;
import iuh.fit.utils.DBHelper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // Lấy danh sách khách check-in hôm nay
	public static List<Reservation> getCheckinToday(LocalDate date) {
	    List<Reservation> data = new ArrayList<>();

	    String sql = """
	            SELECT 
	                c.fullName AS customerName,
	                rf.roomID,
	                h.checkInDate
	            FROM HistoryCheckin h
	            JOIN ReservationForm rf ON h.reservationFormID = rf.reservationFormID
	            JOIN Customer c ON rf.customerID = c.customerID
	            WHERE CAST(h.checkInDate AS DATE) = ?
	            """;

	    try (Connection con = DBHelper.getConnection();
	         PreparedStatement stm = con.prepareStatement(sql)) {

	        stm.setDate(1, Date.valueOf(date));
	        ResultSet rs = stm.executeQuery();

	        while (rs.next()) {
	            data.add(new Reservation(
	                    rs.getString("customerName"),
	                    rs.getString("roomID"),
	                    rs.getTimestamp("checkInDate").toLocalDateTime()
	            ));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return data;
	}


    // Lấy danh sách khách check-out hôm nay
	public static List<Reservation> getCheckoutToday(LocalDate date) {
	    List<Reservation> data = new ArrayList<>();

	    String sql = """
	            SELECT 
	                c.fullName AS customerName,
	                rf.roomID,
	                h.checkOutDate
	            FROM HistoryCheckOut h
	            JOIN ReservationForm rf ON h.reservationFormID = rf.reservationFormID
	            JOIN Customer c ON rf.customerID = c.customerID
	            WHERE CAST(h.checkOutDate AS DATE) = ?
	            """;

	    try (Connection con = DBHelper.getConnection();
	         PreparedStatement stm = con.prepareStatement(sql)) {

	        stm.setDate(1, Date.valueOf(date));
	        ResultSet rs = stm.executeQuery();

	        while (rs.next()) {
	            data.add(new Reservation(
	                    rs.getString("customerName"),
	                    rs.getString("roomID"),
	                    rs.getTimestamp("checkOutDate").toLocalDateTime()
	            ));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return data;
	}


}
