package com.jptechgenius.moneyhub.notes.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jptechgenius.moneyhub.notes.db.Converters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "notes")
@TypeConverters(Converters.class)
public class Note implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "image_paths")
    private List<String> imagePaths;

    @ColumnInfo(name = "video_paths")
    private List<String> videoPaths;

    /** JSON array of voice memo file paths — replaces single voice_path in v7 */
    @ColumnInfo(name = "voice_paths")
    private List<String> voicePaths;

    @ColumnInfo(name = "color")
    private int color;

    @ColumnInfo(name = "todo_data")
    private String todoData;

    @ColumnInfo(name = "status")
    private NoteStatus status;

    @ColumnInfo(name = "trashed_at")
    private long trashedAt;

    @ColumnInfo(name = "label_ids")
    private List<Long> labelIds;

    @Ignore
    private List<Label> labels;

    public Note() {
        this.color      = -1;
        this.status     = NoteStatus.ACTIVE;
        this.timestamp  = System.currentTimeMillis();
        this.imagePaths = new ArrayList<>();
        this.videoPaths = new ArrayList<>();
        this.voicePaths = new ArrayList<>();
        this.labelIds   = new ArrayList<>();
    }

    protected Note(Parcel in) {
        id        = in.readLong();
        title     = in.readString();
        content   = in.readString();
        timestamp = in.readLong();
        color     = in.readInt();
        todoData  = in.readString();
        status    = NoteStatus.valueOf(in.readString());
        trashedAt = in.readLong();
        imagePaths = in.createStringArrayList();
        videoPaths = in.createStringArrayList();
        voicePaths = in.createStringArrayList();
        labelIds = new ArrayList<>();
        long[] ids = in.createLongArray();
        if (ids != null) for (long l : ids) labelIds.add(l);
    }

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override public Note createFromParcel(Parcel in) { return new Note(in); }
        @Override public Note[] newArray(int size)        { return new Note[size]; }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeLong(timestamp);
        dest.writeInt(color);
        dest.writeString(todoData);
        dest.writeString(status.name());
        dest.writeLong(trashedAt);
        dest.writeStringList(imagePaths  != null ? imagePaths  : new ArrayList<>());
        dest.writeStringList(videoPaths  != null ? videoPaths  : new ArrayList<>());
        dest.writeStringList(voicePaths  != null ? voicePaths  : new ArrayList<>());
        long[] ids = new long[labelIds != null ? labelIds.size() : 0];
        if (labelIds != null) for (int i = 0; i < labelIds.size(); i++) ids[i] = labelIds.get(i);
        dest.writeLongArray(ids);
    }

    @Override public int describeContents() { return 0; }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public long getId()                      { return id; }
    public void setId(long id)               { this.id = id; }

    public String getTitle()                 { return title; }
    public void setTitle(String t)           { this.title = t; }

    public String getContent()               { return content; }
    public void setContent(String c)         { this.content = c; }

    public long getTimestamp()               { return timestamp; }
    public void setTimestamp(long t)         { this.timestamp = t; }

    public List<String> getImagePaths()      { return imagePaths  != null ? imagePaths  : new ArrayList<>(); }
    public void setImagePaths(List<String> l){ this.imagePaths = l; }

    public List<String> getVideoPaths()      { return videoPaths  != null ? videoPaths  : new ArrayList<>(); }
    public void setVideoPaths(List<String> l){ this.videoPaths = l; }

    public List<String> getVoicePaths()      { return voicePaths  != null ? voicePaths  : new ArrayList<>(); }
    public void setVoicePaths(List<String> l){ this.voicePaths = l; }

    public int getColor()                    { return color; }
    public void setColor(int c)              { this.color = c; }

    public String getTodoData()              { return todoData; }
    public void setTodoData(String t)        { this.todoData = t; }

    public NoteStatus getStatus()            { return status; }
    public void setStatus(NoteStatus s)      { this.status = s; }

    public long getTrashedAt()               { return trashedAt; }
    public void setTrashedAt(long t)         { this.trashedAt = t; }

    public List<Long> getLabelIds()          { return labelIds != null ? labelIds : new ArrayList<>(); }
    public void setLabelIds(List<Long> l)    { this.labelIds = l; }

    public List<Label> getLabels()           { return labels; }
    public void setLabels(List<Label> l)     { this.labels = l; }

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean hasImages()   { return imagePaths != null && !imagePaths.isEmpty(); }
    public boolean hasVideos()   { return videoPaths != null && !videoPaths.isEmpty(); }
    public boolean hasVoice()    { return voicePaths != null && !voicePaths.isEmpty(); }
    public boolean isActive()    { return status == NoteStatus.ACTIVE; }
    public boolean isArchived()  { return status == NoteStatus.ARCHIVED; }
    public boolean isTrashed()   { return status == NoteStatus.TRASH; }

    public boolean isExpiredFromTrash() {
        if (!isTrashed() || trashedAt == 0) return false;
        return (System.currentTimeMillis() - trashedAt) > 30L * 24 * 60 * 60 * 1000;
    }
}