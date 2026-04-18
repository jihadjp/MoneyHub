package com.jptechgenius.moneyhub.notes.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.jptechgenius.moneyhub.R;

/**
 * Material 3 colour-swatch picker for note card backgrounds.
 * Shows a 4×3 grid of curated pastel + vivid colours plus a "no colour" option.
 */
public class NoteColorPicker {

    public interface OnColorSelected {
        void onColorSelected(int color);
    }

    /** Curated palette: pastel tones ideal for note cards */
    private static final int[] COLORS = {
        -1,                          // No color (white / surface)
        Color.parseColor("#F28B82"), // Red
        Color.parseColor("#FBBC04"), // Yellow
        Color.parseColor("#FFF475"), // Light yellow
        Color.parseColor("#CCFF90"), // Green
        Color.parseColor("#A8DAB5"), // Teal
        Color.parseColor("#CBF0F8"), // Cyan
        Color.parseColor("#AECBFA"), // Blue
        Color.parseColor("#D7AEFB"), // Purple
        Color.parseColor("#FDCFE8"), // Pink
        Color.parseColor("#E6C9A8"), // Brown/beige
        Color.parseColor("#E8EAED"), // Gray
    };

    private static final String[] NAMES = {
        "Default", "Tomato", "Banana", "Citron",
        "Sage", "Basil", "Peacock", "Blueberry",
        "Lavender", "Flamingo", "Graphite", "Pebble"
    };

    public static void show(Context context, int currentColor, OnColorSelected callback) {
        View grid = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        GridLayout gridLayout = grid.findViewById(R.id.colorGrid);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Card colour")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create();

        for (int i = 0; i < COLORS.length; i++) {
            final int color = COLORS[i];
            View swatch = LayoutInflater.from(context)
                .inflate(R.layout.item_color_swatch, gridLayout, false);

            View circle = swatch.findViewById(R.id.colorCircle);
            ImageView checkmark = swatch.findViewById(R.id.checkmark);

            // Draw circle
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            if (color == -1) {
                drawable.setColor(Color.WHITE);
                drawable.setStroke(3, Color.parseColor("#BDBDBD"));
            } else {
                drawable.setColor(color);
                drawable.setStroke(2, darkenColor(color, 0.80f));
            }
            circle.setBackground(drawable);

            // Checkmark for current color
            boolean isSelected = (color == currentColor) || (color == -1 && currentColor == -1);
            checkmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            swatch.setOnClickListener(v -> {
                callback.onColorSelected(color);
                dialog.dismiss();
            });

            gridLayout.addView(swatch);
        }

        dialog.show();
    }

    private static int darkenColor(int color, float factor) {
        return Color.rgb(
            (int) (Color.red(color) * factor),
            (int) (Color.green(color) * factor),
            (int) (Color.blue(color) * factor));
    }
}
