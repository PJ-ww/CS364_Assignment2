package com.example.assignment2;

/** Integration contract for person 4. insert must commit atomically or throw. */
public interface SessionStore {
    void insert(Session session) throws Exception;
    /** Compute totals and maxima from existing rows, including after deletion. */
    Summary readSummary() throws Exception;

    final class Session {
        public final long startedAtMillis, durationMillis;
        public final int steps, shakes;
        public final double stepsPerMinute, shakesPerMinute;
        public Session(long startedAtMillis, int steps, int shakes, long durationMillis) {
            this.startedAtMillis = startedAtMillis;
            this.steps = steps;
            this.shakes = shakes;
            this.durationMillis = durationMillis;
            stepsPerMinute = steps * 60000.0 / Math.max(1, durationMillis);
            shakesPerMinute = shakes * 60000.0 / Math.max(1, durationMillis);
        }
    }

    final class Summary {
        public final long totalSteps, totalShakes;
        public final int maxSteps, maxShakes, count;
        public Summary(long totalSteps, long totalShakes, int maxSteps, int maxShakes, int count) {
            this.totalSteps = totalSteps;
            this.totalShakes = totalShakes;
            this.maxSteps = maxSteps;
            this.maxShakes = maxShakes;
            this.count = count;
        }
    }
}
