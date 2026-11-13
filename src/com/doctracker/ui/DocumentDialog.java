package com.doctracker.ui;

import com.doctracker.model.Document;
import com.doctracker.model.DocumentType;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/** Add / edit form for a single document. */
public class DocumentDialog extends JDialog {

    private final JTextField titleField = new JTextField(24);
    private final JComboBox<DocumentType> typeBox = new JComboBox<>(DocumentType.values());
    private final JTextField numberField = new JTextField(24);
    private final JTextField issuerField = new JTextField(24);
    private final JCheckBox hasIssueDate = new JCheckBox("Known");
    private final DateField issueDate = new DateField(LocalDate.now());
    private final DateField expiryDate = new DateField(LocalDate.now().plusYears(1));
    private final JSpinner reminderDays = new JSpinner(new SpinnerNumberModel(30, 0, 730, 1));
    private final JTextArea notesArea = new JTextArea(3, 24);

    private boolean reminderTouched = false;
    private Document result;

    public DocumentDialog(Frame owner, Document existing) {
        super(owner, existing == null ? "Add Document" : "Edit Document", true);
        buildUi();
        if (existing != null) fill(existing);
        else reminderDays.setValue(((DocumentType) typeBox.getSelectedItem()).getDefaultReminderDays());
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
        // fill() sets the spinner programmatically; only user edits after this count
        reminderTouched = existing != null;
        reminderDays.addChangeListener(e -> reminderTouched = true);
    }

    /** Shows the dialog and returns the saved document, or null if cancelled. */
    public static Document show(Frame owner, Document existing) {
        DocumentDialog dlg = new DocumentDialog(owner, existing);
        dlg.setVisible(true);
        return dlg.result;
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, c, row++, "Title *", titleField);
        addRow(form, c, row++, "Type", typeBox);
        addRow(form, c, row++, "Document / Policy No.", numberField);
        addRow(form, c, row++, "Issued by", issuerField);

        JPanel issuePanel = new JPanel(new BorderLayout(6, 0));
        issuePanel.add(hasIssueDate, BorderLayout.WEST);
        issuePanel.add(issueDate, BorderLayout.CENTER);
        issueDate.setEnabled(false);
        hasIssueDate.addActionListener(e -> issueDate.setEnabled(hasIssueDate.isSelected()));
        addRow(form, c, row++, "Issue date", issuePanel);

        addRow(form, c, row++, "Expiry date *", expiryDate);

        JPanel remindPanel = new JPanel(new BorderLayout(6, 0));
        remindPanel.add(reminderDays, BorderLayout.CENTER);
        remindPanel.add(new JLabel("days before expiry"), BorderLayout.EAST);
        addRow(form, c, row++, "Remind me", remindPanel);

        notesArea.setFont(titleField.getFont());
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        addRow(form, c, row++, "Notes", new JScrollPane(notesArea));

        typeBox.addActionListener(e -> {
            if (!reminderTouched) {
                reminderDays.setValue(((DocumentType) typeBox.getSelectedItem()).getDefaultReminderDays());
                reminderTouched = false; // programmatic change, not the user's
            }
        });

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);
        getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private static void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private Document existing;

    private void fill(Document d) {
        existing = d;
        titleField.setText(d.getTitle());
        typeBox.setSelectedItem(d.getType());
        numberField.setText(nz(d.getDocumentNumber()));
        issuerField.setText(nz(d.getIssuer()));
        if (d.getIssueDate() != null) {
            hasIssueDate.setSelected(true);
            issueDate.setEnabled(true);
            issueDate.setDate(d.getIssueDate());
        }
        expiryDate.setDate(d.getExpiryDate());
        reminderDays.setValue(d.getReminderDays());
        notesArea.setText(nz(d.getNotes()));
    }

    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            error("Please enter a title, e.g. \"My Passport\".");
            titleField.requestFocus();
            return;
        }
        LocalDate expiry = expiryDate.getDate();
        LocalDate issue = hasIssueDate.isSelected() ? issueDate.getDate() : null;
        if (issue != null && !issue.isBefore(expiry)) {
            error("Issue date must be before the expiry date.");
            return;
        }
        if (title.length() > 120 || numberField.getText().length() > 60
                || issuerField.getText().length() > 120 || notesArea.getText().length() > 500) {
            error("Some fields are too long (title/issuer 120, number 60, notes 500 characters).");
            return;
        }

        Document d = new Document();
        if (existing != null) d.setId(existing.getId());
        d.setTitle(title);
        d.setType((DocumentType) typeBox.getSelectedItem());
        d.setDocumentNumber(emptyToNull(numberField.getText()));
        d.setIssuer(emptyToNull(issuerField.getText()));
        d.setIssueDate(issue);
        d.setExpiryDate(expiry);
        d.setReminderDays((Integer) reminderDays.getValue());
        d.setNotes(emptyToNull(notesArea.getText()));
        result = d;
        dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Check the form", JOptionPane.WARNING_MESSAGE);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String emptyToNull(String s) {
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
