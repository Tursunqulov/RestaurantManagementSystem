package com.restaurant.exception;

public class AuthenticationException extends RestaurantException {
    public AuthenticationException(String message) {
        super(message);
    }
}