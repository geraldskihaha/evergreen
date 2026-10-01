package library.auth;

import java.sql.SQLException;
import library.dao.UserDAO;
import library.model.User;
import library.util.PasswordUtil;

public final class AuthService {

    public record LoginResult(Status status, AuthenticatedUser user) {
        public enum Status { SUCCESS, INVALID_CREDENTIALS, ACCOUNT_DISABLED, DATABASE_ERROR }
    }

    private static AuthenticatedUser currentUser;

    private AuthService() { }

    public static AuthenticatedUser currentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    public static LoginResult authenticate(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return new LoginResult(LoginResult.Status.INVALID_CREDENTIALS, null);
        }
        try {
            User user = UserDAO.login(username, new String(password));
            if (user == null) return new LoginResult(LoginResult.Status.INVALID_CREDENTIALS, null);
            if (!"ACTIVE".equals(user.getStatus())) return new LoginResult(LoginResult.Status.ACCOUNT_DISABLED, null);
            currentUser = new AuthenticatedUser(user.getId(), user.getUsername(), user.getFullName(), UserRole.valueOf(user.getRole()));
            return new LoginResult(LoginResult.Status.SUCCESS, currentUser);
        } catch (SQLException | IllegalArgumentException ex) {
            ex.printStackTrace();
            return new LoginResult(LoginResult.Status.DATABASE_ERROR, null);
        }
    }
}
