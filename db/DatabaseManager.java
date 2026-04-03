package bugradar.db;

import java.io.File;
import java.sql.*;

/**
 * Manages the SQLite JDBC connection and schema creation.
 * Database file: ~/.bugradar/bugradar.db
 * NO file fallback — SQLite is required.
 */
public class DatabaseManager {

    private static final String DB_DIR =
        System.getProperty("user.home") + File.separator + ".bugradar";
    private static final String DB_PATH =
        DB_DIR + File.separator + "bugradar.db";
    private static final String DB_URL =
        "jdbc:sqlite:" + DB_PATH;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void connect() throws SQLException {
        // Create directory
        new File(DB_DIR).mkdirs();

        // Load driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "SQLite JDBC driver not found.\n" +
                "Place sqlite-jdbc-*.jar inside the 'lib' folder.", e);
        }

        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);

        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA foreign_keys=ON;");
        }

        createSchema();
        System.out.println("[DB] Connected: " + DB_PATH);
    }

    private void createSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS bugs (
                    id              TEXT PRIMARY KEY,
                    title           TEXT NOT NULL,
                    description     TEXT,
                    type            TEXT NOT NULL,
                    priority        TEXT NOT NULL,
                    status          TEXT NOT NULL DEFAULT 'OPEN',
                    reported_by     TEXT,
                    assigned_to     TEXT,
                    line_number     INTEGER DEFAULT 0,
                    code_snippet    TEXT,
                    suggested_fix   TEXT,
                    detection_score INTEGER DEFAULT 0,
                    created_at      TEXT NOT NULL,
                    updated_at      TEXT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS analysis_sessions (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_code  TEXT,
                    language      TEXT DEFAULT 'Java',
                    total_issues  INTEGER DEFAULT 0,
                    error_count   INTEGER DEFAULT 0,
                    warning_count INTEGER DEFAULT 0,
                    quality_score INTEGER DEFAULT 0,
                    analyzed_at   TEXT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS analysis_issues (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id     INTEGER NOT NULL,
                    line_number    INTEGER,
                    issue_type     TEXT,
                    priority       TEXT,
                    message        TEXT,
                    fix_suggestion TEXT,
                    FOREIGN KEY (session_id)
                        REFERENCES analysis_sessions(id) ON DELETE CASCADE
                )
            """);

            System.out.println("[DB] Schema ready.");
        }
    }

    public Connection getConnection() { return connection; }

    public boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { return false; }
    }

    public String getDbPath() { return DB_PATH; }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Disconnected.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}