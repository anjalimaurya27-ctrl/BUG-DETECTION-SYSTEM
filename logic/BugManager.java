package bugradar.logic;

import bugradar.db.AnalysisDAO;
import bugradar.db.BugDAO;
import bugradar.model.Bug;
import java.util.*;
import java.util.stream.*;

/**
 * BugManager — all storage is SQLite only.
 * Fixed: in-memory list stays in sync with DB on every operation,
 *        refresh() reloads from DB to catch any discrepancy.
 */
public class BugManager {

    private final List<Bug>   bugs        = new ArrayList<>();
    private final BugDAO      bugDAO      = new BugDAO();
    private final AnalysisDAO analysisDAO = new AnalysisDAO();

    public BugManager() {
        load();
    }

    // ── CRUD ────────────────────────────────────────────────────

    public void addBug(Bug bug) {
        boolean saved = bugDAO.insert(bug);
        if (saved) {
            bugs.add(bug);
            System.out.println("[BugManager] Bug added: " + bug.getId()
                + " | total in memory: " + bugs.size());
        } else {
            System.err.println("[BugManager] Failed to insert bug: " + bug.getId());
        }
    }

    public void updateBug(Bug bug) {
        boolean updated = bugDAO.update(bug);
        if (!updated) {
            System.err.println("[BugManager] Failed to update bug: " + bug.getId());
        }
        // Refresh in-memory object
        int idx = -1;
        for (int i = 0; i < bugs.size(); i++) {
            if (bugs.get(i).getId().equals(bug.getId())) { idx = i; break; }
        }
        if (idx >= 0) bugs.set(idx, bug);
    }

    public void deleteBug(String id) {
        boolean deleted = bugDAO.delete(id);
        if (deleted) {
            bugs.removeIf(b -> b.getId().equals(id));
            System.out.println("[BugManager] Bug deleted: " + id
                + " | remaining: " + bugs.size());
        } else {
            System.err.println("[BugManager] Failed to delete bug: " + id);
        }
    }

    /** Hard reload from DB — fixes any in-memory / DB drift */
    public void refreshFromDB() {
        List<Bug> fresh = bugDAO.findAll();
        bugs.clear();
        bugs.addAll(fresh);
        System.out.println("[BugManager] Refreshed from DB: " + bugs.size() + " bugs");
    }

    public Bug findById(String id) {
        return bugs.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    public List<Bug> getAllBugs() {
        return Collections.unmodifiableList(bugs);
    }

    // ── SEARCH ──────────────────────────────────────────────────

    public List<Bug> search(String query) {
        if (query == null || query.isBlank()) return getAllBugs();
        String q = query.toLowerCase().trim();
        return bugs.stream().filter(b ->
            b.getTitle().toLowerCase().contains(q) ||
            b.getDescription().toLowerCase().contains(q) ||
            b.getId().toLowerCase().contains(q) ||
            b.getType().toString().toLowerCase().contains(q)
        ).collect(Collectors.toList());
    }

    // ── STATISTICS ──────────────────────────────────────────────

    public int getTotalCount()       { return bugs.size(); }
    public int getOpenCount()        { return countByStatus(Bug.Status.OPEN); }
    public int getInProgressCount()  { return countByStatus(Bug.Status.IN_PROGRESS); }
    public int getResolvedCount()    { return countByStatus(Bug.Status.RESOLVED); }
    public int getClosedCount()      { return countByStatus(Bug.Status.CLOSED); }
    public int getCriticalCount()    {
        return (int) bugs.stream()
            .filter(b -> b.getPriority() == Bug.Priority.CRITICAL).count();
    }

    private int countByStatus(Bug.Status s) {
        return (int) bugs.stream().filter(b -> b.getStatus() == s).count();
    }

    public int getEfficiencyScore() {
        if (bugs.isEmpty()) return 0;
        long done = bugs.stream().filter(b ->
            b.getStatus() == Bug.Status.RESOLVED ||
            b.getStatus() == Bug.Status.CLOSED
        ).count();
        return (int)((done * 100L) / bugs.size());
    }

    public Map<Bug.Type, Long> getCountByType() {
        Map<Bug.Type, Long> map = new LinkedHashMap<>();
        for (Bug.Type t : Bug.Type.values()) {
            long c = bugs.stream().filter(b -> b.getType() == t).count();
            if (c > 0) map.put(t, c);
        }
        return map;
    }

    // ── CODE ANALYSIS ───────────────────────────────────────────

    public List<AnalysisResult> analyzeCode(String code) {
        List<AnalysisResult> results = new ArrayList<>();
        if (code == null || code.isBlank()) return results;

        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int no = i + 1;
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) continue;

            if ((line.contains(".length()") || line.contains(".size()") ||
                 line.contains(".get(") || line.contains(".toString()")) &&
                !line.contains("if") && !line.contains("null") && !line.contains("Optional"))
                results.add(new AnalysisResult(no, Bug.Type.NULL_POINTER, Bug.Priority.HIGH,
                    "Potential NullPointerException — object not null-checked before use.",
                    "if (obj != null) { obj.method(); }"));

            if (line.matches(".*\\[[a-zA-Z_][a-zA-Z0-9_]*\\].*") &&
                !line.contains("new ") && !line.contains("String") && !line.contains("//"))
                results.add(new AnalysisResult(no, Bug.Type.ARRAY_INDEX, Bug.Priority.HIGH,
                    "Possible ArrayIndexOutOfBoundsException — no bounds check.",
                    "Check: if (i >= 0 && i < arr.length)"));

            if (line.matches(".*i\\s*<=\\s*[a-zA-Z_][a-zA-Z0-9_]*\\.length.*"))
                results.add(new AnalysisResult(no, Bug.Type.LOGIC, Bug.Priority.HIGH,
                    "Off-by-one error: '<=' with .length will throw AIOOB.",
                    "Change '<=' to '<' in loop condition."));

            if (line.matches(".*[/%]\\s*[a-zA-Z_][a-zA-Z0-9_]*.*") && !line.contains("//"))
                results.add(new AnalysisResult(no, Bug.Type.RUNTIME, Bug.Priority.HIGH,
                    "Potential ArithmeticException — division by zero risk.",
                    "Check: if (b != 0) { result = a / b; }"));

            if (!line.endsWith(";") && !line.endsWith("{") && !line.endsWith("}")
                && !line.endsWith(",") && !line.endsWith("(") && !line.endsWith(")")
                && !line.startsWith("@") && !line.startsWith("import") && !line.startsWith("package")
                && !line.startsWith("class ") && !line.startsWith("public ") && !line.startsWith("private ")
                && !line.startsWith("protected ") && !line.startsWith("if") && !line.startsWith("else")
                && !line.startsWith("for") && !line.startsWith("while") && !line.startsWith("try")
                && !line.startsWith("catch") && !line.startsWith("finally")
                && !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*")
                && !line.startsWith("*/") && line.length() > 4 && line.contains(" ")
                && !line.endsWith("\\"))
                results.add(new AnalysisResult(no, Bug.Type.SYNTAX, Bug.Priority.CRITICAL,
                    "Missing semicolon at end of statement.",
                    "Add ';' to end of line " + no + "."));

            if ((line.contains("== \"") || line.contains("!= \"")) && !line.startsWith("//"))
                results.add(new AnalysisResult(no, Bug.Type.LOGIC, Bug.Priority.HIGH,
                    "String compared with '==' — use .equals() for value comparison.",
                    "Replace: str.equals(\"value\")"));

            if (line.matches(".*catch\\s*\\(.*\\)\\s*\\{?\\s*\\}?.*") && !line.contains("//"))
                results.add(new AnalysisResult(no, Bug.Type.RUNTIME, Bug.Priority.MEDIUM,
                    "Empty or broad catch block swallows exceptions silently.",
                    "Catch specific types and log/handle properly."));

            if ((line.contains("new FileReader") || line.contains("new FileWriter") ||
                 line.contains("new BufferedReader") || line.contains("new Scanner(new File")) &&
                !line.contains("try"))
                results.add(new AnalysisResult(no, Bug.Type.IO, Bug.Priority.HIGH,
                    "Resource opened without try-with-resources — resource leak risk.",
                    "Use: try (FileReader fr = new FileReader(file)) { ... }"));

            if ((line.contains("int ") || line.contains("int\t")) &&
                (line.contains("* 1000") || line.contains("* 60") || line.contains("Math.pow")))
                results.add(new AnalysisResult(no, Bug.Type.RUNTIME, Bug.Priority.MEDIUM,
                    "Possible integer overflow in arithmetic.",
                    "Use 'long' or BigInteger for large values."));

            if (line.contains("System.exit(") && !line.startsWith("//"))
                results.add(new AnalysisResult(no, Bug.Type.LOGIC, Bug.Priority.MEDIUM,
                    "System.exit() terminates the JVM unexpectedly.",
                    "Throw an exception or use controlled shutdown instead."));

            if ((line.contains("ArrayList ") || line.contains("HashMap ") || line.contains("List "))
                && !line.contains("<") && !line.contains("//"))
                results.add(new AnalysisResult(no, Bug.Type.LOGIC, Bug.Priority.LOW,
                    "Raw type — missing generic type parameter.",
                    "Use ArrayList<String>, HashMap<String,Integer>, etc."));
        }

        // Deduplicate same line + type
        List<AnalysisResult> deduped = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (AnalysisResult r : results)
            if (seen.add(r.lineNo * 100 + r.type.ordinal())) deduped.add(r);
        return deduped;
    }

    public int computeCodeScore(List<AnalysisResult> results) {
        if (results.isEmpty()) return 100;
        int penalty = 0;
        for (AnalysisResult r : results)
            penalty += switch (r.priority) {
                case CRITICAL -> 25; case HIGH -> 12;
                case MEDIUM -> 6;    case LOW  -> 2;
            };
        return Math.max(0, 100 - penalty);
    }

    public long saveAnalysisSession(String code, List<AnalysisResult> issues, int score) {
        long errors   = issues.stream().filter(r ->
            r.priority == Bug.Priority.CRITICAL || r.priority == Bug.Priority.HIGH).count();
        long warnings = issues.size() - errors;
        return analysisDAO.saveSession(code, issues.size(),
            (int)errors, (int)warnings, score, issues);
    }

    public List<AnalysisDAO.SessionSummary> getRecentAnalysisSessions(int limit) {
        return analysisDAO.getRecentSessions(limit);
    }

    public int getTotalAnalysisSessions() {
        return analysisDAO.getTotalSessionsCount();
    }

    public int getTotalAnalysisIssues() {
        return analysisDAO.getTotalIssuesCount();
    }

    // ── LOAD FROM DB ────────────────────────────────────────────

    private void load() {
        List<Bug> loaded = bugDAO.findAll();
        bugs.clear();
        bugs.addAll(loaded);
        System.out.println("[BugManager] Loaded " + bugs.size() + " bugs from SQLite DB.");
    }

    // ── Inner class ─────────────────────────────────────────────

    public static class AnalysisResult {
        public final int          lineNo;
        public final Bug.Type     type;
        public final Bug.Priority priority;
        public final String       message;
        public final String       fix;

        public AnalysisResult(int l, Bug.Type t, Bug.Priority p, String m, String f) {
            lineNo = l; type = t; priority = p; message = m; fix = f;
        }
    } 
}