package com.doctracker.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One tracked document (passport, licence, policy, warranty ...). */
public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private DocumentType type;
    private String documentNumber;
    private String issuer;
    private LocalDate issueDate;      // optional
    private LocalDate expiryDate;     // required
    private int reminderDays;         // remind this many days before expiry
    private String notes;

    public Document() {
        this.type = DocumentType.OTHER;
        this.reminderDays = 30;
    }

    /** Days left until expiry (negative once expired). */
    public long daysLeft(LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public long daysLeft() { return daysLeft(LocalDate.now()); }

    public ExpiryStatus status(LocalDate today) {
        long d = daysLeft(today);
        if (d < 0) return ExpiryStatus.EXPIRED;
        if (d <= reminderDays) return ExpiryStatus.DUE_SOON;
        return ExpiryStatus.VALID;
    }

    public ExpiryStatus status() { return status(LocalDate.now()); }

    // --- getters / setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public int getReminderDays() { return reminderDays; }
    public void setReminderDays(int reminderDays) { this.reminderDays = reminderDays; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return title + " (" + type.getLabel() + ") expires " + expiryDate;
    }
}
