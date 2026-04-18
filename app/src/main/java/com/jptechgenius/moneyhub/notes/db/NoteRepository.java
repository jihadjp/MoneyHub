package com.jptechgenius.moneyhub.notes.db;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;
import com.jptechgenius.moneyhub.notes.model.NoteStatus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    private static final String TAG = "NoteRepository";

    private final NoteDao         dao;
    private final Context         context;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public NoteRepository(Context context) {
        this.context = context.getApplicationContext();
        dao = NotesDatabase.getInstance(this.context).noteDao();
    }

    // ── Insert ────────────────────────────────────────────────────────────

    public void insert(Note note, OnResultCallback<Long> callback) {
        executor.execute(() -> {
            note.setTimestamp(System.currentTimeMillis());
            long id = dao.insertNote(note);
            if (callback != null) callback.onResult(id);
        });
    }

    public void insertLabel(Label label, OnResultCallback<Long> callback) {
        executor.execute(() -> {
            long id = dao.insertLabel(label);
            if (callback != null) callback.onResult(id);
        });
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update(Note note, OnResultCallback<Void> callback) {
        executor.execute(() -> {
            note.setTimestamp(System.currentTimeMillis());
            dao.updateNote(note);
            if (callback != null) callback.onResult(null);
        });
    }

    public void updateLabel(Label label) {
        executor.execute(() -> dao.updateLabel(label));
    }

    // ── Status changes ────────────────────────────────────────────────────

    public void archiveNote(Note note) {
        executor.execute(() -> {
            note.setStatus(NoteStatus.ARCHIVED);
            dao.updateNote(note);
        });
    }

    public void restoreNote(Note note) {
        executor.execute(() -> {
            note.setStatus(NoteStatus.ACTIVE);
            note.setTrashedAt(0);
            dao.updateNote(note);
        });
    }

    /**
     * Moves a note to Trash (sets status + timestamp).
     * If already in Trash, permanently deletes it.
     */
    public void trashNote(Note note) {
        executor.execute(() -> {
            if (note.isTrashed()) {
                dao.deleteNote(note);
            } else {
                note.setStatus(NoteStatus.TRASH);
                note.setTrashedAt(System.currentTimeMillis());
                dao.updateNote(note);
            }
        });
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /** Hard-delete a single note from the database immediately. */
    public void deleteNotePermanently(Note note) {
        executor.execute(() -> dao.deleteNote(note));
    }

    /** Hard-delete all notes currently in Trash. */
    public void emptyTrash() {
        executor.execute(dao::emptyTrash);
    }

    /** Hard-delete every note (dev/debug use). */
    public void deleteAllNotes() {
        executor.execute(dao::deleteAllNotes);
    }

    /** Delete a label row; also strips the label id from every note that references it. */
    public void deleteLabel(Label label) {
        executor.execute(() -> {
            dao.deleteLabel(label);
            // Remove this label id from all notes that carry it
            List<Note> allNotes = dao.getAllNotesSynchronous();
            if (allNotes == null) return;
            for (Note note : allNotes) {
                List<Long> ids = note.getLabelIds();
                if (ids != null && ids.remove(label.getId())) {
                    note.setLabelIds(ids);
                    dao.updateNote(note);
                }
            }
        });
    }

    /** Auto-purge notes trashed more than 30 days ago. Call on app start. */
    public void purgeExpiredTrash() {
        executor.execute(() -> dao.purgeExpiredTrash(System.currentTimeMillis()));
    }

    // ── Queries (LiveData — observed on UI thread) ────────────────────────

    public LiveData<List<Note>>  getActiveNotes()          { return dao.getActiveNotes(); }
    public LiveData<List<Note>>  getArchivedNotes()        { return dao.getArchivedNotes(); }
    public LiveData<List<Note>>  getTrashNotes()           { return dao.getTrashNotes(); }
    public LiveData<List<Note>>  searchNotes(String query) { return dao.searchActiveNotes(query); }
    public LiveData<List<Note>>  getNotesByLabel(long id)  { return dao.getNotesByLabel(id); }
    public LiveData<List<Label>> getAllLabels()             { return dao.getAllLabels(); }

    /** Synchronous — call from a background thread only. */
    public Note        getNoteById(long id)          { return dao.getNoteById(id); }
    /** Synchronous — call from a background thread only. */
    public List<Label> getLabelsByIds(List<Long> ids){ return dao.getLabelsByIds(ids); }

    // ── Export / Import ───────────────────────────────────────────────────

    /**
     * Exports ALL notes (active + archived + trash) to a JSON file at the
     * given URI. Runs on the executor thread; result is delivered via callback.
     */
    public void exportNotes(Uri uri, OnResultCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                List<Note> notes = dao.getAllNotesSynchronous();
                if (notes == null) notes = new ArrayList<>();

                // Strip media paths that are device-local file paths so the
                // recipient device doesn't end up with broken references.
                // Content text, title, labels and colors are always preserved.
                for (Note n : notes) {
                    n.setImagePaths(filterExistingPaths(n.getImagePaths()));
                    n.setVideoPaths(filterExistingPaths(n.getVideoPaths()));
                    n.setVoicePaths(filterExistingPaths(n.getVoicePaths()));
                }

                String json = new Gson().toJson(notes);
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os == null) throw new Exception("Could not open output stream");
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                if (callback != null) callback.onResult(true);
            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                if (callback != null) callback.onResult(false);
            }
        });
    }

    /** Returns only paths that actually exist on this device. */
    private List<String> filterExistingPaths(List<String> paths) {
        if (paths == null) return new ArrayList<>();
        List<String> valid = new ArrayList<>();
        for (String p : paths) {
            if (p == null) continue;
            if (p.startsWith("content://")) {
                valid.add(p); // content URIs — keep, resolver will fail gracefully
            } else {
                if (new java.io.File(p).exists()) valid.add(p);
            }
        }
        return valid;
    }

    /**
     * Imports notes from a JSON file. Each note gets a fresh auto-generated id
     * and is set to ACTIVE status to avoid duplicating archived/trash state.
     */
    public void importNotes(Uri uri, OnResultCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("Could not open input stream");

                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                Type listType = new TypeToken<ArrayList<Note>>() {}.getType();
                List<Note> imported = new Gson().fromJson(sb.toString(), listType);

                if (imported == null || imported.isEmpty()) {
                    if (callback != null) callback.onResult(false);
                    return;
                }

                for (Note n : imported) {
                    n.setId(0);                           // let Room generate a new id
                    n.setStatus(NoteStatus.ACTIVE);       // always restore as active
                    n.setTrashedAt(0);
                    n.setTimestamp(System.currentTimeMillis());
                    // Remove any media paths that don't exist on this device
                    n.setImagePaths(filterExistingPaths(n.getImagePaths()));
                    n.setVideoPaths(filterExistingPaths(n.getVideoPaths()));
                    n.setVoicePaths(filterExistingPaths(n.getVoicePaths()));
                    dao.insertNote(n);
                }

                if (callback != null) callback.onResult(true);
            } catch (Exception e) {
                Log.e(TAG, "Import failed", e);
                if (callback != null) callback.onResult(false);
            }
        });
    }

    // ── Callback interface ────────────────────────────────────────────────

    public interface OnResultCallback<T> {
        void onResult(T result);
    }
}