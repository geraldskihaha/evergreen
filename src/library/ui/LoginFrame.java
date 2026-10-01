package library.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import library.auth.AuthService;
import library.util.FontManager;
import library.util.Logo;
import library.util.ThemeManager;
import static library.util.ThemeManager.*;

public final class LoginFrame extends JFrame {

    private static final int FIELD_HEIGHT = 44;

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    public LoginFrame() {

        ThemeManager.apply(ui(Font.PLAIN, 13.5f));
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        errorLabel = new JLabel(" ");
        setTitle("Evergreen Library - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setMinimumSize(new Dimension(1050, 700));
        setSize(1280, 780);
        setLocationRelativeTo(null);

        setIconImages(Logo.windowIcons());
        wireFields();
        styleFields();
        rebuild();

        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    private Font ui(int style, float size) { return FontManager.ui(style, size); }
    private Font uiLabel(float size) { return FontManager.uiLabel(size); }
    private Font display(int style, float size) { return FontManager.display(style, size); }

    private void styleFields() {
        usernameField.putClientProperty("JTextField.placeholderText", "your username");
        passwordField.putClientProperty("JTextField.placeholderText", "your password");
        for (javax.swing.text.JTextComponent field : new javax.swing.text.JTextComponent[]{usernameField, passwordField}) {
            field.setBackground(FIELD);
            field.setForeground(TEXT);
            field.setFont(ui(Font.PLAIN, 13.5f));
            field.setCaretColor(MOSS);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EDGE, 1, true),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            field.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
            field.setMinimumSize(new Dimension(0, FIELD_HEIGHT));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        }
        passwordField.putClientProperty("JTextField.trailingComponent", showToggle());
    }

    private JButton showToggle() {
        JButton show = new JButton("Show");
        show.setFont(uiLabel(11.5f));
        show.setForeground(MOSS);
        show.setContentAreaFilled(false);
        show.setBorderPainted(false);
        show.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        show.setMargin(new Insets(0, 6, 0, 12));
        show.putClientProperty("JButton.buttonType", "borderless");
        show.setToolTipText("Show or hide the password");
        show.addActionListener(e -> {
            boolean hidden = passwordField.getEchoChar() != (char) 0;
            passwordField.setEchoChar(hidden ? (char) 0 : '•');
            show.setText(hidden ? "Hide" : "Show");
        });
        return show;
    }

    private void wireFields() {
        usernameField.addActionListener(e -> authenticate());
        passwordField.addActionListener(e -> authenticate());

        usernameField.getDocument().addDocumentListener(new SimpleDocumentListener(this::clearError));
        passwordField.getDocument().addDocumentListener(new SimpleDocumentListener(this::clearError));
    }

    private void rebuild() {
        JPanel background = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics raw) {
                Graphics2D g = (Graphics2D) raw.create();
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x10, 0x2A, 0x1E),
                        0, getHeight(), new Color(0x0A, 0x17, 0x10)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(24, 24, 24, 24);
        background.add(card(), c);

        setContentPane(background);
        revalidate();
        repaint();
    }

    private JPanel card() {
        JPanel card = new CardPanel(new BorderLayout());
        card.setBackground(SURFACE);
        card.setPreferredSize(new Dimension(520, 620));
        card.setBorder(BorderFactory.createEmptyBorder(36, 52, 36, 52));
        card.add(cardBody(), BorderLayout.CENTER);
        return card;
    }

    private JPanel cardBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        body.add(centredRow(crest()));
        body.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("EVERGREEN LIBRARY", SwingConstants.CENTER);
        title.setFont(display(Font.PLAIN, 32));
        title.setForeground(new Color(234, 243, 236));
        body.add(centredRow(title));
        body.add(Box.createVerticalStrut(8));

        JLabel tagline = new JLabel("BOOK BORROWING SYSTEM", SwingConstants.CENTER);
        tagline.setFont(FontManager.tracked(uiLabel(10.5f), 0.16f));
        tagline.setForeground(GOLD);
        body.add(centredRow(tagline));
        body.add(Box.createVerticalStrut(24));

        body.add(leftRow(divider()));
        body.add(Box.createVerticalStrut(24));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(display(Font.PLAIN, 27));
        welcome.setForeground(TEXT);
        body.add(leftRow(welcome));
        body.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Sign in to manage your library experience.");
        subtitle.setFont(ui(Font.PLAIN, 13.5f));
        subtitle.setForeground(SUBDUED);
        body.add(leftRow(subtitle));
        body.add(Box.createVerticalStrut(24));

        body.add(leftRow(fieldGroup("USERNAME", usernameField)));
        body.add(Box.createVerticalStrut(18));
        body.add(leftRow(fieldGroup("PASSWORD", passwordField)));
        body.add(Box.createVerticalStrut(16));

        errorLabel.setFont(uiLabel(12.5f));
        errorLabel.setForeground(DANGER);
        body.add(leftRow(errorLabel));
        body.add(Box.createVerticalStrut(16));

        JButton login = new JButton("Sign in");
        login.setFont(uiLabel(14f));
        login.setForeground(Color.WHITE);
        login.setBackground(PRIMARY);
        login.setPreferredSize(new Dimension(0, CONTROL_HEIGHT));
        login.setMinimumSize(new Dimension(0, CONTROL_HEIGHT));
        login.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        login.putClientProperty("JButton.buttonType", "roundRect");
        login.addActionListener(e -> authenticate());
        body.add(leftRow(login, 48));
        body.add(Box.createVerticalStrut(16));

        JLabel hint = new JLabel("Ask the librarian if you need an account.");
        hint.setFont(ui(Font.PLAIN, 12.5f));
        hint.setForeground(SUBDUED);
        body.add(centredRow(hint));

        return body;
    }

    private JComponent crest() {
        JLabel mark = new JLabel(Logo.icon(76));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        return mark;
    }

    private JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(new Color(255, 255, 255, 22));
        line.setPreferredSize(new Dimension(0, 1));
        line.setMinimumSize(new Dimension(0, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private JPanel leftRow(java.awt.Component content) {
        return leftRow(content, 0);
    }

    private JPanel leftRow(java.awt.Component content, int height) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (height > 0) {
            content.setPreferredSize(new Dimension(0, height));
            row.setPreferredSize(new Dimension(0, height));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        } else {
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, content.getPreferredSize().height));
        }
        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JPanel centredRow(java.awt.Component content) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, content.getPreferredSize().height));
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        inner.setOpaque(false);
        inner.add(content);
        row.add(inner, BorderLayout.CENTER);
        return row;
    }

    private JPanel fieldGroup(String caption, java.awt.Component field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(caption);
        label.setFont(FontManager.tracked(uiLabel(11.5f), 0.06f));
        label.setForeground(SUBDUED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(label);
        group.add(Box.createVerticalStrut(8));

        ((javax.swing.JComponent) field).setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(field);
        return group;
    }

    private void authenticate() {
        String name = usernameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter your username.", usernameField);
            return;
        }
        if (passwordField.getPassword().length == 0) {
            showError("Please enter your password.", passwordField);
            return;
        }
        AuthService.LoginResult result = AuthService.authenticate(name, passwordField.getPassword());
        switch (result.status()) {
            case SUCCESS -> {
                dispose();
                new DashboardFrame(result.user()).setVisible(true);
            }
            case INVALID_CREDENTIALS -> {
                passwordField.setText("");
                showError("Incorrect username or password.", passwordField);
            }
            case ACCOUNT_DISABLED -> {
                passwordField.setText("");
                showError("This account is disabled. Please ask the librarian.", passwordField);
            }
            case DATABASE_ERROR -> JOptionPane.showMessageDialog(this,
                    "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showError(String message, java.awt.Component focus) {
        errorLabel.setText(message);
        focus.requestFocusInWindow();
    }

    private void clearError() {
        if (!" ".equals(errorLabel.getText())) errorLabel.setText(" ");
    }

}
