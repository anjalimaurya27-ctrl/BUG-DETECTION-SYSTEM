package bugradar.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bug model — data stored ONLY in SQLite.
 * ID is generated as UUID string — no static counter
 * so restarting the app never resets or duplicates IDs.
 */
public class Bug implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public enum Type {
        RUNTIME("Runtime Error"), LOGIC("Logic Error"),
        SYNTAX("Syntax Error"), NULL_POINTER("Null Pointer"),
        ARRAY_INDEX("Array Index OOB"), CLASS_CAST("Class Cast"),
        IO("I/O Error"), OTHER("Other");
        private final String label;
        Type(String l) { label = l; }
        @Override public String toString() { return label; }
    }

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status   { OPEN, IN_PROGRESS, RESOLVED, CLOSED }

    private String        id;
    private String        title;
    private String        description;
    private Type          type;
    private Priority      priority;
    private Status        status;
    private String        reportedBy;
    private String        assignedTo;
    private int           lineNumber;
    private String        codeSnippet;
    private String        suggestedFix;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int           detectionScore;

    public Bug(String title, String description, Type type, Priority priority) {
        // UUID-based ID — unique across all runs, never conflicts
        this.id             = "BUG-" + java.util.UUID.randomUUID()
                                .toString().substring(0, 8).toUpperCase();
        this.title          = title;
        this.description    = description != null ? description : "";
        this.type           = type;
        this.priority       = priority;
        this.status         = Status.OPEN;
        this.createdAt      = LocalDateTime.now();
        this.updatedAt      = LocalDateTime.now();
        this.reportedBy     = "Developer";
        this.assignedTo     = "Unassigned";
        this.lineNumber     = 0;
        this.codeSnippet    = "";
        this.suggestedFix   = "";
        this.detectionScore = 0;
    }

    // Getters
    public String        getId()             { return id; }
    public String        getTitle()          { return title; }
    public String        getDescription()    { return description; }
    public Type          getType()           { return type; }
    public Priority      getPriority()       { return priority; }
    public Status        getStatus()         { return status; }
    public String        getReportedBy()     { return reportedBy; }
    public String        getAssignedTo()     { return assignedTo; }
    public int           getLineNumber()     { return lineNumber; }
    public String        getCodeSnippet()    { return codeSnippet; }
    public String        getSuggestedFix()   { return suggestedFix; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public LocalDateTime getUpdatedAt()      { return updatedAt; }
    public int           getDetectionScore() { return detectionScore; }

    // Setters
    public void setId(String v)               { id = v; }
    public void setTitle(String v)            { title = v;         touch(); }
    public void setDescription(String v)      { description = v != null ? v : ""; touch(); }
    public void setType(Type v)               { type = v;          touch(); }
    public void setPriority(Priority v)       { priority = v;      touch(); }
    public void setStatus(Status v)           { status = v;        touch(); }
    public void setReportedBy(String v)       { reportedBy = v != null ? v : ""; touch(); }
    public void setAssignedTo(String v)       { assignedTo = v != null ? v : ""; touch(); }
    public void setLineNumber(int v)          { lineNumber = v;    touch(); }
    public void setCodeSnippet(String v)      { codeSnippet = v != null ? v : ""; touch(); }
    public void setSuggestedFix(String v)     { suggestedFix = v != null ? v : ""; touch(); }
    public void setDetectionScore(int v)      { detectionScore = v; touch(); }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }

    private void touch() { updatedAt = LocalDateTime.now(); }

    public String getCreatedFormatted() {
        return createdAt != null ? createdAt.format(FMT) : "";
    }
    public String getUpdatedFormatted() {
        return updatedAt != null ? updatedAt.format(FMT) : "";
    }

    @Override public String toString() { return "[" + id + "] " + title; }
}