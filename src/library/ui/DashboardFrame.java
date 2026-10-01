package library.ui;

import library.auth.AuthService;
import library.auth.AuthenticatedUser;
import library.dao.BookDAO;
import library.dao.UserDAO;
import library.model.Book;
import library.model.Borrowing;
import library.model.User;
import library.service.BookService;
import library.service.BorrowingService;
import library.service.UserService;
import library.util.FontManager;
import library.util.Logo;
import library.util.ThemeManager;
import static library.util.ThemeManager.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class DashboardFrame extends JFrame {

    private static final int PROFILE_LABEL_WIDTH = 190;

    private static final int PROFILE_FIELD_WIDTH = 420;

    private enum Glyph { BOOK, HOME, SEARCH, PLUS, CHECK, BORROW, RETURN, USERS, REPORT, TRASH, USER, LOGOUT, POWER, EXPORT }

    private String role = "Admin";
    private String view = "dashboard";
    private final AuthenticatedUser authenticatedUser;
    private final CardLayout views = new CardLayout();
    private final JPanel content = new JPanel(views);
    private final ArrayList<Book> currentBooks = new ArrayList<>();
    private DefaultTableModel bookModel;
    private JTable bookTable;
    private DefaultTableModel usersModel;
    private JTable userTable;
    private DefaultTableModel reportModel;
    private JTable reportTable;
    private JComboBox<String> statusFilter;
    private DefaultTableModel myBooksModel;
    private JTable myBooksTable;
    private JLabel usernameValue, fullNameValue, emailValue, roleValue, statusValue, createdValue;
    private JPasswordField currentPasswordField, newPasswordField, confirmPasswordField;
    private JLabel usersTotal;
    private DefaultTableModel recentModel;
    private JTable recentTable;
    private JLabel catalogTotal, availableTotal, loanTotal;
    private JTextField searchField;

    private JButton editBookButton, deleteBookButton, borrowSelfButton, editUserButton, disableUserButton;
    private JLabel catalogueHint, usersHint;

    private final HashMap<String, MenuButton> menuButtons = new HashMap<>();
    private String activeMenu = "Dashboard";

    public DashboardFrame(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser");
        this.role = authenticatedUser.role().displayName();
        ThemeManager.apply(FontManager.ui(Font.PLAIN, 13.5f));
        setTitle("Evergreen Library - Book Borrowing System");
        setIconImages(Logo.windowIcons());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1280, 780);
        setLocationRelativeTo(null);
        rebuild();
    }

    private Font ui(int style, float size) { return FontManager.ui(style, size); }
    private Font uiLabel(float size) { return FontManager.uiLabel(size); }
    private Font display(int style, float size) { return FontManager.display(style, size); }

    private void styleField(javax.swing.text.JTextComponent field) {
        field.setBackground(FIELD);
        field.setForeground(foreground());
        field.setCaretColor(MOSS);
        field.setFont(ui(Font.PLAIN, 13.5f));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(255, 255, 255, 28), 1, true),
                new EmptyBorder(0, 14, 0, 14)));
    }

    private void rebuild() {
        content.removeAll();
        content.add(dashboardMain(), "dashboard");
        content.add(booksMain(), "books");
        if (role.equals("Admin")) content.add(usersMain(), "users");
        if (!role.equals("Member")) content.add(reportsMain(), "reports");
        if (role.equals("Member")) content.add(myBooksMain(), "mybooks");
        content.add(profileMain(), "profile");

        JPanel root = panel(new BorderLayout(), background());
        root.add(sidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        views.show(content, view);
        markActive(activeMenu);

        refreshDashboard();
        revalidate();
        repaint();
    }

    private JPanel dashboardMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        main.add(topbar(), BorderLayout.NORTH);

        JPanel body = panel(new BorderLayout(0, 24), background());
        body.add(stats(), BorderLayout.NORTH);
        body.add(recentBorrowingsCard(), BorderLayout.CENTER);
        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private JPanel booksMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        main.add(pageHeader(role.equals("Member") ? "Browse Books" : "Books",
                "Browse, find, and manage books from one place.", true), BorderLayout.NORTH);
        main.add(catalogue(), BorderLayout.CENTER);
        return main;
    }

    private JPanel pageHeader(String title, String subtitle, boolean withSearch) {
        JPanel header = panel(new BorderLayout(), background());
        JPanel words = panel(new GridLayout(subtitle == null ? 1 : 2, 1, 0, 6), background());
        words.add(label(title, 30, foreground(), Font.BOLD));
        if (subtitle != null) words.add(label(subtitle, 13.5f, subdued(), Font.PLAIN));
        header.add(words, BorderLayout.WEST);
        if (withSearch) header.add(searchBox(background()), BorderLayout.EAST);
        return header;
    }

    private JPanel searchBox(Color bg) {
        JPanel finder = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0), bg);
        searchField = new JTextField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search books");
        styleField(searchField);
        searchField.setToolTipText("Search by title, author, ISBN, or category");
        searchField.addActionListener(e -> refreshTable());

        JButton go = toolButton("Search", Glyph.SEARCH, e -> refreshTable());
        int height = Math.max(CONTROL_HEIGHT, go.getPreferredSize().height);
        int fieldWidth = searchField.getPreferredSize().width;
        int buttonWidth = go.getPreferredSize().width;
        searchField.setPreferredSize(new Dimension(fieldWidth, height));
        searchField.setMinimumSize(new Dimension(200, height));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        go.setPreferredSize(new Dimension(buttonWidth, height));
        go.setMinimumSize(new Dimension(buttonWidth, height));

        finder.add(searchField);
        finder.add(go);
        return finder;
    }

    private JPanel usersMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        main.add(pageHeader("User Management", null, false), BorderLayout.NORTH);

        JPanel usersCard = roundedPanel(new BorderLayout(0, 16), surface());
        usersCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        usersModel = new DefaultTableModel(new String[]{"ID", "USERNAME", "FULL NAME", "EMAIL", "ROLE", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(usersModel);
        styleTable(userTable);
        fixedColumn(userTable, 0, 64, SwingConstants.CENTER);
        flexibleColumn(userTable, 1, 130, true);
        flexibleColumn(userTable, 2, 190, false);
        flexibleColumn(userTable, 3, 190, false);
        flexibleColumn(userTable, 4, 90, false);
        statusColumn(userTable, 5, 140);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelectionState();
        });

        editUserButton = toolButton("Edit user", Glyph.REPORT, e -> editUserDialog());
        disableUserButton = dangerButton("Disable user", Glyph.TRASH, e -> disableUserAction());

        usersHint = new JLabel("Select a user to continue");
        usersHint.setFont(ui(Font.PLAIN, 12.5f));
        usersHint.setForeground(subdued());
        usersCard.add(toolbar(primaryButton("Add user", Glyph.PLUS, e -> addUserDialog()),
                editUserButton, disableUserButton, usersHint), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(surface());
        usersCard.add(scroll, BorderLayout.CENTER);

        main.add(usersCard, BorderLayout.CENTER);
        refreshUsers();
        return main;
    }

    private void refreshUsers() {
        if (usersModel == null) return;
        try {
            usersModel.setRowCount(0);
            for (User u : UserService.getUsers()) {
                usersModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getFullName(),
                        u.getEmail() == null ? "-" : u.getEmail(), u.getRole(), u.getStatus()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
        updateSelectionState();
    }

    private User selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user first.", "Evergreen Library", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        try {
            return UserService.getUserById((Integer) usersModel.getValueAt(row, 0));
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void addUserDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField fullNameField = new JTextField();
        JTextField emailField = new JTextField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"ADMIN", "STAFF", "MEMBER"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "DISABLED"});
        Object[] message = {
            "Username:", usernameField,
            "Password:", passwordField,
            "Full Name:", fullNameField,
            "Email (optional):", emailField,
            "Role:", roleBox,
            "Status:", statusBox
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Add a new user", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;
        try {
            String error = UserService.addUser(usernameField.getText(), new String(passwordField.getPassword()),
                    fullNameField.getText(), emailField.getText(),
                    (String) roleBox.getSelectedItem(), (String) statusBox.getSelectedItem());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save the user.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editUserDialog() {
        User user = selectedUser();
        if (user == null) return;
        JTextField usernameField = new JTextField(user.getUsername());
        JPasswordField passwordField = new JPasswordField();
        passwordField.setToolTipText("Leave blank to keep the current password");
        JTextField fullNameField = new JTextField(user.getFullName());
        JTextField emailField = new JTextField(user.getEmail());
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"ADMIN", "STAFF", "MEMBER"});
        roleBox.setSelectedItem(user.getRole());
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "DISABLED"});
        statusBox.setSelectedItem(user.getStatus());
        Object[] message = {
            "Username:", usernameField,
            "Password (blank keeps current):", passwordField,
            "Full Name:", fullNameField,
            "Email (optional):", emailField,
            "Role:", roleBox,
            "Status:", statusBox
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Edit user", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;
        try {
            String error = UserService.updateUser(user, usernameField.getText(), new String(passwordField.getPassword()),
                    fullNameField.getText(), emailField.getText(),
                    (String) roleBox.getSelectedItem(), (String) statusBox.getSelectedItem());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save the user.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disableUserAction() {
        User user = selectedUser();
        if (user == null) return;
        if (user.getId() == authenticatedUser.id()) {
            JOptionPane.showMessageDialog(this, "You cannot disable your own account.", "Evergreen Library", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
            "Disable the account \"" + user.getUsername() + "\"?",
            "Evergreen Library",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) return;
        try {
            String outcome = UserService.disableUser(user.getId());
            JOptionPane.showMessageDialog(this, outcome, "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not disable the user.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel reportsMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        main.add(pageHeader("Reports", null, false), BorderLayout.NORTH);

        JPanel reportCard = roundedPanel(new BorderLayout(0, 16), surface());
        reportCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        reportModel = new DefaultTableModel(new String[]{"USER", "BOOK", "BORROW DATE", "DUE DATE", "RETURN DATE", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reportTable = new JTable(reportModel);
        styleTable(reportTable);
        flexibleColumn(reportTable, 0, 150, false);
        flexibleColumn(reportTable, 1, 220, true);
        flexibleColumn(reportTable, 2, 120, false);
        flexibleColumn(reportTable, 3, 120, false);
        flexibleColumn(reportTable, 4, 120, false);
        statusColumn(reportTable, 5, 140);

        JPanel filter = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0), surface());
        filter.add(label("Status:", 12.5f, subdued(), Font.PLAIN));
        statusFilter = new JComboBox<>(new String[]{"All", "BORROWED", "RETURNED"});
        statusFilter.setFont(ui(Font.PLAIN, 13.5f));
        statusFilter.setPreferredSize(new Dimension(170, CONTROL_HEIGHT));
        statusFilter.setMinimumSize(new Dimension(170, CONTROL_HEIGHT));
        statusFilter.addActionListener(e -> refreshReport());
        filter.add(statusFilter);

        JPanel bar = panel(new BorderLayout(12, 0), surface());
        bar.add(toolbar(toolButton("Export CSV", Glyph.EXPORT, e -> exportReportCsv())), BorderLayout.WEST);
        bar.add(filter, BorderLayout.EAST);
        reportCard.add(bar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(reportTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(surface());
        reportCard.add(scroll, BorderLayout.CENTER);

        main.add(reportCard, BorderLayout.CENTER);
        refreshReport();
        return main;
    }

    private void refreshReport() {
        if (reportModel == null) return;
        try {
            String filter = statusFilter == null ? "All" : (String) statusFilter.getSelectedItem();
            reportModel.setRowCount(0);
            for (Borrowing b : BorrowingService.getBorrowings()) {
                if (!"All".equals(filter) && !filter.equals(b.getStatus())) continue;
                User member = UserDAO.getUserById(b.getUserId());
                Book book = BookDAO.getBookById(b.getBookId());
                reportModel.addRow(new Object[]{
                        member != null ? member.getFullName() : "Unknown",
                        book != null ? book.getTitle() : "Unknown",
                        b.getBorrowDate(),
                        b.getDueDate(),
                        b.getReturnDate() == null ? "-" : b.getReturnDate(),
                        b.getStatus()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportReportCsv() {
        String filename = "borrowings-report.csv";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("User,Book,Borrow Date,Due Date,Return Date,Status");
            for (int row = 0; row < reportModel.getRowCount(); row++) {
                out.println(escape((String) reportModel.getValueAt(row, 0)) + ","
                        + escape((String) reportModel.getValueAt(row, 1)) + ","
                        + reportModel.getValueAt(row, 2) + ","
                        + reportModel.getValueAt(row, 3) + ","
                        + reportModel.getValueAt(row, 4) + ","
                        + reportModel.getValueAt(row, 5));
            }
            JOptionPane.showMessageDialog(this, "Report saved to " + filename, "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not write report: " + ex.getMessage(), "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics raw) {
                Graphics2D g = (Graphics2D) raw.create();
                g.setPaint(new GradientPaint(0, 0, new Color(0x12, 0x2C, 0x20),
                        0, getHeight(), new Color(0x0C, 0x20, 0x16)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        side.setPreferredSize(new Dimension(232, 0));

        side.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, ThemeManager.HAIRLINE),
                new EmptyBorder(26, 18, 20, 18)));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JLabel mark = new JLabel(Logo.icon(46));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(mark);
        top.add(Box.createVerticalStrut(10));
        JLabel wordmark = label("EVERGREEN", 22, Color.WHITE, Font.BOLD);
        wordmark.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(wordmark);
        top.add(Box.createVerticalStrut(4));
        JLabel portal = new JLabel("LIBRARY PORTAL");
        portal.setFont(FontManager.tracked(FontManager.uiLabel(10.5f), 0.14f));
        portal.setForeground(GOLD);
        portal.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(portal);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(40, 0, 0, 0));
        addMenu(menu, "Dashboard", Glyph.HOME, false);
        if (role.equals("Member")) addMenu(menu, "Browse books", Glyph.BOOK, false);
        else addMenu(menu, "Books", Glyph.BOOK, false);
        if (!role.equals("Member")) addMenu(menu, "Borrow book", Glyph.BORROW, false);
        if (!role.equals("Member")) addMenu(menu, "Return book", Glyph.RETURN, false);
        if (role.equals("Admin")) addMenu(menu, "Manage users", Glyph.USERS, false);
        if (!role.equals("Member")) addMenu(menu, "Reports", Glyph.REPORT, false);
        if (role.equals("Member")) addMenu(menu, "My Loans", Glyph.BOOK, false);
        addMenu(menu, "My profile", Glyph.USER, false);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        addMenu(bottom, "Log out", Glyph.LOGOUT, true);
        addMenu(bottom, "Exit application", Glyph.POWER, true);

        side.add(top, BorderLayout.NORTH);
        side.add(menu, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void addMenu(JPanel host, String text, Glyph glyph, boolean danger) {
        MenuButton b = new MenuButton(text, icon(glyph, danger ? new Color(255, 205, 194) : new Color(187, 222, 197), 17));
        b.setIconTextGap(12);
        b.setMaximumSize(new Dimension(196, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIdleForeground(danger ? new Color(255, 205, 194) : new Color(225, 240, 228));
        b.setFont(ui(Font.PLAIN, 13.5f));
        b.addActionListener(e -> {
            if (text.equals("Log out")) {
                if (!confirm("Log out of Evergreen Library?", "Log out")) return;
                AuthService.logout();
                dispose();
                new LoginFrame().setVisible(true);
            } else if (text.startsWith("Exit")) {
                if (!confirm("Close Evergreen Library?", "Exit")) return;
                dispose();
            } else if (text.equals("Borrow book")) {
                borrowBook();
            } else if (text.equals("Return book")) {
                returnBook();
            } else if (text.equals("Dashboard")) {
                view = "dashboard";
                views.show(content, view);
                refreshDashboard();
            } else if (text.equals("Books") || text.equals("Browse books")) {
                view = "books";
                views.show(content, view);
                refreshTable();
            } else if (text.equals("Manage users")) {
                view = "users";
                views.show(content, view);
            } else if (text.equals("Reports")) {
                view = "reports";
                views.show(content, view);
                refreshReport();
            } else if (text.equals("My Loans")) {
                view = "mybooks";
                views.show(content, view);
                refreshMyBooks();
            } else if (text.equals("My profile")) {
                view = "profile";
                views.show(content, view);
                refreshProfile();
            }
            if (isViewItem(text)) markActive(text);
        });
        if (isViewItem(text)) menuButtons.put(text, b);
        host.add(b);
        host.add(Box.createVerticalStrut(4));
    }

    private boolean isViewItem(String text) {
        return text.equals("Dashboard") || text.equals("Books") || text.equals("Browse books")
                || text.equals("Manage users") || text.equals("Reports")
                || text.equals("My Loans") || text.equals("My profile");
    }

    private void markActive(String text) {
        activeMenu = text;
        for (java.util.Map.Entry<String, MenuButton> entry : menuButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(text));
        }
    }

    private JPanel topbar() {
        JPanel p = panel(new BorderLayout(), background());
        JPanel title = panel(new GridLayout(2, 1, 0, 4), background());
        title.add(label(role + " Dashboard", 30, foreground(), Font.BOLD));
        title.add(label("Welcome, " + authenticatedUser.fullName() + ". Here is your library at a glance.", 13.5f, subdued(), Font.PLAIN));

        JPanel tools = panel(new FlowLayout(FlowLayout.RIGHT, 10, 0), background());

        tools.add(new RoleBadge(role.toUpperCase(), role.equals("Admin") ? GOLD : MOSS));

        p.add(title, BorderLayout.WEST);
        p.add(tools, BorderLayout.EAST);
        return p;
    }

    private JPanel stats() {
        JPanel s = panel(new GridLayout(1, 4, 16, 0), background());
        catalogTotal = new JLabel();
        availableTotal = new JLabel();
        loanTotal = new JLabel();
        usersTotal = new JLabel();
        s.add(statCard("TOTAL BOOKS", catalogTotal, "Titles in the collection", new Color(139, 209, 155), Glyph.BOOK));
        s.add(statCard("AVAILABLE BOOKS", availableTotal, "Ready for the next reader", MOSS, Glyph.CHECK));
        s.add(statCard("BORROWED BOOKS", loanTotal, "Currently on loan", GOLD, Glyph.BORROW));
        s.add(statCard("TOTAL USERS", usersTotal, "Registered accounts", new Color(202, 220, 206), Glyph.USERS));
        return s;
    }

    private JPanel statCard(String heading, JLabel value, String caption, Color accent, Glyph glyph) {
        JPanel p = roundedPanel(new BorderLayout(0, 10), surface());
        p.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel top = panel(new BorderLayout(), surface());
        top.add(cardLabel(heading), BorderLayout.WEST);
        top.add(new GlyphBadge(icon(glyph, accent, 16), accent), BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        value.setFont(display(Font.BOLD, 34));
        value.setForeground(accent);
        p.add(value, BorderLayout.CENTER);
        p.add(label(caption, 12.5f, subdued(), Font.PLAIN), BorderLayout.SOUTH);
        return p;
    }

    private final class RoleBadge extends JLabel {
        private final Color accent;

        RoleBadge(String text, Color accent) {
            super(text);
            this.accent = accent;
            setFont(FontManager.tracked(uiLabel(10.5f), 0.12f));
            setForeground(accent);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 14, 8, 14));
        }

        @Override protected void paintComponent(Graphics raw) {
            Graphics2D g = (Graphics2D) raw.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
            g.setColor(ThemeManager.tint(accent, 28));
            g.fill(shape);
            g.setColor(ThemeManager.tint(accent, 70));
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
            g.dispose();
            super.paintComponent(raw);
        }
    }

    private final class GlyphBadge extends JLabel {
        private final Color tint;

        GlyphBadge(Icon glyph, Color accent) {
            super(glyph);
            this.tint = ThemeManager.tint(accent, 36);
            setOpaque(false);
            setBorder(new EmptyBorder(7, 7, 7, 7));
        }

        @Override protected void paintComponent(Graphics raw) {
            Graphics2D g = (Graphics2D) raw.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(tint);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(raw);
        }
    }

    private JPanel recentBorrowingsCard() {
        JPanel card = roundedPanel(new BorderLayout(0, 16), surface());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.add(label("Recent Borrowings", 21, foreground(), Font.BOLD), BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"USER", "BOOK", "DATE", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        recentTable = new JTable(recentModel);
        styleTable(recentTable);
        flexibleColumn(recentTable, 0, 150, false);
        flexibleColumn(recentTable, 1, 220, true);
        flexibleColumn(recentTable, 2, 130, false);
        statusColumn(recentTable, 3, 140);
        JScrollPane scroll = new JScrollPane(recentTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 230));
        scroll.getViewport().setBackground(surface());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void refreshDashboard() {
        try {
            java.util.List<Book> books = BookService.getBooks();
            int availableBooks = 0;
            int borrowedBooks = 0;
            for (Book b : books) {
                if (b.isBorrowed()) borrowedBooks++;
                else availableBooks++;
            }
            if (catalogTotal != null) {
                catalogTotal.setText("" + books.size());
                availableTotal.setText("" + availableBooks);
                loanTotal.setText("" + borrowedBooks);
                usersTotal.setText("" + UserService.getUsers().size());
            }
            if (recentModel != null) {
                recentModel.setRowCount(0);
                int shown = 0;
                for (Borrowing b : BorrowingService.getBorrowings()) {
                    if (shown >= 8) break;
                    User member = UserDAO.getUserById(b.getUserId());
                    Book book = BookDAO.getBookById(b.getBookId());
                    recentModel.addRow(new Object[]{
                            member != null ? member.getFullName() : "Unknown",
                            book != null ? book.getTitle() : "Unknown",
                            b.getBorrowDate(),
                            b.getStatus()});
                    shown++;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel catalogue() {
        JPanel p = roundedPanel(new BorderLayout(0, 16), surface());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        bookModel = new DefaultTableModel(new String[]{"ISBN", "TITLE", "AUTHOR", "CATEGORY", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = new JTable(bookModel);
        styleTable(bookTable);
        flexibleColumn(bookTable, 0, 150, false);
        flexibleColumn(bookTable, 1, 260, true);
        flexibleColumn(bookTable, 2, 170, false);
        flexibleColumn(bookTable, 3, 130, false);
        statusColumn(bookTable, 4, 140);
        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelectionState();
        });

        catalogueHint = new JLabel(role.equals("Member") ? "Select a book to borrow it" : "Select a book to continue");
        catalogueHint.setFont(ui(Font.PLAIN, 12.5f));
        catalogueHint.setForeground(subdued());

        JPanel actions;
        if (!role.equals("Member")) {
            JButton borrow = toolButton("Borrow book", Glyph.BORROW, e -> borrowBook());
            JButton giveBack = toolButton("Return book", Glyph.RETURN, e -> returnBook());
            if (role.equals("Admin")) {
                editBookButton = toolButton("Edit book", Glyph.REPORT, e -> editSelected());
                deleteBookButton = dangerButton("Delete", Glyph.TRASH, e -> deleteSelected());

                actions = toolbar(primaryButton("Add book", Glyph.PLUS, e -> addBook()), borrow, giveBack,
                        editBookButton, toolButton("Export report", Glyph.EXPORT, e -> exportReport()),
                        Box.createHorizontalStrut(18), deleteBookButton, catalogueHint);
            } else {
                actions = toolbar(primaryButton("Add book", Glyph.PLUS, e -> addBook()), borrow, giveBack);
            }
        } else {
            borrowSelfButton = primaryButton("Borrow this book", Glyph.BORROW, e -> borrowSelectedBook());
            actions = toolbar(borrowSelfButton,
                    toolButton("Return a book", Glyph.RETURN, e -> returnOwnBook()), catalogueHint);
        }

        p.add(actions, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(bookTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(surface());
        p.add(scroll, BorderLayout.CENTER);

        refreshTable();
        return p;
    }

    private boolean confirm(String message, String confirmLabel) {
        Object[] options = {confirmLabel, "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, message, "Evergreen Library",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    private JPanel toolbar(java.awt.Component... items) {
        JPanel bar = panel(new WrapLayout(), surface());
        for (java.awt.Component item : items) bar.add(item);
        return bar;
    }

    private void updateSelectionState() {
        if (bookTable != null) {
            boolean selected = bookTable.getSelectedRow() >= 0;
            boolean dependent = editBookButton != null || deleteBookButton != null || borrowSelfButton != null;
            if (editBookButton != null) editBookButton.setEnabled(selected);
            if (deleteBookButton != null) deleteBookButton.setEnabled(selected);
            if (borrowSelfButton != null) borrowSelfButton.setEnabled(selected);
            if (catalogueHint != null) {
                catalogueHint.setVisible(dependent && !selected && bookTable.getRowCount() > 0);
            }
        }
        if (userTable != null) {
            boolean selected = userTable.getSelectedRow() >= 0;
            if (editUserButton != null) editUserButton.setEnabled(selected);
            if (disableUserButton != null) disableUserButton.setEnabled(selected);
            if (usersHint != null) {
                usersHint.setVisible(!selected && userTable.getRowCount() > 0);
            }
        }
    }

    private void addBook() {
        JTextField isbnField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField categoryField = new JTextField();
        Object[] message = {
            "ISBN:", isbnField,
            "Title:", titleField,
            "Author:", authorField,
            "Category:", categoryField
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Add a new book", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;

        try {
            String error = BookService.addBook(isbnField.getText().trim(), titleField.getText().trim(),
                    authorField.getText().trim(), categoryField.getText().trim());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save the book.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        Book book = selectedBook();
        if (book == null) return;
        JTextField isbnField = new JTextField(book.getIsbn());
        JTextField titleField = new JTextField(book.getTitle());
        JTextField authorField = new JTextField(book.getAuthor());
        JTextField categoryField = new JTextField(book.getCategory());
        Object[] message = {
            "ISBN:", isbnField,
            "Title:", titleField,
            "Author:", authorField,
            "Category:", categoryField
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Edit book", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) return;

        try {
            String error = BookService.updateBook(book, isbnField.getText().trim(), titleField.getText().trim(),
                    authorField.getText().trim(), categoryField.getText().trim());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save the book.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void borrowBook() {
        try {
            java.util.List<User> members = new ArrayList<>();
            for (User u : UserDAO.getUsers()) {
                if ("MEMBER".equals(u.getRole()) && "ACTIVE".equals(u.getStatus())) members.add(u);
            }
            if (members.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No member accounts found. Please create member accounts first.", "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            java.util.List<Book> available = new ArrayList<>();
            for (Book b : BookService.getBooks()) {
                if ("ACTIVE".equals(b.getStatus()) && !b.isBorrowed()) available.add(b);
            }
            if (available.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No books are currently available.", "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JComboBox<String> memberBox = new JComboBox<>(members.stream()
                    .map(u -> u.getFullName() + " (" + u.getUsername() + ")").toArray(String[]::new));
            JComboBox<String> bookBox = new JComboBox<>(available.stream()
                    .map(b -> b.getTitle() + "  [" + b.getIsbn() + "]").toArray(String[]::new));
            JTextField dueField = new JTextField(LocalDate.now().plusDays(14).toString());
            Object[] message = {
                "Member:", memberBox,
                "Book:", bookBox,
                "Due date (yyyy-MM-dd):", dueField
            };
            int option = JOptionPane.showConfirmDialog(this, message, "Borrow a book", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) return;

            String error = BorrowingService.borrowBook(
                    members.get(memberBox.getSelectedIndex()).getId(),
                    available.get(bookBox.getSelectedIndex()).getId(),
                    dueField.getText());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Book borrowed successfully.", "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not complete the borrow.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void borrowSelectedBook() {
        Book book = selectedBook();
        if (book == null) return;
        if (book.isBorrowed()) {
            JOptionPane.showMessageDialog(this, "This book is already borrowed.",
                    "Evergreen Library", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "Borrow \"" + book.getTitle() + "\"?\nDue back in 14 days.",
                "Evergreen Library", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option != JOptionPane.YES_OPTION) return;
        try {
            String error = BorrowingService.borrowForSelf(book.getId());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Book borrowed successfully.",
                    "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            refreshMyBooks();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not complete the borrow.",
                    "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void returnOwnBook() {
        try {
            java.util.List<Borrowing> open = new ArrayList<>();
            for (Borrowing b : BorrowingService.getBorrowingsByUser(authenticatedUser.id())) {
                if ("BORROWED".equals(b.getStatus())) open.add(b);
            }
            if (open.isEmpty()) {
                JOptionPane.showMessageDialog(this, "You have no borrowed books to return.",
                        "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] labels = new String[open.size()];
            for (int i = 0; i < open.size(); i++) {
                Borrowing b = open.get(i);
                Book book = BookDAO.getBookById(b.getBookId());
                labels[i] = (book != null ? book.getTitle() : "Unknown") + " (due " + b.getDueDate() + ")";
            }
            JComboBox<String> loanBox = new JComboBox<>(labels);
            Object[] message = { "Select the book to return:", loanBox };
            int option = JOptionPane.showConfirmDialog(this, message, "Return a book",
                    JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) return;

            String error = BorrowingService.returnForSelf(open.get(loanBox.getSelectedIndex()).getId());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Book returned successfully.",
                    "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            refreshMyBooks();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not complete the return.",
                    "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void returnBook() {
        try {
            java.util.List<Borrowing> open = new ArrayList<>();
            for (Borrowing b : BorrowingService.getBorrowings()) {
                if ("BORROWED".equals(b.getStatus())) open.add(b);
            }
            if (open.isEmpty()) {
                JOptionPane.showMessageDialog(this, "There are no borrowed books to return.", "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] labels = new String[open.size()];
            for (int i = 0; i < open.size(); i++) {
                Borrowing b = open.get(i);
                User member = UserDAO.getUserById(b.getUserId());
                Book book = BookDAO.getBookById(b.getBookId());
                labels[i] = (member != null ? member.getFullName() : "Unknown") + " - "
                        + (book != null ? book.getTitle() : "Unknown") + " (due " + b.getDueDate() + ")";
            }
            JComboBox<String> borrowBox = new JComboBox<>(labels);
            Object[] message = { "Select the borrowing to return:", borrowBox };
            int option = JOptionPane.showConfirmDialog(this, message, "Return a book", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) return;

            String error = BorrowingService.returnBook(open.get(borrowBox.getSelectedIndex()).getId());
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Book returned successfully.", "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not complete the return.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        Book book = selectedBook();
        if (book == null) return;
        int option = JOptionPane.showConfirmDialog(this,
            "Permanently delete \"" + book.getTitle() + "\"?\n\n"
            + "Any past borrowing records for this book will be removed as well.\n"
            + "This cannot be undone.",
            "Evergreen Library",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) return;
        try {
            String outcome = BookService.deleteBook(book);
            JOptionPane.showMessageDialog(this, outcome, "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not delete the book.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportReport() {
        String filename = "catalogue-report.csv";
        try {
            java.util.List<Book> books = BookService.getBooks();
            try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
                out.println("ISBN,Title,Author,Category,Availability,Status");
                for (Book b : books) {
                    out.println(escape(b.getIsbn()) + "," + escape(b.getTitle()) + "," + escape(b.getAuthor()) + ","
                        + escape(b.getCategory()) + "," + b.getAvailability() + "," + b.getStatus());
                }
            }
            JOptionPane.showMessageDialog(this, "Report saved to " + filename, "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not write report: " + ex.getMessage(), "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private Book selectedBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first.", "Evergreen Library", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String isbn = (String) bookModel.getValueAt(row, 0);
        for (Book b : currentBooks) {
            if (b.getIsbn().equals(isbn)) return b;
        }
        return null;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(44);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(47, 85, 62));
        t.setSelectionForeground(foreground());
        t.setForeground(foreground());
        t.setBackground(surface());
        t.setFont(ui(Font.PLAIN, 13.5f));
        t.setFillsViewportHeight(false);

        JTableHeader h = t.getTableHeader();
        h.setBackground(SIDEBAR);
        h.setForeground(Color.WHITE);
        h.setFont(FontManager.tracked(uiLabel(11.5f), 0.06f));
        h.setPreferredSize(new Dimension(0, 44));

        h.setReorderingAllowed(false);
        h.setResizingAllowed(false);
        h.setBorder(new MatteBorder(0, 0, 1, 0, ThemeManager.tint(GOLD, 70)));
    }

    private void flexibleColumn(JTable t, int index, int width, boolean primary) {
        column(t, index, width, false, primary, SwingConstants.LEFT);
    }

    private void fixedColumn(JTable t, int index, int width, int align) {
        column(t, index, width, true, false, align);
    }

    private void statusColumn(JTable t, int index, int width) {
        TableColumn c = t.getColumnModel().getColumn(index);
        c.setPreferredWidth(width);
        c.setMinWidth(width);
        c.setMaxWidth(width);
        c.setHeaderRenderer(headerRenderer(SwingConstants.LEFT));
        c.setCellRenderer(new StatusRenderer());
    }

    private void column(JTable t, int index, int width, boolean fixed, boolean primary, int align) {
        TableColumn c = t.getColumnModel().getColumn(index);
        c.setPreferredWidth(width);
        c.setMinWidth(Math.min(width, 64));
        if (fixed) c.setMaxWidth(width);
        c.setHeaderRenderer(headerRenderer(align));
        c.setCellRenderer(new CellRenderer(primary, align));
    }

    private DefaultTableCellRenderer headerRenderer(int align) {
        DefaultTableCellRenderer h = new DefaultTableCellRenderer();
        h.setHorizontalAlignment(align);
        h.setFont(FontManager.tracked(uiLabel(11.5f), 0.06f));
        h.setBackground(SIDEBAR);
        h.setForeground(Color.WHITE);
        h.setOpaque(true);
        h.setBorder(new EmptyBorder(0, 14, 0, 14));
        return h;
    }

    private final class CellRenderer extends DefaultTableCellRenderer {
        private final boolean primary;
        private final int align;

        CellRenderer(boolean primary, int align) {
            this.primary = primary;
            this.align = align;
            setBorder(new EmptyBorder(0, 14, 0, 14));
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setHorizontalAlignment(align);
            setFont(ui(primary ? Font.BOLD : Font.PLAIN, 13.5f));
            setForeground(primary ? foreground() : subdued());
            if (!selected) setBackground(row % 2 == 0 ? surface() : ThemeManager.RAISED);
            return this;
        }
    }

    private JButton primaryButton(String text, Glyph glyph, java.awt.event.ActionListener onClick) {
        JButton b = new JButton(text, icon(glyph, Color.WHITE, 16));
        b.setIconTextGap(9);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(uiLabel(12.5f));
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(onClick);
        return b;
    }

    private JButton toolButton(String text, Glyph glyph, java.awt.event.ActionListener onClick) {
        JButton b = new JButton(text, icon(glyph, TOOL_TEXT, 16));
        b.setIconTextGap(9);
        b.setBackground(TOOL_BG);
        b.setForeground(TOOL_TEXT);
        b.setFont(uiLabel(12.5f));
        b.setBorder(new CompoundBorder(
                new LineBorder(TOOL_EDGE, 1, true),
                new EmptyBorder(9, 15, 9, 15)));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(onClick);
        return b;
    }

    private JButton dangerButton(String text, Glyph glyph, java.awt.event.ActionListener onClick) {
        JButton b = new JButton(text, icon(glyph, DANGER_ON, 16));
        b.setIconTextGap(9);
        b.setBackground(DANGER_FILL);
        b.setForeground(DANGER_ON);
        b.setFont(uiLabel(12.5f));
        b.setBorder(dangerBorder(true));

        b.addPropertyChangeListener("enabled", e -> b.setBorder(dangerBorder(b.isEnabled())));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.addActionListener(onClick);
        return b;
    }

    private Border dangerBorder(boolean armed) {
        return new CompoundBorder(
                new LineBorder(armed ? DANGER_EDGE : TOOL_EDGE, 1, true),
                new EmptyBorder(9, 15, 9, 15));
    }

    private void refreshTable() {
        if (bookModel == null) return;
        try {
            String q = searchField == null ? "" : searchField.getText().trim();
            java.util.List<Book> books = q.isEmpty() ? BookService.getBooks() : BookService.searchBooks(q);
            currentBooks.clear();
            currentBooks.addAll(books);
            bookModel.setRowCount(0);
            int availableBooks = 0;
            int borrowedBooks = 0;
            for (Book b : books) {
                if (b.isBorrowed()) borrowedBooks++;
                else availableBooks++;
                bookModel.addRow(new Object[]{b.getIsbn(), b.getTitle(), b.getAuthor(), b.getCategory(),
                        b.getAvailability()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
        updateSelectionState();
    }

    private JPanel profileMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        JPanel header = pageHeader("My Profile", null, false);

        JPanel infoCard = roundedPanel(new BorderLayout(0, 16), surface());
        infoCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        infoCard.add(label("Account details", 21, foreground(), Font.BOLD), BorderLayout.NORTH);

        usernameValue = valueLabel();
        fullNameValue = valueLabel();
        emailValue = valueLabel();
        roleValue = valueLabel();
        statusValue = valueLabel();
        createdValue = valueLabel();

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(detailRow("Username", usernameValue, true));
        rows.add(detailRow("Full Name", fullNameValue, true));
        rows.add(detailRow("Email", emailValue, true));
        rows.add(detailRow("Role", roleValue, true));
        rows.add(detailRow("Status", statusValue, true));
        rows.add(detailRow("Account Created", createdValue, false));
        infoCard.add(rows, BorderLayout.CENTER);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(infoCard);
        column.add(Box.createVerticalStrut(24));
        JPanel passwordCard = changePasswordCard();
        passwordCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(passwordCard);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(header);
        stack.add(Box.createVerticalStrut(24));
        stack.add(column);
        main.add(stack, BorderLayout.NORTH);
        refreshProfile();
        return main;
    }

    private JLabel valueLabel() {
        JLabel value = new JLabel();
        value.setFont(ui(Font.BOLD, 13.5f));
        value.setForeground(foreground());
        return value;
    }

    private JPanel detailRow(String name, JLabel value, boolean rule) {
        JPanel row = panel(new BorderLayout(16, 0), surface());
        row.add(profileLabel(name), BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, rule ? 1 : 0, 0, ThemeManager.HAIRLINE),
                new EmptyBorder(10, 0, 10, 0)));
        return row;
    }

    private JPanel fieldRow(String name, JComponent field) {
        JPanel row = panel(new BorderLayout(16, 0), surface());
        row.add(profileLabel(name), BorderLayout.WEST);

        JPanel holder = panel(new FlowLayout(FlowLayout.LEFT, 0, 0), surface());
        holder.add(field);
        row.add(holder, BorderLayout.CENTER);
        return row;
    }

    private JLabel profileLabel(String name) {
        JLabel label = new JLabel(name);
        label.setFont(ui(Font.PLAIN, 12.5f));
        label.setForeground(subdued());
        label.setPreferredSize(new Dimension(PROFILE_LABEL_WIDTH, 20));
        return label;
    }

    private JPanel changePasswordCard() {
        JPanel card = roundedPanel(new BorderLayout(0, 16), surface());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.add(label("Change Password", 21, foreground(), Font.BOLD), BorderLayout.NORTH);

        currentPasswordField = new JPasswordField();
        newPasswordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        for (JPasswordField field : new JPasswordField[]{currentPasswordField, newPasswordField, confirmPasswordField}) {
            styleField(field);
            field.setPreferredSize(new Dimension(PROFILE_FIELD_WIDTH, CONTROL_HEIGHT));
            field.setMinimumSize(new Dimension(260, CONTROL_HEIGHT));
            field.setMaximumSize(new Dimension(PROFILE_FIELD_WIDTH, CONTROL_HEIGHT));
        }

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(fieldRow("Current Password", currentPasswordField));
        rows.add(Box.createVerticalStrut(14));
        rows.add(fieldRow("New Password", newPasswordField));
        rows.add(Box.createVerticalStrut(14));
        rows.add(fieldRow("Confirm New Password", confirmPasswordField));
        card.add(rows, BorderLayout.CENTER);

        JPanel actions = panel(new BorderLayout(0, 0), surface());
        actions.add(Box.createHorizontalStrut(PROFILE_LABEL_WIDTH + 16), BorderLayout.WEST);
        JPanel buttons = panel(new WrapLayout(), surface());
        buttons.add(primaryButton("Save", Glyph.USER, e -> changeOwnPassword()));
        buttons.add(label("At least 6 characters", 12.5f, subdued(), Font.PLAIN));
        actions.add(buttons, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void changeOwnPassword() {
        try {
            String error = UserService.changePassword(
                    new String(currentPasswordField.getPassword()),
                    new String(newPasswordField.getPassword()),
                    new String(confirmPasswordField.getPassword()));
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Evergreen Library", JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            JOptionPane.showMessageDialog(this, "Password changed successfully.",
                    "Evergreen Library", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not change the password.",
                    "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshProfile() {
        try {
            User u = UserService.getUserById(authenticatedUser.id());
            if (u != null && usernameValue != null) {
                usernameValue.setText(u.getUsername());
                fullNameValue.setText(u.getFullName());
                emailValue.setText(u.getEmail() == null || u.getEmail().isBlank() ? "-" : u.getEmail());
                roleValue.setText(u.getRole());
                statusValue.setText(u.getStatus());
                createdValue.setText(u.getCreatedAt() == null || u.getCreatedAt().isBlank() ? "-" : u.getCreatedAt());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel myBooksMain() {
        JPanel main = panel(new BorderLayout(0, 24), background());
        main.setBorder(new EmptyBorder(24, 28, 24, 36));
        main.add(pageHeader("My Borrowed Books", null, false), BorderLayout.NORTH);

        JPanel card = roundedPanel(new BorderLayout(0, 16), surface());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        myBooksModel = new DefaultTableModel(new String[]{"BOOK", "BORROW DATE", "DUE DATE", "RETURN DATE", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        myBooksTable = new JTable(myBooksModel);
        styleTable(myBooksTable);
        flexibleColumn(myBooksTable, 0, 260, true);
        flexibleColumn(myBooksTable, 1, 130, false);
        flexibleColumn(myBooksTable, 2, 130, false);
        flexibleColumn(myBooksTable, 3, 130, false);
        statusColumn(myBooksTable, 4, 140);
        JScrollPane scroll = new JScrollPane(myBooksTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(surface());
        card.add(scroll, BorderLayout.CENTER);
        main.add(card, BorderLayout.CENTER);
        refreshMyBooks();
        return main;
    }

    private void refreshMyBooks() {
        if (myBooksModel == null) return;
        try {
            myBooksModel.setRowCount(0);
            for (Borrowing b : BorrowingService.getBorrowingsByUser(authenticatedUser.id())) {
                Book book = BookDAO.getBookById(b.getBookId());
                myBooksModel.addRow(new Object[]{
                        book != null ? book.getTitle() : "Unknown",
                        b.getBorrowDate(),
                        b.getDueDate(),
                        b.getReturnDate() == null ? "-" : b.getReturnDate(),
                        b.getStatus()});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Icon icon(Glyph glyph, Color color, int size) {
        return switch (glyph) {
            case BOOK -> LucideIcon.book(color, size);
            case HOME -> LucideIcon.home(color, size);
            case SEARCH -> LucideIcon.search(color, size);
            case PLUS -> LucideIcon.plus(color, size);
            case CHECK -> LucideIcon.check(color, size);
            case BORROW -> LucideIcon.borrow(color, size);
            case RETURN -> LucideIcon.giveBack(color, size);
            case USERS -> LucideIcon.users(color, size);
            case REPORT -> LucideIcon.report(color, size);
            case TRASH -> LucideIcon.trash(color, size);
            case USER -> LucideIcon.user(color, size);
            case LOGOUT -> LucideIcon.logout(color, size);
            case POWER -> LucideIcon.power(color, size);
            case EXPORT -> LucideIcon.export(color, size);
        };
    }

    private JPanel panel(LayoutManager l, Color bg) {
        JPanel p = new JPanel(l);

        if (bg == surface()) {
            p.setOpaque(false);
        } else {
            p.setBackground(bg);
        }
        return p;
    }

    private JPanel roundedPanel(LayoutManager l, Color bg) {
        JPanel p = new CardPanel(l);
        p.setBackground(bg);
        return p;
    }

    private JLabel label(String text, float size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(size >= 18 ? display(style, size) : ui(style, size));
        l.setForeground(color);
        return l;
    }

    private JLabel cardLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FontManager.tracked(uiLabel(11.5f), 0.06f));
        l.setForeground(subdued());
        return l;
    }

    private Color background() { return WINDOW; }
    private Color surface() { return SURFACE; }
    private Color foreground() { return TEXT; }
    private Color subdued() { return SUBDUED; }

    private static final class WrapLayout extends FlowLayout {
        WrapLayout() { super(FlowLayout.LEFT, 8, 4); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension size = layoutSize(target, false);
            size.width -= 8;
            return size;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component member = target.getComponent(i);
                    if (!member.isVisible()) continue;
                    Dimension size = preferred ? member.getPreferredSize() : member.getMinimumSize();
                    if (rowWidth + size.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += size.width;
                    rowHeight = Math.max(rowHeight, size.height);
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }

    private static final class StatusPill extends javax.swing.JComponent {
        private String text = "";
        private Color accent = new Color(106, 183, 123);

        StatusPill() { setFont(FontManager.uiLabel(11.5f)); }

        void set(String value, Color color) {
            String next = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
            if (!next.equals(text) || !color.equals(accent)) {
                text = next;
                accent = color;
                revalidate();
                repaint();
            }
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(text) + 40, 24);
        }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics raw) {
            Graphics2D g = (Graphics2D) raw.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int width = Math.min(getWidth(), getPreferredSize().width);
            int height = 22;
            int top = (getHeight() - height) / 2;
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 38));
            g.fillRoundRect(0, top, width, height, height, height);
            g.setColor(accent);
            g.fillOval(11, top + (height - 6) / 2, 6, 6);
            g.drawString(text, 25, top + (height + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    private static Color statusAccent(Object value) {
        String text = value == null ? "" : value.toString().trim().toUpperCase(java.util.Locale.ROOT);
        if (text.startsWith("BORROW")) return GOLD;
        if (text.startsWith("DISABLED") || text.startsWith("INACTIVE")) return new Color(0xF0, 0xA0, 0x90);
        if (text.startsWith("AVAIL") || text.startsWith("ACTIVE") || text.startsWith("RETURN")) return new Color(0x8B, 0xD1, 0x9B);
        return SUBDUED;
    }

    private final class StatusRenderer implements TableCellRenderer {
        private final JPanel row = new JPanel();
        private final StatusPill pill = new StatusPill();

        StatusRenderer() {
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setOpaque(true);
            row.add(Box.createHorizontalStrut(14));
            row.add(pill);
            row.add(Box.createHorizontalGlue());
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int rowIndex, int column) {
            pill.set(value == null ? "" : value.toString(), statusAccent(value));
            row.setBackground(selected ? table.getSelectionBackground()
                    : (rowIndex % 2 == 0 ? surface() : ThemeManager.RAISED));
            return row;
        }
    }

    private final class MenuButton extends JButton {
        private boolean active;
        private boolean keyboardFocus;
        private Color idle = Color.WHITE;

        MenuButton(String text, Icon icon) {
            super(text, icon);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(10, 12, 10, 8));
            putClientProperty("JButton.buttonType", "borderless");

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) {

                    java.awt.event.FocusEvent.Cause cause = e.getCause();
                    keyboardFocus = cause == java.awt.event.FocusEvent.Cause.TRAVERSAL
                            || cause == java.awt.event.FocusEvent.Cause.TRAVERSAL_FORWARD
                            || cause == java.awt.event.FocusEvent.Cause.TRAVERSAL_BACKWARD;
                    repaint();
                }
                @Override public void focusLost(java.awt.event.FocusEvent e) {
                    keyboardFocus = false;
                    repaint();
                }
            });
        }

        void setIdleForeground(Color color) {
            idle = color;
            setForeground(color);
        }

        void setActive(boolean value) {
            if (active != value) {
                active = value;
                setForeground(value ? Color.WHITE : idle);
                repaint();
            }
        }

        @Override protected void paintComponent(Graphics raw) {
            if (active || keyboardFocus) {
                Graphics2D g = (Graphics2D) raw.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g.setColor(ACTIVE_BG);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                if (keyboardFocus) {
                    g.setColor(GOLD);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                }
                g.dispose();
            }
            super.paintComponent(raw);
        }
    }

}
