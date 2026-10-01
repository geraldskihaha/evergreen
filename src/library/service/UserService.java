package library.service;

import java.sql.SQLException;
import java.util.List;
import library.auth.AuthService;
import library.auth.UserRole;
import library.dao.UserDAO;
import library.model.User;
import library.util.PasswordUtil;

public final class UserService {

    private UserService() { }

    public static List<User> getUsers() throws SQLException {
        return UserDAO.getUsers();
    }

    public static User getUserById(int id) throws SQLException {
        return UserDAO.getUserById(id);
    }

    public static String addUser(String username, String password, String fullName, String email,
                                 String role, String status) throws SQLException {
        if (!isAdmin()) return "You do not have permission to manage users.";
        String error = validate(username, fullName, email, role, status);
        if (error != null) return error;
        if (password == null || password.isBlank()) return "Please enter a password.";
        if (UserDAO.getUserByUsername(username) != null) return "Username already exists.";
        UserDAO.addUser(new User(0, username.trim().toLowerCase(), PasswordUtil.hash(password),
                fullName.trim(), blankToNull(email), role, status, null));
        return null;
    }

    public static String updateUser(User user, String username, String password, String fullName,
                                    String email, String role, String status) throws SQLException {
        if (!isAdmin()) return "You do not have permission to manage users.";
        String error = validate(username, fullName, email, role, status);
        if (error != null) return error;
        User duplicate = UserDAO.getUserByUsername(username);
        if (duplicate != null && duplicate.getId() != user.getId()) return "Username already exists.";
        user.setUsername(username.trim().toLowerCase());
        user.setPassword(password == null || password.isBlank() ? user.getPassword() : PasswordUtil.hash(password));
        user.setFullName(fullName.trim());
        user.setEmail(blankToNull(email));
        user.setRole(role);
        user.setStatus(status);
        UserDAO.updateUser(user);
        return null;
    }

    public static String disableUser(int id) throws SQLException {
        if (!isAdmin()) return "You do not have permission to manage users.";
        if (UserDAO.disableUser(id)) return "User disabled.";
        return "Could not disable the user.";
    }

    public static String changePassword(String currentPassword, String newPassword, String confirmPassword)
            throws SQLException {
        if (AuthService.currentUser() == null) return "Please sign in again.";
        if (currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            return "Please fill in every password field.";
        }
        if (newPassword.trim().isEmpty() || newPassword.contains(" ")) {
            return "The new password cannot contain spaces.";
        }
        User user = UserDAO.getUserById(AuthService.currentUser().id());
        if (user == null) return "Please sign in again.";

        if (!PasswordUtil.hash(currentPassword).equals(user.getPassword())) {
            return "Wrong current password.";
        }
        if (newPassword.equals(currentPassword)) {
            return "The new password must be different from the current one.";
        }
        if (newPassword.length() < 6) {
            return "The new password must be at least 6 characters.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "New passwords differ.";
        }
        user.setPassword(PasswordUtil.hash(newPassword));
        UserDAO.updateUser(user);
        return null;
    }

    private static boolean isAdmin() {
        return AuthService.currentUser() != null && AuthService.currentUser().role() == UserRole.ADMIN;
    }

    private static String validate(String username, String fullName, String email, String role, String status) {
        if (username == null || username.isBlank()) return "Username cannot be empty.";
        if (fullName == null || fullName.isBlank()) return "Full name cannot be empty.";
        if (email != null && !email.isBlank() && !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address.";
        }
        if (!"ADMIN".equals(role) && !"STAFF".equals(role) && !"MEMBER".equals(role)) return "Invalid role.";
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) return "Invalid status.";
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
