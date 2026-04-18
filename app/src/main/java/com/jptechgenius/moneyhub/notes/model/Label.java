package com.jptechgenius.moneyhub.notes.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "labels")
public class Label {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    /** Hex color string, e.g. "#E8F5E9" for label chip background */
    @ColumnInfo(name = "color_hex")
    private String colorHex;

    public Label() {}

    public Label(String name, String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
