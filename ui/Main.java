package bugradar;

import bugradar.db.DatabaseManager;
import bugradar.ui.MainWindow;
import javax.swing.*;

/**
 * BugRadar — Entry Point
 * Team: The Eternals Avengers
 * Storage: SQLite ONLY (no file fallback)
 */
public class Main {
    public static void main(String[] args) {
        // Connect to SQLite — mandatory
        try {
            DatabaseManager.getInstance().connect();
        } catch (Exception e) {
            // Show error dialog and exit if DB fails
            JOptionPane.showMessageDialog(null,
                "Cannot connect to SQLite database!\n\n" +
                "Please make sure sqlite-jdbc-*.jar is inside the lib\\ folder.\n\n" +
                "Download from:\nhttps://github.com/xerial/sqlite-jdbc/releases\n\n" +
                "Error: " + e.getMessage(),
                "Database Connection Failed",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Launch UI
        SwingUtilities.invokeLater(MainWindow::new);

        // Close DB on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            DatabaseManager.getInstance().disconnect()));
    }
}