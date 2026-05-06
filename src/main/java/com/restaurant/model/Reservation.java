package com.restaurant.model;

import com.restaurant.model.enums.ReservationStatus;
import java.time.LocalDateTime;

/**
 * Represents a table reservation.
 * customer_id and reserved_by_user_id are Integer (nullable) — a
 * walk-in may have no customer record, and the receptionist field is
 * optional for programmatic inserts.
 */
public class Reservation {
    private int reservationId;
    private int tableId;
    private Integer customerId;
    private Integer reservedByUserId;
    private LocalDateTime timeOfReservation;
    private int peopleCount;
    private ReservationStatus status;
    private String notes;
    private LocalDateTime checkinTime;
    private LocalDateTime createdAt;

    public Reservation() { this.status = ReservationStatus.PENDING; }

    // ── Getters & Setters ───────────────────────────────────
    public int getReservationId()                              { return reservationId; }
    public void setReservationId(int reservationId)            { this.reservationId = reservationId; }

    public int getTableId()                                    { return tableId; }
    public void setTableId(int tableId)                        { this.tableId = tableId; }

    public Integer getCustomerId()                             { return customerId; }
    public void setCustomerId(Integer customerId)              { this.customerId = customerId; }

    public Integer getReservedByUserId()                       { return reservedByUserId; }
    public void setReservedByUserId(Integer userId)            { this.reservedByUserId = userId; }

    public LocalDateTime getTimeOfReservation()                { return timeOfReservation; }
    public void setTimeOfReservation(LocalDateTime t)          { this.timeOfReservation = t; }

    public int getPeopleCount()                                { return peopleCount; }
    public void setPeopleCount(int peopleCount)                { this.peopleCount = peopleCount; }

    public ReservationStatus getStatus()                       { return status; }
    public void setStatus(ReservationStatus status)            { this.status = status; }

    public String getNotes()                                   { return notes; }
    public void setNotes(String notes)                         { this.notes = notes; }

    public LocalDateTime getCheckinTime()                      { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime)      { this.checkinTime = checkinTime; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)          { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Reservation #" + reservationId + " [" + status + "] @ " + timeOfReservation;
    }
}