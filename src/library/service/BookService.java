package library.service;

import java.sql.SQLException;
import java.util.List;
import library.dao.BookDAO;
import library.model.Book;

public final class BookService {

    private BookService() { }

    public static List<Book> getBooks() throws SQLException {
        return BookDAO.getAllBooks();
    }

    public static List<Book> searchBooks(String query) throws SQLException {
        return BookDAO.searchBooks(query);
    }

    public static String addBook(String isbn, String title, String author, String category) throws SQLException {
        String error = validate(isbn, title, author);
        if (error != null) return error;
        if (BookDAO.getBookByIsbn(isbn) != null) return "ISBN already exists.";
        Book book = new Book(0, isbn.trim(), title.trim(), author.trim(), blankToNull(category), false, "ACTIVE");
        BookDAO.addBook(book);
        return null;
    }

    public static String updateBook(Book book, String isbn, String title, String author, String category) throws SQLException {
        String error = validate(isbn, title, author);
        if (error != null) return error;
        Book duplicate = BookDAO.getBookByIsbn(isbn);
        if (duplicate != null && duplicate.getId() != book.getId()) return "ISBN already exists.";
        book.setIsbn(isbn.trim());
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setCategory(blankToNull(category));
        BookDAO.updateBook(book);
        return null;
    }

    public static String deleteBook(Book book) throws SQLException {

        if (book.isBorrowed()) return "This book is currently borrowed and cannot be deleted.";
        int removedLoans = BookDAO.deleteBookWithHistory(book.getId());
        if (removedLoans < 0) return "Could not delete the book.";
        if (removedLoans == 0) return "Book deleted.";
        return "Book deleted, along with " + removedLoans
                + (removedLoans == 1 ? " past borrowing record." : " past borrowing records.");
    }

    private static String validate(String isbn, String title, String author) {
        if (isbn == null || isbn.isBlank()) return "ISBN cannot be empty.";
        if (title == null || title.isBlank()) return "Book title cannot be empty.";
        if (author == null || author.isBlank()) return "Book author cannot be empty.";
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
