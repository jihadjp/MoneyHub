package com.jptechgenius.moneyhub.data.model;

public class Note {
    private long id;
    private String title;
    private String content;
    private long date;

    public Note(long id, String title, String content, long date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getDate() { return date; }
}
