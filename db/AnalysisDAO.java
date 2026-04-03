package bugradar.db;

import bugradar.logic.BugManager.AnalysisResult;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AnalysisDAO — saves analysis sessions and individual issues to SQLite.
 * Fixed: safe string binding, proper batch insert, total count query.
 */
public class AnalysisDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── Save full session + issues ───────────────────────────────
    public long saveSession(String code, int total, int errors,
                            int warnings, int score,
                            List<AnalysisResult> issues) {
        if (conn() == null) {
            System.err.println("[SQL] saveSession: no DB connection");
            return -1;
        }

        String sessionSql = """
            INSERT INTO analysis_sessions
              (session_code, language, total_issues, error_count,
               warning_count, quality_score, analyzed_at)
            VALUES (?, 'Java', ?, ?, ?, ?, ?)
        """;

        try {
            long sessionId;

            // Insert session row
            try (PreparedStatement ps = conn().prepareStatement(
                    sessionSql, Statement.RETURN_GENERATED_KEYS)) {
                String stored = (code != null && code.length() > 2000)
                    ? code.substring(0, 2000) + "..." : (code != null ? code : "");
                ps.setString(1, stored);
                ps.setInt(2,    total);
                ps.setInt(3,    errors);
                ps.setInt(4,    warnings);
                ps.setInt(5,    score);
                ps.setString(6, LocalDateTime.now().toString());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        sessionId = keys.getLong(1);
                    } else {
                        System.err.println("[SQL] saveSession: no generated key returned");
                        return -1;
                    }
                }
            }

            // Insert each issue
            if (issues != null && !issues.isEmpty()) {
                String issueSql = """
                    INSERT INTO analysis_issues
                      (session_id, line_number, issue_type,
                       priority, message, fix_suggestion)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
                try (PreparedStatement ps = conn().prepareStatement(issueSql)) {
                    for (AnalysisResult r : issues) {
                        ps.setLong(1,   sessionId);
                        ps.setInt(2,    r.lineNo);
                        ps.setString(3, r.type != null ? r.type.name() : "OTHER");
                        ps.setString(4, r.priority != null ? r.priority.name() : "MEDIUM");
                        ps.setString(5, r.message != null ? r.message : "");
                        ps.setString(6, r.fix != null ? r.fix : "");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            System.out.println("[SQL] INSERT analysis_sessions id=" + sessionId
                + " total=" + total + " score=" + score + "%");
            return sessionId;

        } catch (SQLException e) {
            System.err.println("[SQL] saveSession error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // ── Get recent sessions ──────────────────────────────────────
    public List<SessionSummary> getRecentSessions(int limit) {
        List<SessionSummary> list = new ArrayList<>();
        if (conn() == null) return list;

        String sql = """
            SELECT id, total_issues, error_count, warning_count,
                   quality_score, analyzed_at
            FROM analysis_sessions
            ORDER BY id DESC
            LIMIT ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SessionSummary(
                        rs.getLong("id"),
                        rs.getInt("total_issues"),
                        rs.getInt("error_count"),
                        rs.getInt("warning_count"),
                        rs.getInt("quality_score"),
                        rs.getString("analyzed_at")
                    ));
                }
            }
            System.out.println("[SQL] SELECT analysis_sessions -> " + list.size() + " rows");
        } catch (SQLException e) {
            System.err.println("[SQL] getRecentSessions error: " + e.getMessage());
        }
        return list;
    }

    // ── Count total sessions ─────────────────────────────────────
    public int getTotalSessionsCount() {
        if (conn() == null) return 0;
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) AS cnt FROM analysis_sessions")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("[SQL] getTotalSessionsCount error: " + e.getMessage());
        }
        return 0;
    }

    // ── Count issues for DB panel display ────────────────────────
    public int getTotalIssuesCount() {
        if (conn() == null) return 0;
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) AS cnt FROM analysis_issues")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("[SQL] getTotalIssuesCount error: " + e.getMessage());
        }
        return 0;
    }

    // ── Inner record ─────────────────────────────────────────────
    public static class SessionSummary {
        public final long   id;
        public final int    totalIssues, errors, warnings, score;
        public final String analyzedAt;

        public SessionSummary(long id, int t, int e, int w, int s, String at) {
            this.id = id; totalIssues = t; errors = e;
            warnings = w; score = s; analyzedAt = at;
        }
    }
}