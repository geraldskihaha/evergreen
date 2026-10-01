package library.auth;

public enum UserRole {
    ADMIN("Admin"), STAFF("Staff"), MEMBER("Member");

    private final String displayName;

    UserRole(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
