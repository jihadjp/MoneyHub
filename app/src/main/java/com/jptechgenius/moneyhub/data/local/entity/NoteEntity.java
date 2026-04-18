package com.jptechgenius.moneyhub.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a quick Note entry.
 * Maps to the 'notes' table in the Room database.
 */
@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "body")
    private String body;

    /** Creation timestamp (epoch millis) */
    @ColumnInfo(name = "created_at")
    private long createdAt;

    /** Last modified timestamp (epoch millis) */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    /**
     * Background color for the note card.
     * Store as a color resource name: "color_note_yellow", "color_note_pink", etc.
     */
    @ColumnInfo(name = "color_tag")
    private String colorTag;

    // ---- Constructors ----

    public NoteEntity() {}

    public NoteEntity(String title, String body, long createdAt, String colorTag) {
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.colorTag = colorTag;
    }

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getColorTag() { return colorTag; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }
}