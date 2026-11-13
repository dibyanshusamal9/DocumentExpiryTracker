package com.doctracker.ui;

import com.doctracker.dao.DaoException;
import com.doctracker.dao.DocumentDao;
import com.doctracker.model.Document;
import com.doctracker.model.DocumentType;
import com.doctracker.model.ExpiryStatus;
import com.doctracker.service.ReminderService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Main window: summary cards, filters, colour-coded document table, reminders. */
public class MainFrame extends JFrame {

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final long CHECK_INTERVAL_MS = 60 * 60 * 1000L; // re-check reminders hourly

    private final DocumentDao dao;
    private final ReminderService reminders = new ReminderService();
    private final DocTableModel model = new DocTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<DocTableModel> sorter = new TableRowSorter<>(model);

    private final JTextField searchField = new JTextField(16);
    private final JComboBox<Object> typeFilter = new JComboBox<>();
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final Map<ExpiryStatus, JLabel> countLabels = new EnumMap<>(ExpiryStatus.class);
    private final JLabel totalLabel = new JLabel("0");
    private final JLabel statusBar = new JLabel();

    private TrayIcon trayIcon;
    /** Remember what we already notified about today so the hourly check doesn't spam. */
    private final Set<String> notifiedToday = new HashSet<>();
    private LocalDate notifiedOn = LocalDate.now();

    public MainFrame(DocumentDao dao) {
        super("Personal Document Expiry Tracker");
        this.dao = dao;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(AppIcon.image(64));

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTable(), BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        setContentPane(root);

        statusBar.setText("Storage — " + dao.describe());
        statusBar.setForeground(Color.GRAY);

        setSize(1050, 620);
        setLocationRelativeTo(null);

        setupTray();
        reload();
        new javax.swing.Timer((int) CHECK_INTERVAL_MS, e -> { reload(); notifyDue(false); }).start();
    }

    /** Called once after the window is visible. */
    public void showStartupReminders() {
        List<Document> due = reminders.needingAttention(model.rows, LocalDate.now());
        if (due.isEmpty()) return;
        StringBuilder sb = new StringBuilder("<html><b>These documents need your attention:</b><br><br>");
        for (Document d : due) {
            String color = d.status() == ExpiryStatus.EXPIRED ? "#b00020" : "#8a6d00";
            sb.append("<span style='color:").append(color).append("'>&#9679;</span> ")
              .append(escape(ReminderService.describe(d, LocalDate.now())))
              .append(" &nbsp;<span style='color:gray'>(").append(d.getExpiryDate().format(FMT)).append(")</span><br>");
        }
        sb.append("</html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "Renewal reminders", JOptionPane.WARNING_MESSAGE);
        notifyDue(true);
    }

    // ------------------------------------------------------------------ UI building

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 8));

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.add(card("Total documents", totalLabel, new Color(0xE3E8F0), null));
        for (ExpiryStatus s : ExpiryStatus.values()) {
            JLabel l = new JLabel("0");
            countLabels.put(s, l);
            cards.add(card(s.getLabel(), l, s.getColor(), s));
        }
        header.add(cards, BorderLayout.NORTH);

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(button("+ Add", e -> onAdd()));
        bar.add(button("Edit", e -> onEdit()));
        bar.add(button("Mark renewed", e -> onRenew()));
        bar.add(button("Delete", e -> onDelete()));
        bar.addSeparator();
        bar.add(button("Export CSV", e -> onExport()));
        bar.add(button("Refresh", e -> reload()));
        bar.add(Box.createHorizontalGlue());
        bar.add(Box.createHorizontalStrut(12));

        typeFilter.addItem("All types");
        for (DocumentType t : DocumentType.values()) typeFilter.addItem(t);
        statusFilter.addItem("All statuses");
        for (ExpiryStatus s : ExpiryStatus.values()) statusFilter.addItem(s);
        typeFilter.addActionListener(e -> applyFilter());
        statusFilter.addActionListener(e -> applyFilter());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        searchField.setToolTipText("Search title, number, issuer or notes");

        searchField.setMaximumSize(new Dimension(240, searchField.getPreferredSize().height + 6));
        bar.add(new JLabel("Search  "));
        bar.add(searchField);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(typeFilter);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(statusFilter);
        header.add(bar, BorderLayout.SOUTH);
        return header;
    }

    private JComponent card(String title, JLabel value, Color bg, ExpiryStatus filterOnClick) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel t = new JLabel(title);
        t.setForeground(new Color(0x444444));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 26f));
        p.add(t, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText(filterOnClick == null ? "Show all" : "Show only \"" + filterOnClick.getLabel() + "\"");
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                statusFilter.setSelectedItem(filterOnClick == null ? "All statuses" : filterOnClick);
            }
        });
        return p;
    }

    private static JButton button(String text, java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.addActionListener(l);
        b.setFocusable(false);
        return b;
    }

    private JComponent buildTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        sorter.setComparator(DocTableModel.COL_DAYS, Comparator.comparingLong(o -> (Long) o));
        sorter.setComparator(DocTableModel.COL_EXPIRY, Comparator.comparing(o -> (LocalDate) o));
        sorter.setSortKeys(List.of(new RowSorter.SortKey(DocTableModel.COL_DAYS, SortOrder.ASCENDING)));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
                Object shown = value;
                if (value instanceof LocalDate) shown = ((LocalDate) value).format(FMT);
                if (col == DocTableModel.COL_DAYS) {
                    long d = (Long) value;
                    shown = d < 0 ? (-d) + " overdue" : d == 0 ? "Today" : String.valueOf(d);
                }
                Component c = super.getTableCellRendererComponent(t, shown, sel, focus, row, col);
                if (!sel) {
                    Document doc = model.rows.get(t.convertRowIndexToModel(row));
                    c.setBackground(doc.status().getColor());
                }
                setHorizontalAlignment(col == DocTableModel.COL_DAYS ? RIGHT : LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Long.class, renderer);

        int[] widths = {200, 120, 130, 160, 100, 90, 90};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) onEdit();
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "deleteDoc");
        table.getActionMap().put("deleteDoc", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { onDelete(); }
        });
        return new JScrollPane(table);
    }

    // ------------------------------------------------------------------ actions

    private void reload() {
        try {
            model.setRows(dao.findAll());
        } catch (DaoException ex) {
            showError(ex);
        }
        LocalDate today = LocalDate.now();
        Map<ExpiryStatus, Long> counts = reminders.counts(model.rows, today);
        counts.forEach((s, n) -> countLabels.get(s).setText(String.valueOf(n)));
        totalLabel.setText(String.valueOf(model.rows.size()));
        if (trayIcon != null) {
            long due = counts.get(ExpiryStatus.EXPIRED) + counts.get(ExpiryStatus.DUE_SOON);
            trayIcon.setToolTip(due == 0 ? "Document Tracker — all good" : "Document Tracker — " + due + " need attention");
        }
    }

    private Document selected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a document first.");
            return null;
        }
        return model.rows.get(table.convertRowIndexToModel(r));
    }

    private void onAdd() {
        Document d = DocumentDialog.show(this, null);
        if (d == null) return;
        try {
            dao.insert(d);
            reload();
        } catch (DaoException ex) { showError(ex); }
    }

    private void onEdit() {
        Document cur = selected();
        if (cur == null) return;
        Document d = DocumentDialog.show(this, cur);
        if (d == null) return;
        try {
            dao.update(d);
            reload();
        } catch (DaoException ex) { showError(ex); }
    }

    private void onRenew() {
        Document cur = selected();
        if (cur == null) return;
        DateField newExpiry = new DateField(ReminderService.suggestRenewal(cur, LocalDate.now()));
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel("<html>Renewed <b>" + escape(cur.getTitle()) + "</b>?<br>New expiry date:</html>"), BorderLayout.NORTH);
        p.add(newExpiry, BorderLayout.CENTER);
        int ok = JOptionPane.showConfirmDialog(this, p, "Mark as renewed", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        LocalDate date = newExpiry.getDate();
        if (!date.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "The new expiry date should be in the future.");
            return;
        }
        cur.setIssueDate(LocalDate.now().isBefore(date) ? LocalDate.now() : null);
        cur.setExpiryDate(date);
        try {
            dao.update(cur);
            reload();
        } catch (DaoException ex) { showError(ex); }
    }

    private void onDelete() {
        Document cur = selected();
        if (cur == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete \"" + cur.getTitle() + "\"? This cannot be undone.",
                "Delete document", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            dao.delete(cur.getId());
            reload();
        } catch (DaoException ex) { showError(ex); }
    }

    private void onExport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("documents.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8))) {
            w.println("Title,Type,Number,Issuer,Issue Date,Expiry Date,Days Left,Status,Reminder Days,Notes");
            for (Document d : model.rows) {
                w.println(String.join(",", csv(d.getTitle()), csv(d.getType().getLabel()), csv(d.getDocumentNumber()),
                        csv(d.getIssuer()), csv(d.getIssueDate() == null ? "" : d.getIssueDate().toString()),
                        csv(d.getExpiryDate().toString()), String.valueOf(d.daysLeft()), csv(d.status().getLabel()),
                        String.valueOf(d.getReminderDays()), csv(d.getNotes())));
            }
            JOptionPane.showMessageDialog(this, "Exported " + model.rows.size() + " documents.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String q = searchField.getText().trim().toLowerCase();
        Object type = typeFilter.getSelectedItem();
        Object status = statusFilter.getSelectedItem();
        sorter.setRowFilter(new RowFilter<DocTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DocTableModel, ? extends Integer> e) {
                Document d = model.rows.get(e.getIdentifier());
                if (type instanceof DocumentType && d.getType() != type) return false;
                if (status instanceof ExpiryStatus && d.status() != status) return false;
                if (q.isEmpty()) return true;
                for (String s : new String[]{d.getTitle(), d.getDocumentNumber(), d.getIssuer(), d.getNotes()}) {
                    if (s != null && s.toLowerCase().contains(q)) return true;
                }
                return false;
            }
        });
    }

    // ------------------------------------------------------------------ reminders / tray

    private void setupTray() {
        if (!SystemTray.isSupported()) return;
        try {
            trayIcon = new TrayIcon(AppIcon.image(16), "Document Tracker");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> { setVisible(true); setState(NORMAL); toFront(); });
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Open");
            open.addActionListener(e -> { setVisible(true); setState(NORMAL); toFront(); });
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> System.exit(0));
            menu.add(open);
            menu.add(exit);
            trayIcon.setPopupMenu(menu);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException | UnsupportedOperationException ex) {
            trayIcon = null;
        }
    }

    /** Desktop notification for anything due that we haven't announced today. */
    private void notifyDue(boolean startup) {
        if (trayIcon == null) return;
        LocalDate today = LocalDate.now();
        if (!today.equals(notifiedOn)) { notifiedToday.clear(); notifiedOn = today; }
        List<Document> due = reminders.needingAttention(model.rows, today);
        List<String> fresh = new ArrayList<>();
        for (Document d : due) {
            if (notifiedToday.add(d.getId() + ":" + d.getExpiryDate())) fresh.add(ReminderService.describe(d, today));
        }
        if (fresh.isEmpty()) return;
        String body = fresh.size() <= 3 ? String.join("\n", fresh)
                : String.join("\n", fresh.subList(0, 3)) + "\n+" + (fresh.size() - 3) + " more";
        trayIcon.displayMessage(startup ? "Renewal reminders" : "Document expiring soon", body, TrayIcon.MessageType.WARNING);
    }

    // ------------------------------------------------------------------ helpers

    private void showError(DaoException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Table model over the loaded documents. */
    static class DocTableModel extends AbstractTableModel {
        static final int COL_EXPIRY = 4, COL_DAYS = 5;
        private static final String[] COLS = {"Title", "Type", "Number", "Issued by", "Expiry", "Days left", "Status"};
        List<Document> rows = new ArrayList<>();

        void setRows(List<Document> r) { rows = r; fireTableDataChanged(); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Class<?> getColumnClass(int c) {
            return c == COL_DAYS ? Long.class : c == COL_EXPIRY ? LocalDate.class : Object.class;
        }

        @Override
        public Object getValueAt(int r, int c) {
            Document d = rows.get(r);
            switch (c) {
                case 0: return d.getTitle();
                case 1: return d.getType().getLabel();
                case 2: return d.getDocumentNumber() == null ? "" : d.getDocumentNumber();
                case 3: return d.getIssuer() == null ? "" : d.getIssuer();
                case 4: return d.getExpiryDate();
                case 5: return d.daysLeft();
                default: return d.status().getLabel();
            }
        }
    }
}
