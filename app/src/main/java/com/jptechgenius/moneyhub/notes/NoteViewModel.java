package com.jptechgenius.moneyhub.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.jptechgenius.moneyhub.data.local.entity.NoteEntity;
import com.jptechgenius.moneyhub.data.repository.NoteRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NoteViewModel extends ViewModel {
    private final NoteRepository repository;
    private final LiveData<List<NoteEntity>> allNotes;

    @Inject
    public NoteViewModel(NoteRepository repository) {
        this.repository = repository;
        this.allNotes = repository.getAllNotes();
    }

    public LiveData<List<NoteEntity>> getAllNotes() {
        return allNotes;
    }

    public void insert(NoteEntity note) {
        repository.insert(note);
    }
    
    public void delete(NoteEntity note) {
        repository.delete(note);
    }
}
