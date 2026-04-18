package com.jptechgenius.moneyhub.notes.ui;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.jptechgenius.moneyhub.notes.db.NoteRepository;
import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;
import com.jptechgenius.moneyhub.notes.model.NoteStatus;

import java.util.List;

public class NotesViewModel extends AndroidViewModel {

    private final NoteRepository repository;

    // ── Filter / view state ───────────────────────────────────────────────
    private final MutableLiveData<String>     searchQuery  = new MutableLiveData<>("");
    private final MutableLiveData<Long>       filterLabelId = new MutableLiveData<>(-1L);
    private final MutableLiveData<NoteStatus> currentView  = new MutableLiveData<>(NoteStatus.ACTIVE);

    // ── Derived LiveData ──────────────────────────────────────────────────
    private final LiveData<List<Note>>  notes;
    private final LiveData<List<Label>> labels;

    public NotesViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);

        // Auto-purge trash entries older than 30 days on startup
        repository.purgeExpiredTrash();

        // notes reacts to ALL three: currentView, searchQuery, filterLabelId
        notes = Transformations.switchMap(currentView, status -> {

            if (status == NoteStatus.ARCHIVED) {
                return repository.getArchivedNotes();
            }

            if (status == NoteStatus.TRASH) {
                return repository.getTrashNotes();
            }

            // ACTIVE: also react to search and label filter
            return Transformations.switchMap(searchQuery, query -> {

                if (query != null && !query.trim().isEmpty()) {
                    // Search overrides label filter
                    return repository.searchNotes(query.trim());
                }

                return Transformations.switchMap(filterLabelId, labelId -> {
                    if (labelId != null && labelId >= 0) {
                        return repository.getNotesByLabel(labelId);
                    }
                    return repository.getActiveNotes();
                });
            });
        });

        labels = repository.getAllLabels();
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────

    public LiveData<List<Note>>  getNotes()      { return notes; }
    public LiveData<List<Label>> getLabels()     { return labels; }
    public LiveData<NoteStatus>  getCurrentView(){ return currentView; }

    // ── State setters ─────────────────────────────────────────────────────

    public void setSearchQuery(String query)    { searchQuery.setValue(query != null ? query : ""); }
    public void setFilterLabel(long labelId)    { filterLabelId.setValue(labelId); }
    public void setCurrentView(NoteStatus status) {
        if (status != null) currentView.setValue(status);
    }

    // ── Note CRUD ─────────────────────────────────────────────────────────

    /** Insert or update. Calls back with the row id (insert) or existing id (update). */
    public void saveNote(Note note, NoteRepository.OnResultCallback<Long> callback) {
        if (note.getId() == 0) {
            repository.insert(note, callback);
        } else {
            repository.update(note, id -> {
                if (callback != null) callback.onResult(note.getId());
            });
        }
    }

    public void trashNote(Note note)              { repository.trashNote(note); }
    public void archiveNote(Note note)            { repository.archiveNote(note); }
    public void restoreNote(Note note)            { repository.restoreNote(note); }

    /** Hard-delete a single note from the database (used in Trash view). */
    public void deleteNotePermanently(Note note)  { repository.deleteNotePermanently(note); }

    /** Hard-delete all notes currently in Trash. */
    public void emptyTrash()                      { repository.emptyTrash(); }

    /** Hard-delete every note in the database (dev/debug use). */
    public void deleteAllNotes()                  { repository.deleteAllNotes(); }

    // ── Labels ────────────────────────────────────────────────────────────

    public void addLabel(Label label)             { repository.insertLabel(label, null); }

    /** Delete a label and remove it from all notes that reference it. */
    public void deleteLabel(Label label)          { repository.deleteLabel(label); }

    // ── Backup / Restore ─────────────────────────────────────────────────

    public void exportNotes(Uri uri, NoteRepository.OnResultCallback<Boolean> cb) {
        repository.exportNotes(uri, cb);
    }

    public void importNotes(Uri uri, NoteRepository.OnResultCallback<Boolean> cb) {
        repository.importNotes(uri, cb);
    }
}