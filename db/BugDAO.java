package bugradar.db;

import bugradar.model.Bug;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * BugDAO — all bug CRUD operations against SQLite.
 * Fixed: null-safe string binding, safe timestamp parsing,
 *        SQL logged for every operation.
 */
public class BugDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── INSERT ───────────────────────────────────────────────────
    public boolean insert(Bug b) {
        String sql = """
            INSERT OR REPLACE INTO bugs
            (id, title, description, type, priority, status,
             reported_by, assigned_to, line_number, code_snippet,
             suggested_fix, detection_score, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,  safe(b.getId()));
            ps.setString(2,  safe(b.getTitle()));
            ps.setString(3,  safe(b.getDescription()));
            ps.setString(4,  b.getType().name());
            ps.setString(5,  b.getPriority().name());
            ps.setString(6,  b.getStatus().name());
            ps.setString(7,  safe(b.getReportedBy()));
            ps.setString(8,  safe(b.getAssignedTo()));
            ps.setInt(9,     b.getLineNumber());
            ps.setString(10, safe(b.getCodeSnippet()));
            ps.setString(11, safe(b.getSuggestedFix()));
            ps.setInt(12,    b.getDetectionScore());
            ps.setString(13, ts(b.getCreatedAt()));
            ps.setString(14, ts(b.getUpdatedAt()));
            int rows = ps.executeUpdate();
            System.out.println("[SQL] INSERT bugs id=" + b.getId() + " rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[SQL] INSERT error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────
    public boolean update(Bug b) {
        String sql = """
            UPDATE bugs SET
              title=?, description=?, type=?, priority=?, status=?,
              reported_by=?, assigned_to=?, line_number=?,
              code_snippet=?, suggested_fix=?, detection_score=?,
              updated_at=?
            WHERE id=?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,  safe(b.getTitle()));
            ps.setString(2,  safe(b.getDescription()));
            ps.setString(3,  b.getType().name());
            ps.setString(4,  b.getPriority().name());
            ps.setString(5,  b.getStatus().name());
            ps.setString(6,  safe(b.getReportedBy()));
            ps.setString(7,  safe(b.getAssignedTo()));
            ps.setInt(8,     b.getLineNumber());
            ps.setString(9,  safe(b.getCodeSnippet()));
            ps.setString(10, safe(b.getSuggestedFix()));
            ps.setInt(11,    b.getDetectionScore());
            ps.setString(12, ts(LocalDateTime.now()));
            ps.setString(13, safe(b.getId()));
            int rows = ps.executeUpdate();
            System.out.println("[SQL] UPDATE bugs id=" + b.getId() + " rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[SQL] UPDATE error: " + e.getMessage());
            return false;
        }
    }

    // ── DELETE ───────────────────────────────────────────────────
    public boolean delete(String id) {
        try (PreparedStatement ps = conn()
                .prepareStatement("DELETE FROM bugs WHERE id=?")) {
            ps.setString(1, safe(id));
            int rows = ps.executeUpdate();
            System.out.println("[SQL] DELETE bugs id=" + id + " rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[SQL] DELETE error: " + e.getMessage());
            return false;
        }
    }

    // ── SELECT ALL ───────────────────────────────────────────────
    public List<Bug> findAll() {
        List<Bug> list = new ArrayList<>();
        String sql = "SELECT * FROM bugs ORDER BY created_at DESC";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Bug b = mapRow(rs);
                if (b != null) list.add(b);
            }
            System.out.println("[SQL] SELECT * FROM bugs -> " + list.size() + " rows");
        } catch (SQLException e) {
            System.err.println("[SQL] SELECT ALL error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ── ROW MAPPER ───────────────────────────────────────────────
    private Bug mapRow(ResultSet rs) {
        try {
            String typeStr     = rs.getString("type");
            String priorityStr = rs.getString("priority");
            String statusStr   = rs.getString("status");

            Bug.Type     type     = parseEnum(Bug.Type.class,     typeStr,     Bug.Type.OTHER);
            Bug.Priority priority = parseEnum(Bug.Priority.class, priorityStr, Bug.Priority.MEDIUM);
            Bug.Status   status   = parseEnum(Bug.Status.class,   statusStr,   Bug.Status.OPEN);

            Bug b = new Bug(
                safe(rs.getString("title")),
                safe(rs.getString("description")),
                type, priority
            );
            b.setId(safe(rs.getString("id")));
            b.setStatus(status);
            b.setReportedBy(safe(rs.getString("reported_by")));
            b.setAssignedTo(safe(rs.getString("assigned_to")));
            b.setLineNumber(rs.getInt("line_number"));
            b.setCodeSnippet(safe(rs.getString("code_snippet")));
            b.setSuggestedFix(safe(rs.getString("suggested_fix")));
            b.setDetectionScore(rs.getInt("detection_score"));

            String ca = rs.getString("created_at");
            String ua = rs.getString("updated_at");
            if (ca != null && !ca.isBlank()) b.setCreatedAt(parseDT(ca));
            if (ua != null && !ua.isBlank()) b.setUpdatedAt(parseDT(ua));

            return b;
        } catch (Exception e) {
            System.err.println("[SQL] mapRow error: " + e.getMessage());
            return null;
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────
    private String safe(String s) {
        return s != null ? s : "";
    }

    private String ts(LocalDateTime t) {
        return t != null ? t.toString() : LocalDateTime.now().toString();
    }

    private LocalDateTime parseDT(String s) {
        try { return LocalDateTime.parse(s); }
        catch (Exception e) {
            try { return LocalDateTime.parse(s.replace(" ", "T")); }
            catch (Exception e2) { return LocalDateTime.now(); }
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> cls, String val, T fallback) {
        if (val == null || val.isBlank()) return fallback;
        try { return Enum.valueOf(cls, val.trim().toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
}