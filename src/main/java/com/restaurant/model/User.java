package com.restaurant.model;

import com.restaurant.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Represents an authenticated system user (employee).
 *
 * <p>The {@link UserRole} field drives dashboard routing after login:
 * {@code MANAGER} → Manager Dashboard, {@code RECEPTIONIST} → Reservation Dashboard,
 * {@code WAITER} → Order Dashboard.</p>
 *
 * <p><b>Encapsulation:</b> the password field has <em>no getter</em> —
 * once set, it can only be verified through {@link #checkPassword(String)}.
 * This prevents accidental leaking of credentials in logs or UI.</p>
 */
public class User {

    private int userId;
    private String username;
    private String password;   // write-only outside this class
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private Integer branchId;  // nullable — ADMINs are system-wide
    private boolean active;
    private LocalDateTime createdAt;

    public User() { this.active = true; }

    public User(int userId, String username, String password,
                UserRole role, Integer branchId) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.branchId = branchId;
        this.active = true;
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getUserId()                          { return userId; }
    public void setUserId(int userId)               { this.userId = userId; }

    public String getUsername()                      { return username; }
    public void setUsername(String username)         { this.username = username; }

    /**
     * Sets the password. For authentication, use {@link #checkPassword(String)}.
     */
    public void setPassword(String password)        { this.password = password; }

    /**
     * Verifies a candidate password against the stored value.
     *
     * @param candidate the plain-text password entered at login
     * @return {@code true} if it matches
     */
    public boolean checkPassword(String candidate) {
        return this.password != null && this.password.equals(candidate);
    }

    /**
     * Returns the raw password <b>for DAO persistence only</b>.
     * <p>Do NOT use this in UI or logging code — use {@link #checkPassword}
     * for authentication instead. This exists solely so the DAO layer
     * can write the password to the database.</p>
     */
    public String getPasswordForStorage() { return password; }

    public String getFullName()                     { return fullName; }
    public void setFullName(String fullName)        { this.fullName = fullName; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getPhone()                        { return phone; }
    public void setPhone(String phone)              { this.phone = phone; }

    public UserRole getRole()                       { return role; }
    public void setRole(UserRole role)              { this.role = role; }

    public Integer getBranchId()                    { return branchId; }
    public void setBranchId(Integer branchId)       { this.branchId = branchId; }

    public boolean isActive()                       { return active; }
    public void setActive(boolean active)           { this.active = active; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime ts)      { this.createdAt = ts; }

    @Override
    public String toString() {
        return username + " [" + role + "]";
    }
}
