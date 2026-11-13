package com.doctracker.model;

/** Categories of documents the tracker understands. */
public enum DocumentType {
    PASSPORT("Passport", 180),
    DRIVING_LICENSE("Driving License", 30),
    INSURANCE("Insurance Policy", 15),
    WARRANTY("Warranty", 30),
    VEHICLE_RC("Vehicle RC / PUC", 15),
    ID_CARD("ID Card", 30),
    SUBSCRIPTION("Subscription", 7),
    OTHER("Other", 30);

    private final String label;
    private final int defaultReminderDays;

    DocumentType(String label, int defaultReminderDays) {
        this.label = label;
        this.defaultReminderDays = defaultReminderDays;
    }

    public String getLabel() { return label; }

    /** Sensible default lead time, e.g. passports need ~6 months. */
    public int getDefaultReminderDays() { return defaultReminderDays; }

    @Override
    public String toString() { return label; }

    public static DocumentType parse(String s) {
        try {
            return DocumentType.valueOf(s);
        } catch (Exception e) {
            return OTHER;
        }
    }
}
