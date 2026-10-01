package library;

import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import library.database.DatabaseInitializer;
import library.ui.LoginFrame;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unable to connect to the database.", "Evergreen Library", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
