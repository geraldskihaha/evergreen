package library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import library.database.DatabaseConnection;
import library.model.Borrowing;

public final class BorrowingDAO {

    private BorrowingDAO() { }

    public static boolean borrowBook(Borrowing borrowing) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int borrowingId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO loans (user_id, book_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, borrowing.getUserId());
                    ps.setInt(2, borrowing.getBookId());
                    ps.setString(3, borrowing.getBorrowDate());
                    ps.setString(4, borrowing.getDueDate());
                    ps.setString(5, borrowing.getStatus());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        borrowingId = keys.next() ? keys.getInt(1) : 0;
                    }
                }
                int marked;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE books SET is_borrowed = 1 "
                        + "WHERE id = ? AND is_borrowed = 0")) {
                    ps.setInt(1, borrowing.getBookId());
                    marked = ps.executeUpdate();
                }
                if (borrowingId == 0 || marked != 1) {
                    conn.rollback();
                    return false;
                }
                borrowing.setId(borrowingId);
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static boolean returnBook(int borrowingId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updatedBorrowing;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE loans SET return_date = ?, status = 'RETURNED' "
                        + "WHERE id = ? AND status = 'BORROWED'")) {
                    ps.setString(1, LocalDate.now().toString());
                    ps.setInt(2, borrowingId);
                    updatedBorrowing = ps.executeUpdate();
                }
                int released = 0;
                if (updatedBorrowing == 1) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE books SET is_borrowed = 0 "
                            + "WHERE id = (SELECT book_id FROM loans WHERE id = ?)")) {
                        ps.setInt(1, borrowingId);
                        released = ps.executeUpdate();
                    }
                }
                if (updatedBorrowing != 1 || released != 1) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<Borrowing> getBorrowings() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM loans ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Borrowing> loans = new ArrayList<>();
            while (rs.next()) loans.add(readBorrowing(rs));
            return loans;
        }
    }

    public static List<Borrowing> getBorrowingsByUser(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM loans WHERE user_id = ? ORDER BY id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Borrowing> loans = new ArrayList<>();
                while (rs.next()) loans.add(readBorrowing(rs));
                return loans;
            }
        }
    }

    public static List<Borrowing> getBorrowingsByBook(int bookId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM loans WHERE book_id = ? ORDER BY id DESC")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Borrowing> loans = new ArrayList<>();
                while (rs.next()) loans.add(readBorrowing(rs));
                return loans;
            }
        }
    }

    private static Borrowing readBorrowing(ResultSet rs) throws SQLException {
        return new Borrowing(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("book_id"),
                rs.getString("borrow_date"),
                rs.getString("due_date"),
                rs.getString("return_date"),
                rs.getString("status"));
    }
}
