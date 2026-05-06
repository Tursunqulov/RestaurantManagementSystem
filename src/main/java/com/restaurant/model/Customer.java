package com.restaurant.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registered customer — created by a Receptionist during reservation flow.
 */
public class Customer {
    private int customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate lastVisited;
    private LocalDateTime createdAt;

    public Customer() {}

    public Customer(int customerId, String fullName, String email, String phone) {
        this.customerId = customerId;
        this.fullName   = fullName;
        this.email      = email;
        this.phone      = phone;
    }

    public int getCustomerId()                       { return customerId; }
    public void setCustomerId(int customerId)        { this.customerId = customerId; }
    public String getFullName()                      { return fullName; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }
    public String getPhone()                         { return phone; }
    public void setPhone(String phone)               { this.phone = phone; }
    public LocalDate getLastVisited()                { return lastVisited; }
    public void setLastVisited(LocalDate lastVisited){ this.lastVisited = lastVisited; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime ts)       { this.createdAt = ts; }

    @Override
    public String toString() { return fullName + " (" + phone + ")"; }
}
