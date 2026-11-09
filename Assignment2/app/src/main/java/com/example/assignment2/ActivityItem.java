package com.example.assignment2;

/** One saved activity round read back from SQLite (used by the history list). */
public class ActivityItem {
    public final long id;
    public final long startedAtMillis;
    public final long durationMillis;
    public final int steps;
    public final int shakes;
    public final double stepsPerMinute;
    public final double shakesPerMinute;

    public ActivityItem(long id, long startedAtMillis, long durationMillis,
                        int steps, int shakes,
                        double stepsPerMinute, double shakesPerMinute) {
        this.id = id;
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
        this.steps = steps;
        this.shakes = shakes;
        this.stepsPerMinute = stepsPerMinute;
        this.shakesPerMinute = shakesPerMinute;
    }
}