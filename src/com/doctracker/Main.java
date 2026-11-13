package com.doctracker;

import com.doctracker.dao.DaoException;
import com.doctracker.dao.DocumentDao;
import com.doctracker.dao.FileDocumentDao;
import com.doctracker.dao.MySqlDocumentDao;
import com.doctracker.ui.MainFrame;

import javax.swing.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Entry point. Connects to MySQL using db.properties; if that fails the user can continue
 * with offline file storage. Pass --offline to skip MySQL entirely.
 */
public class Main {

    public static void main(String[] args) {
        boolean offline = args.length > 0 && args[0].equalsIgnoreCase("--offline");
        SwingUtilities.invokeLater(() -> start(offline));
    }

    private static void start(boolean offline) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* default look is fine */ }

        DocumentDao dao = offline ? openFileStore() : openDatabase();
        if (dao == null) System.exit(1);

        MainFrame frame = new MainFrame(dao);
        frame.setVisible(true);
        SwingUtilities.invokeLater(frame::showStartupReminders);
    }

    private static DocumentDao openDatabase() {
        Properties cfg = loadConfig();
        try {
            return new MySqlDocumentDao(cfg);
        } catch (DaoException e) {
            int choice = JOptionPane.showConfirmDialog(null,
                    "<html><b>Could not connect to MySQL.</b><br>" + e.getMessage().replace("<", "&lt;") +
                    "<br><br>Check db.properties and that the MySQL server is running.<br><br>" +
                    "Continue with offline storage (data/documents.dat) instead?</html>",
                    "Database unavailable", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return choice == JOptionPane.YES_OPTION ? openFileStore() : null;
        }
    }

    private static DocumentDao openFileStore() {
        try {
            return new FileDocumentDao(Paths.get("data", "documents.dat"));
        } catch (DaoException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Storage error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /** Reads ./db.properties if present; otherwise defaults (root / no password on localhost:3306). */
    static Properties loadConfig() {
        Properties p = new Properties();
        Path file = Paths.get("db.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (Exception e) {
                System.err.println("Could not read db.properties: " + e.getMessage());
            }
        }
        return p;
    }
}
