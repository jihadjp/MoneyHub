package com.jptechgenius.moneyhub.notes.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;

@Database(entities = {Note.class, Label.class}, version = 7, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class NotesDatabase extends RoomDatabase {

    private static volatile NotesDatabase INSTANCE;
    public abstract NoteDao noteDao();

    // ── Migration 4 → 5 ──────────────────────────────────────────────────
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notes_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "title TEXT,content TEXT," +
                    "timestamp INTEGER NOT NULL," +
                    "image_paths TEXT,voice_path TEXT," +
                    "color INTEGER NOT NULL," +
                    "todo_data TEXT,status TEXT," +
                    "trashed_at INTEGER NOT NULL,label_ids TEXT)");
            db.execSQL("INSERT INTO notes_new(id,title,content,timestamp,image_paths," +
                    "voice_path,color,todo_data,status,trashed_at,label_ids) " +
                    "SELECT _id,title,content," +
                    "CAST(strftime('%s',timestamp) AS INTEGER)*1000," +
                    "CASE WHEN image_path IS NOT NULL AND image_path!='' " +
                    "THEN '[\"'||image_path||'\"]' ELSE '[]' END," +
                    "NULL,COALESCE(color,-1),todo_data,'ACTIVE',0,'[]' FROM notes");
            db.execSQL("DROP TABLE notes");
            db.execSQL("ALTER TABLE notes_new RENAME TO notes");
            db.execSQL("CREATE TABLE IF NOT EXISTS labels(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT,color_hex TEXT)");
        }
    };

    // ── Migration 5 → 6  (adds video_paths) ──────────────────────────────
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notes ADD COLUMN video_paths TEXT");
        }
    };

    // ── Migration 6 → 7  (full table rebuild) ────────────────────────────
    //
    // Why a full rebuild instead of simple ALTER TABLE?
    //
    //  1. Room requires ALL columns to have defaultValue='undefined' in its
    //     schema hash. Any column added via ALTER TABLE … DEFAULT '…' carries
    //     a stored default that Room sees as a mismatch → crash.
    //
    //  2. The old 'voice_path' TEXT column must be removed (SQLite < 3.35
    //     has no DROP COLUMN).  We convert it to the new 'voice_paths' JSON
    //     array column at the same time.
    //
    //  3. Previous migrations left DEFAULT '0', DEFAULT '-1', DEFAULT '[]'
    //     on several columns.  The rebuild strips all of those.
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(SupportSQLiteDatabase db) {

            // Step 1 — create the target table with NO DEFAULT clauses
            db.execSQL("CREATE TABLE IF NOT EXISTS notes_v7 (" +
                    "id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "title       TEXT," +
                    "content     TEXT," +
                    "timestamp   INTEGER NOT NULL," +
                    "image_paths TEXT," +
                    "video_paths TEXT," +
                    "voice_paths TEXT," +
                    "color       INTEGER NOT NULL," +
                    "todo_data   TEXT," +
                    "status      TEXT," +
                    "trashed_at  INTEGER NOT NULL," +
                    "label_ids   TEXT)");

            // Step 2 — copy rows; convert voice_path → voice_paths JSON array
            db.execSQL(
                    "INSERT INTO notes_v7 " +
                            "(id,title,content,timestamp,image_paths,video_paths,voice_paths," +
                            " color,todo_data,status,trashed_at,label_ids) " +
                            "SELECT " +
                            "  id, title, content, timestamp, image_paths," +
                            "  COALESCE(video_paths, '[]')," +
                            "  CASE WHEN voice_path IS NOT NULL AND voice_path != '' " +
                            "       THEN '[\"' || voice_path || '\"]' " +
                            "       ELSE '[]' END," +
                            "  color, todo_data, status, trashed_at," +
                            "  COALESCE(label_ids, '[]') " +
                            "FROM notes");

            // Step 3 — swap
            db.execSQL("DROP TABLE notes");
            db.execSQL("ALTER TABLE notes_v7 RENAME TO notes");
        }
    };

    // ── Singleton ─────────────────────────────────────────────────────────
    public static NotesDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NotesDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    NotesDatabase.class, "notes.db")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}