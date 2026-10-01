package library.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import library.util.PasswordUtil;

public final class DatabaseInitializer {

    private static final String[] TABLES = {
        """
        CREATE TABLE IF NOT EXISTS users (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            username    TEXT UNIQUE NOT NULL,
            password    TEXT NOT NULL,
            full_name   TEXT NOT NULL,
            email       TEXT,
            role        TEXT NOT NULL,
            status      TEXT NOT NULL,
            created_at  TEXT
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS books (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            isbn         TEXT UNIQUE NOT NULL,
            title        TEXT NOT NULL,
            author       TEXT NOT NULL,
            category     TEXT,
            is_borrowed  INTEGER NOT NULL DEFAULT 0,
            status       TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS loans (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id      INTEGER NOT NULL,
            book_id      INTEGER NOT NULL,
            borrow_date  TEXT NOT NULL,
            due_date     TEXT NOT NULL,
            return_date  TEXT,
            status       TEXT NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (book_id) REFERENCES books(id)
        )
        """
    };

    private static final String[][] SAMPLE_BOOKS = {
        {"9780743273565", "The Great Gatsby", "F. Scott Fitzgerald", "Classic"},
        {"9780061120084", "To Kill a Mockingbird", "Harper Lee", "Classic"},
        {"9780132350884", "Clean Code", "Robert C. Martin", "Technology"},
        {"9781786892737", "The Midnight Library", "Matt Haig", "Fiction"},
        {"9780735211292", "Atomic Habits", "James Clear", "Self-Help"}
    };

    private DatabaseInitializer() { }

    public static void initialize() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String table : TABLES) {
                try (Statement st = conn.createStatement()) {
                    st.execute(table);
                }
            }
            addMissingUserColumns(conn);
            seedData(conn);
        }
    }

    private static void addMissingUserColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "users", "email")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN email TEXT");
            }
        }
        if (!hasColumn(conn, "users", "created_at")) {

            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN created_at TEXT");
                st.execute("UPDATE users SET created_at = datetime('now') WHERE created_at IS NULL");
            }
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    private static void seedData(Connection conn) throws SQLException {
        if (count(conn, "users") == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, full_name, role, status, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
                ps.setString(1, "admin");
                ps.setString(2, PasswordUtil.hash("admin123"));
                ps.setString(3, "Library Administrator");
                ps.setString(4, "ADMIN");
                ps.setString(5, "ACTIVE");
                ps.addBatch();
                ps.setString(1, "staff");
                ps.setString(2, PasswordUtil.hash("staff123"));
                ps.setString(3, "Library Staff");
                ps.setString(4, "STAFF");
                ps.setString(5, "ACTIVE");
                ps.addBatch();
                ps.setString(1, "member");
                ps.setString(2, PasswordUtil.hash("member123"));
                ps.setString(3, "Library Member");
                ps.setString(4, "MEMBER");
                ps.setString(5, "ACTIVE");
                ps.addBatch();
                ps.executeBatch();
            }
        }
        if (count(conn, "books") == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO books (isbn, title, author, category, is_borrowed, status) "
                    + "VALUES (?, ?, ?, ?, 0, 'ACTIVE')")) {
                for (String[] book : SAMPLE_BOOKS) {
                    ps.setString(1, book[0]);
                    ps.setString(2, book[1]);
                    ps.setString(3, book[2]);
                    ps.setString(4, book[3]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private static int count(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
