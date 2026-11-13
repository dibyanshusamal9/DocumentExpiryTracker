package com.doctracker.model;

import java.awt.Color;

/** Where a document stands relative to today. */
public enum ExpiryStatus {
    EXPIRED("Expired", new Color(0xF8D7DA)),
    DUE_SOON("Renew soon", new Color(0xFFF3CD)),
    VALID("Valid", new Color(0xD4EDDA));

    private final String label;
    private final Color color;

    ExpiryStatus(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public Color getColor() { return color; }

    @Override
    public String toString() { return label; }
}
