package library.auth;

public record AuthenticatedUser(int id, String username, String fullName, UserRole role) { }
