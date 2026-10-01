package library.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "library.db";
    private static final String URL = "jdbc:sqlite:" + DB_FOLDER + "/" + DB_FILE;

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(Paths.get(DB_FOLDER));
        } catch (IOException ex) {
            throw new SQLException("Could not create the data folder: " + ex.getMessage(), ex);
        }
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException ex) {
            conn.close();
            throw ex;
        }
        return conn;
    }

}
