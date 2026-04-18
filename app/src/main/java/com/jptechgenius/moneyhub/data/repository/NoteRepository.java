package com.jptechgenius.moneyhub.data.repository;

import androidx.lifecycle.LiveData;
import com.jptechgenius.moneyhub.data.local.dao.NoteDao;
import com.jptechgenius.moneyhub.data.local.entity.NoteEntity;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NoteRepository {
    private final NoteDao noteDao;

    @Inject
    public NoteRepository(NoteDao noteDao) {
        this.noteDao = noteDao;
    }

    public void insert(NoteEntity note) {
        new Thread(() -> noteDao.insert(note)).start();
    }

    public void update(NoteEntity note) {
        new Thread(() -> noteDao.update(note)).start();
    }

    public void delete(NoteEntity note) {
        new Thread(() -> noteDao.delete(note)).start();
    }

    public LiveData<List<NoteEntity>> getAllNotes() {
        return noteDao.getAllNotes();
    }
}
