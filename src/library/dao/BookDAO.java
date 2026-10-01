package library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import library.database.DatabaseConnection;
import library.model.Book;

public final class BookDAO {

    private BookDAO() { }

    public static List<Book> getAllBooks() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM books ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Book> books = new ArrayList<>();
            while (rs.next()) books.add(readBook(rs));
            return books;
        }
    }

    public static Book getBookById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM books WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readBook(rs) : null;
            }
        }
    }

    public static Book getBookByIsbn(String isbn) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM books WHERE lower(isbn) = ?")) {
            ps.setString(1, isbn.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readBook(rs) : null;
            }
        }
    }

    public static int addBook(Book book) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO books (isbn, title, author, category, is_borrowed, status) "
                     + "VALUES (?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.isBorrowed() ? 1 : 0);
            ps.setString(6, book.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public static boolean updateBook(Book book) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE books SET isbn = ?, title = ?, author = ?, category = ?, "
                     + "is_borrowed = ?, status = ? WHERE id = ?")) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.isBorrowed() ? 1 : 0);
            ps.setString(6, book.getStatus());
            ps.setInt(7, book.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public static int deleteBookWithHistory(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int removedLoans;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM loans WHERE book_id = ?")) {
                    ps.setInt(1, id);
                    removedLoans = ps.executeUpdate();
                }
                int removedBook;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {
                    ps.setInt(1, id);
                    removedBook = ps.executeUpdate();
                }
                if (removedBook != 1) {
                    conn.rollback();
                    return -1;
                }
                conn.commit();
                return removedLoans;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<Book> searchBooks(String query) throws SQLException {
        String like = "%" + query.trim().toLowerCase() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM books WHERE lower(title) LIKE ? OR lower(author) LIKE ? "
                     + "OR lower(isbn) LIKE ? OR lower(category) LIKE ? ORDER BY id")) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (rs.next()) books.add(readBook(rs));
                return books;
            }
        }
    }

    private static Book readBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("category"),
                rs.getInt("is_borrowed") == 1,
                rs.getString("status"));
    }
}
