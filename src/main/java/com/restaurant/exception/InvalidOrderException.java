package com.restaurant.exception;

public class InvalidOrderException extends RestaurantException {
    public InvalidOrderException(String message) {
        super(message);
    }
}