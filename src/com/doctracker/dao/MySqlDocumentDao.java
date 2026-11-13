package com.doctracker.dao;

import com.doctracker.model.Document;
import com.doctracker.model.DocumentType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** JDBC implementation backed by MySQL. Uses PreparedStatements everywhere (no SQL injection). */
public class MySqlDocumentDao implements DocumentDao {

    private static final String CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS documents (" +
        " id INT AUTO_INCREMENT PRIMARY KEY," +
        " title VARCHAR(120) NOT NULL," +
        " doc_type VARCHAR(30) NOT NULL," +
        " doc_number VARCHAR(60)," +
        " issuer VARCHAR(120)," +
        " issue_date DATE," +
        " expiry_date DATE NOT NULL," +
        " reminder_days INT NOT NULL DEFAULT 30," +
        " notes VARCHAR(500)," +
        " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        " updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
        " INDEX idx_expiry (expiry_date))";

    private static final String COLS =
        "id, title, doc_type, doc_number, issuer, issue_date, expiry_date, reminder_days, notes";

    private final String url;
    private final String user;
    private final String password;

    public MySqlDocumentDao(Properties cfg) throws DaoException {
        this.url = cfg.getProperty("db.url",
                "jdbc:mysql://localhost:3306/doc_tracker?createDatabaseIfNotExist=true&serverTimezone=UTC");
        this.user = cfg.getProperty("db.user", "root");
        this.password = cfg.getProperty("db.password", "");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DaoException("MySQL JDBC driver not found. Put mysql-connector-j-*.jar in the lib folder.", e);
        }
        // Verify the connection and make sure the table exists.
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new DaoException("Cannot connect to MySQL at " + url + ": " + e.getMessage(), e);
        }
    }

    private Connection connect() throws SQLException {
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Document> findAll() throws DaoException {
        String sql = "SELECT " + COLS + " FROM documents ORDER BY expiry_date";
        List<Document> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DaoException("Failed to load documents: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Document findById(int id) throws DaoException {
        String sql = "SELECT " + COLS + " FROM documents WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to load document " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Document insert(Document d) throws DaoException {
        String sql = "INSERT INTO documents (title, doc_type, doc_number, issuer, issue_date, expiry_date, reminder_days, notes)" +
                     " VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }
            return d;
        } catch (SQLException e) {
            throw new DaoException("Failed to save document: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Document d) throws DaoException {
        String sql = "UPDATE documents SET title=?, doc_type=?, doc_number=?, issuer=?, issue_date=?, expiry_date=?," +
                     " reminder_days=?, notes=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, d);
            ps.setInt(9, d.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to update document: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws DaoException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM documents WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to delete document: " + e.getMessage(), e);
        }
    }

    @Override
    public String describe() {
        String hostPart = url.replaceFirst("^jdbc:mysql://", "").replaceFirst("\\?.*$", "");
        return "MySQL: " + hostPart;
    }

    private static void bind(PreparedStatement ps, Document d) throws SQLException {
        ps.setString(1, d.getTitle());
        ps.setString(2, d.getType().name());
        ps.setString(3, d.getDocumentNumber());
        ps.setString(4, d.getIssuer());
        ps.setDate(5, d.getIssueDate() == null ? null : Date.valueOf(d.getIssueDate()));
        ps.setDate(6, Date.valueOf(d.getExpiryDate()));
        ps.setInt(7, d.getReminderDays());
        ps.setString(8, d.getNotes());
    }

    private static Document map(ResultSet rs) throws SQLException {
        Document d = new Document();
        d.setId(rs.getInt("id"));
        d.setTitle(rs.getString("title"));
        d.setType(DocumentType.parse(rs.getString("doc_type")));
        d.setDocumentNumber(rs.getString("doc_number"));
        d.setIssuer(rs.getString("issuer"));
        Date issue = rs.getDate("issue_date");
        d.setIssueDate(issue == null ? null : issue.toLocalDate());
        d.setExpiryDate(rs.getDate("expiry_date").toLocalDate());
        d.setReminderDays(rs.getInt("reminder_days"));
        d.setNotes(rs.getString("notes"));
        return d;
    }
}
