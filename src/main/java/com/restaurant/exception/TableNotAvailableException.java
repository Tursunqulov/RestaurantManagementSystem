package com.restaurant.exception;

public class TableNotAvailableException extends RestaurantException {
    public TableNotAvailableException(String message) {
        super(message);
    }
}