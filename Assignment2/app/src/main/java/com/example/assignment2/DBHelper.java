package com.example.assignment2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Person 4: SQLite storage for StepByStep.
 * Implements the SessionStore contract from person 3 and also exposes
 * getAll / getById / delete for person 5 (RecyclerView).
 */
public class DBHelper extends SQLiteOpenHelper implements SessionStore {

    private static final String DB_NAME = "stepbystep.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "activity_log";
    private static final String COL_ID = "_id";
    private static final String COL_STARTED = "started_at";
    private static final String COL_DURATION = "duration_ms";
    private static final String COL_STEPS = "steps";
    private static final String COL_SHAKES = "shakes";
    private static final String COL_STEPS_PM = "steps_per_min";
    private static final String COL_SHAKES_PM = "shakes_per_min";

    public DBHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_STARTED + " INTEGER NOT NULL, "
                + COL_DURATION + " INTEGER NOT NULL, "
                + COL_STEPS + " INTEGER NOT NULL DEFAULT 0, "
                + COL_SHAKES + " INTEGER NOT NULL DEFAULT 0, "
                + COL_STEPS_PM + " REAL NOT NULL DEFAULT 0, "
                + COL_SHAKES_PM + " REAL NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ------------------------------------------------------------
    // Checklist item 6: Insert Session
    // Saves one round inside a transaction. On failure it throws
    // (SQLiteException), which MainActivity catches to keep the round.
    // ------------------------------------------------------------
    @Override
    public void insert(Session session) {
        ContentValues values = new ContentValues();
        values.put(COL_STARTED, session.startedAtMillis);
        values.put(COL_DURATION, session.durationMillis);
        values.put(COL_STEPS, session.steps);
        values.put(COL_SHAKES, session.shakes);
        values.put(COL_STEPS_PM, session.stepsPerMinute);
        values.put(COL_SHAKES_PM, session.shakesPerMinute);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long rowId = db.insert(TABLE, null, values);
            if (rowId == -1) {
                throw new SQLiteException("Insert failed");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------
    // Checklist item 7: Query & Persistence (newest first)
    // Used by person 5's RecyclerView.
    // ------------------------------------------------------------
    @SuppressWarnings("unused") // remove once the RecyclerView calls it
    public List<ActivityItem> getAll() {
        List<ActivityItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, null, null,
                null, null, COL_STARTED + " DESC, " + COL_ID + " DESC")) {
            while (c.moveToNext()) {
                list.add(readRow(c));
            }
        }
        return list;
    }

    /** @return the item, or null if the id does not exist. */
    @SuppressWarnings("unused") // remove once the detail dialog calls it
    public ActivityItem getById(long id) {
        try (Cursor c = getReadableDatabase().query(TABLE, null,
                COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (c.moveToFirst()) {
                return readRow(c);
            }
        }
        return null;
    }

    // ------------------------------------------------------------
    // Checklist item 8: Delete & Recalculate
    // ------------------------------------------------------------
    /**
     * @return true if a row was deleted.
     * Call MainActivity.refreshSavedSummary() afterward to update the totals.
     */
    @SuppressWarnings("unused") // remove once the delete button calls it
    public boolean delete(long id) {
        int rows = getWritableDatabase().delete(TABLE, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        return rows > 0;
    }

    /** Totals and maxima are always recomputed from the rows that exist now. */
    @Override
    public Summary readSummary() {
        String sql = "SELECT COALESCE(SUM(" + COL_STEPS + "),0), "
                + "COALESCE(SUM(" + COL_SHAKES + "),0), "
                + "COALESCE(MAX(" + COL_STEPS + "),0), "
                + "COALESCE(MAX(" + COL_SHAKES + "),0), "
                + "COUNT(*) FROM " + TABLE;
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                return new Summary(c.getLong(0), c.getLong(1),
                        c.getInt(2), c.getInt(3), c.getInt(4));
            }
        }
        return new Summary(0, 0, 0, 0, 0);
    }

    private ActivityItem readRow(Cursor c) {
        return new ActivityItem(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(COL_STARTED)),
                c.getLong(c.getColumnIndexOrThrow(COL_DURATION)),
                c.getInt(c.getColumnIndexOrThrow(COL_STEPS)),
                c.getInt(c.getColumnIndexOrThrow(COL_SHAKES)),
                c.getDouble(c.getColumnIndexOrThrow(COL_STEPS_PM)),
                c.getDouble(c.getColumnIndexOrThrow(COL_SHAKES_PM)));
    }
}