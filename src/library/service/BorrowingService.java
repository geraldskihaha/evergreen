package library.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import library.auth.AuthService;
import library.auth.UserRole;
import library.dao.BookDAO;
import library.dao.BorrowingDAO;
import library.dao.UserDAO;
import library.model.Book;
import library.model.Borrowing;
import library.model.User;

public final class BorrowingService {

    private BorrowingService() { }

    public static String borrowBook(int userId, int bookId, String dueDateText) throws SQLException {
        if (!canBorrowOrReturn()) return "You do not have permission to borrow or return books.";
        return createLoan(userId, bookId, dueDateText);
    }

    public static String returnBook(int borrowingId) throws SQLException {
        if (!canBorrowOrReturn()) return "You do not have permission to borrow or return books.";
        return closeLoan(borrowingId);
    }

    public static String borrowForSelf(int bookId) throws SQLException {
        if (currentMember() == null) return "Please sign in as a member to borrow a book.";

        return createLoan(AuthService.currentUser().id(), bookId, LocalDate.now().plusDays(14).toString());
    }

    public static String returnForSelf(int borrowingId) throws SQLException {
        User member = currentMember();
        if (member == null) return "Please sign in as a member to return a book.";
        boolean mine = false;
        for (Borrowing b : BorrowingDAO.getBorrowingsByUser(member.getId())) {
            if (b.getId() == borrowingId) mine = true;
        }

        if (!mine) return "That borrowing does not belong to your account.";
        return closeLoan(borrowingId);
    }

    private static String createLoan(int userId, int bookId, String dueDateText) throws SQLException {
        User user = UserDAO.getUserById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus()) || !"MEMBER".equals(user.getRole())) {
            return "Please select a valid member.";
        }
        Book book = BookDAO.getBookById(bookId);
        if (book == null || !"ACTIVE".equals(book.getStatus())) return "Please select a valid book.";
        if (book.isBorrowed()) return "This book is already borrowed.";
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dueDateText.trim());
        } catch (DateTimeParseException ex) {
            return "Due date must be in yyyy-MM-dd format.";
        }
        if (dueDate.isBefore(LocalDate.now())) return "Due date cannot be in the past.";
        BorrowingDAO.borrowBook(new Borrowing(userId, bookId, LocalDate.now().toString(), dueDate.toString()));
        return null;
    }

    private static String closeLoan(int borrowingId) throws SQLException {
        if (BorrowingDAO.returnBook(borrowingId)) return null;
        return "This borrowing was already returned.";
    }

    private static User currentMember() {
        if (AuthService.currentUser() == null) return null;
        if (AuthService.currentUser().role() != UserRole.MEMBER) return null;
        return new User(AuthService.currentUser().id(), AuthService.currentUser().username(),
                null, AuthService.currentUser().fullName(), null, "MEMBER", "ACTIVE", null);
    }

    private static boolean canBorrowOrReturn() {
        return AuthService.currentUser() != null
                && (AuthService.currentUser().role() == UserRole.ADMIN || AuthService.currentUser().role() == UserRole.STAFF);
    }

    public static List<Borrowing> getBorrowings() throws SQLException {
        return BorrowingDAO.getBorrowings();
    }

    public static List<Borrowing> getBorrowingsByUser(int userId) throws SQLException {
        return BorrowingDAO.getBorrowingsByUser(userId);
    }
}
