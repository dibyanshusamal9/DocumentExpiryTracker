package com.doctracker.ui;

import javax.swing.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** Small date picker built on JSpinner (dd-MM-yyyy). Arrow keys / mouse wheel change the selected part. */
public class DateField extends JSpinner {

    public DateField(LocalDate initial) {
        super(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        setEditor(new JSpinner.DateEditor(this, "dd-MM-yyyy"));
    }

    public LocalDate getDate() {
        try { commitEdit(); } catch (java.text.ParseException ignored) { /* keep last valid value */ }
        return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate d) { setValue(toDate(d)); }

    private static Date toDate(LocalDate d) {
        return Date.from((d == null ? LocalDate.now() : d).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
