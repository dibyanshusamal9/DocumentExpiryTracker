package com.doctracker.service;

import com.doctracker.model.Document;
import com.doctracker.model.ExpiryStatus;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Pure reminder logic: decides which documents need attention. No UI, so it is easy to test. */
public class ReminderService {

    /** Documents that are expired or inside their reminder window, most urgent first. */
    public List<Document> needingAttention(List<Document> all, LocalDate today) {
        return all.stream()
                .filter(d -> d.status(today) != ExpiryStatus.VALID)
                .sorted(Comparator.comparingLong(d -> d.daysLeft(today)))
                .collect(Collectors.toList());
    }

    public Map<ExpiryStatus, Long> counts(List<Document> all, LocalDate today) {
        Map<ExpiryStatus, Long> m = new EnumMap<>(ExpiryStatus.class);
        for (ExpiryStatus s : ExpiryStatus.values()) m.put(s, 0L);
        for (Document d : all) m.merge(d.status(today), 1L, Long::sum);
        return m;
    }

    /** Human readable one-liner, e.g. "Car Insurance expired 5 days ago". */
    public static String describe(Document d, LocalDate today) {
        long days = d.daysLeft(today);
        if (days < 0) return d.getTitle() + " expired " + (-days) + (days == -1 ? " day" : " days") + " ago";
        if (days == 0) return d.getTitle() + " expires TODAY";
        if (days == 1) return d.getTitle() + " expires tomorrow";
        return d.getTitle() + " expires in " + days + " days";
    }

    /** Suggested new expiry when the user marks a document as renewed. */
    public static LocalDate suggestRenewal(Document d, LocalDate today) {
        int years;
        switch (d.getType()) {
            case PASSPORT:        years = 10; break;
            case DRIVING_LICENSE: years = 20; break;
            case VEHICLE_RC:      years = 1;  break;
            default:              years = 1;
        }
        LocalDate base = d.getExpiryDate().isBefore(today) ? today : d.getExpiryDate();
        if (d.getIssueDate() != null && d.getIssueDate().isBefore(d.getExpiryDate())) {
            // keep the same validity length as last time
            long len = java.time.temporal.ChronoUnit.DAYS.between(d.getIssueDate(), d.getExpiryDate());
            return base.plusDays(len);
        }
        return base.plusYears(years);
    }
}
