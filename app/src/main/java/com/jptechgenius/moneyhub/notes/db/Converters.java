package com.jptechgenius.moneyhub.notes.db;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jptechgenius.moneyhub.notes.model.NoteStatus;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {

    private static final Gson gson = new Gson();

    // ── List<String> ──────────────────────────────────────────────────────────

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // ── List<Long> ────────────────────────────────────────────────────────────

    @TypeConverter
    public static String fromLongList(List<Long> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Long> toLongList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Long>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // ── NoteStatus ────────────────────────────────────────────────────────────

    @TypeConverter
    public static String fromNoteStatus(NoteStatus status) {
        return status == null ? NoteStatus.ACTIVE.name() : status.name();
    }

    @TypeConverter
    public static NoteStatus toNoteStatus(String value) {
        try {
            return NoteStatus.valueOf(value);
        } catch (Exception e) {
            return NoteStatus.ACTIVE;
        }
    }
}
