package com.pantrypal.model;

import com.google.firebase.firestore.Exclude;import com.google.firebase.firestore.IgnoreExtraProperties;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@IgnoreExtraProperties
public class Item {
    @Exclude
    private String documentId;
    private String userId;
    private String name;

    // CHANGED FROM INT TO DOUBLE
    private double quantity;

    private String unit;
    private String purchaseDate;
    private String expiryDate;

    // --- EMPTY CONSTRUCTOR (Required by Firestore) ---
    public Item() { }

    // --- CONSTRUCTOR ---
    // Updated constructor to accept double for quantity
    public Item(String userId, String name, double quantity, String unit, LocalDate purchaseDate, LocalDate expiryDate) {
        this.userId = userId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.purchaseDate = (purchaseDate != null) ? purchaseDate.toString() : null;
        this.expiryDate = (expiryDate != null) ? expiryDate.toString() : null;
    }

    // --- LOGIC ---
    @Exclude
    public long getDaysUntilExpiry() {
        LocalDate expiry = getExpiryDateAsLocal();
        if (expiry == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiry);
    }

    // --- GETTERS & SETTERS ---
    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Updated Getter to return double
    public double getQuantity() { return quantity; }

    // Updated Setter to accept double
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // --- DATE HELPERS ---
    @Exclude
    public LocalDate getPurchaseDateAsLocal() {
        return (purchaseDate == null || purchaseDate.isEmpty()) ? null : LocalDate.parse(purchaseDate);
    }
    @Exclude
    public LocalDate getExpiryDateAsLocal() {
        return (expiryDate == null || expiryDate.isEmpty()) ? null : LocalDate.parse(expiryDate);
    }
}
