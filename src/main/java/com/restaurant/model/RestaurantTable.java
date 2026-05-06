package com.restaurant.model;

import com.restaurant.model.enums.TableStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantTable {
    private int tableId;
    private int branchId;
    private String tableNumber;
    private int maxCapacity;
    private String locationIdentifier;
    private TableStatus status;
    private final List<TableSeat> seats = new ArrayList<>();

    public RestaurantTable() { this.status = TableStatus.FREE; }

    public RestaurantTable(int tableId, int branchId, String tableNumber,
                           int maxCapacity, String locationIdentifier, TableStatus status) {
        this.tableId = tableId;
        this.branchId = branchId;
        this.tableNumber = tableNumber;
        this.maxCapacity = maxCapacity;
        this.locationIdentifier = locationIdentifier;
        this.status = status;
    }

    public int getTableId()                                { return tableId; }
    public void setTableId(int tableId)                    { this.tableId = tableId; }
    public int getBranchId()                               { return branchId; }
    public void setBranchId(int branchId)                  { this.branchId = branchId; }
    public String getTableNumber()                         { return tableNumber; }
    public void setTableNumber(String tableNumber)         { this.tableNumber = tableNumber; }
    public int getMaxCapacity()                            { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity)            { this.maxCapacity = maxCapacity; }
    public String getLocationIdentifier()                  { return locationIdentifier; }
    public void setLocationIdentifier(String loc)          { this.locationIdentifier = loc; }
    public TableStatus getStatus()                         { return status; }
    public void setStatus(TableStatus status)              { this.status = status; }
    public List<TableSeat> getSeats()                      { return Collections.unmodifiableList(seats); }
    public void addSeat(TableSeat seat)                    { seats.add(seat); }

    @Override
    public String toString() { return "Table " + tableNumber + " (cap:" + maxCapacity + ") [" + status + "]"; }
}