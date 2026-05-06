package com.restaurant.model;

import com.restaurant.model.enums.SeatType;

public class TableSeat {
    private int id;
    private int tableId;
    private int seatNumber;
    private SeatType type;

    public TableSeat(int id, int tableId, int seatNumber, SeatType type) {
        this.id = id;
        this.tableId = tableId;
        this.seatNumber = seatNumber;
        this.type = type;
    }

    public int getId() { return id; }
    public int getTableId() { return tableId; }
    public int getSeatNumber() { return seatNumber; }
    public SeatType getType() { return type; }
}