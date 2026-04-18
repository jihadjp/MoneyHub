package com.jptechgenius.moneyhub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.jptechgenius.moneyhub.data.local.entity.NoteEntity;
import java.util.List;

/**
 * Data Access Object for notes.
 */
@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes WHERE " +
            "title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' " +
            "ORDER BY updated_at DESC")
    LiveData<List<NoteEntity>> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE id = :id")
    LiveData<NoteEntity> getNoteById(int id);
}