module com.restaurant {

    // ─── JavaFX modules ───────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // ─── JDBC ─────────────────────────────────────────────────
    requires java.sql;

    // ─── Open packages to JavaFX for reflection ───────────────
    opens   com.restaurant             to javafx.fxml, javafx.graphics;
    opens   com.restaurant.controller  to javafx.fxml;
    opens   com.restaurant.model       to javafx.base;
    opens   com.restaurant.model.enums to javafx.base;

    // ─── Export public API packages ───────────────────────────
    exports com.restaurant;
    exports com.restaurant.model;
    exports com.restaurant.model.enums;
    exports com.restaurant.controller;
    exports com.restaurant.dao;
    exports com.restaurant.service;
    exports com.restaurant.util;
    exports com.restaurant.exception;
}
