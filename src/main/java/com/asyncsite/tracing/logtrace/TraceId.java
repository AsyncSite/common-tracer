package com.asyncsite.tracing.logtrace;

import lombok.Getter;

/**
 * Trace ID value object
 *
 * Responsibilities:
 * - Holds trace ID string (from Micrometer or generated)
 * - Manages call depth level for log indentation
 * - Creates next/previous level TraceIds
 */
@Getter
public class TraceId {

    private final String id;
    private final int level;

    public TraceId(String id) {
        this.id = id;
        this.level = 0;
    }

    private TraceId(String id, int level) {
        this.id = id;
        this.level = level;
    }

    /**
     * Create next level TraceId (deeper call)
     */
    public TraceId createNextId() {
        return new TraceId(id, level + 1);
    }

    /**
     * Create previous level TraceId (return from call)
     */
    public TraceId createPreviousId() {
        return new TraceId(id, level - 1);
    }

    /**
     * Check if this is the first level (root call)
     */
    public boolean isFirstLevel() {
        return level == 0;
    }
}
