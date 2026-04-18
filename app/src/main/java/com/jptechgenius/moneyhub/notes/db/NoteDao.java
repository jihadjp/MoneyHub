package com.jptechgenius.moneyhub.notes.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;

import java.util.List;

@Dao
public interface NoteDao {

    // ── INSERT ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLabel(Label label);

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Update
    void updateNote(Note note);

    @Update
    void updateLabel(Label label);

    // ── DELETE ────────────────────────────────────────────────────────────

    @Delete
    void deleteNote(Note note);

    @Delete
    void deleteLabel(Label label);

    @Query("DELETE FROM notes")
    void deleteAllNotes();

    /** Hard-delete all notes currently in TRASH status */
    @Query("DELETE FROM notes WHERE status = 'TRASH'")
    void emptyTrash();

    /**
     * Auto-delete trash notes older than 30 days.
     * 2_592_000_000 ms = 30 days.
     */
    @Query("DELETE FROM notes WHERE status = 'TRASH' " +
            "AND trashed_at > 0 " +
            "AND (:nowMs - trashed_at) > 2592000000")
    void purgeExpiredTrash(long nowMs);

    // ── ACTIVE NOTES ──────────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    LiveData<List<Note>> getActiveNotes();

    @Query("SELECT * FROM notes " +
            "WHERE status = 'ACTIVE' " +
            "AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') " +
            "ORDER BY timestamp DESC")
    LiveData<List<Note>> searchActiveNotes(String query);

    /**
     * Filter active notes by label id.
     * Uses a LIKE check on the JSON label_ids column — pragmatic for small
     * label counts. For large datasets consider a junction table.
     */
    @Query("SELECT * FROM notes " +
            "WHERE status = 'ACTIVE' " +
            "AND label_ids LIKE '%' || :labelId || '%' " +
            "ORDER BY timestamp DESC")
    LiveData<List<Note>> getNotesByLabel(long labelId);

    // ── ARCHIVED ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE status = 'ARCHIVED' ORDER BY timestamp DESC")
    LiveData<List<Note>> getArchivedNotes();

    // ── TRASH ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE status = 'TRASH' ORDER BY trashed_at DESC")
    LiveData<List<Note>> getTrashNotes();

    // ── SINGLE NOTE ───────────────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note getNoteById(long id);

    // ── ALL NOTES (synchronous — background thread only) ──────────────────

    /**
     * Returns every note regardless of status.
     * Used by: exportNotes, deleteLabel cleanup.
     * Must be called from a background thread.
     */
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    List<Note> getAllNotesSynchronous();

    // ── LABELS ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM labels ORDER BY name ASC")
    LiveData<List<Label>> getAllLabels();

    @Query("SELECT * FROM labels WHERE id IN (:ids)")
    List<Label> getLabelsByIds(List<Long> ids);

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    Label getLabelById(long id);
}