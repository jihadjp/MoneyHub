package com.jptechgenius.moneyhub.notes.ui;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jptechgenius.moneyhub.R;
import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Material 3 NotesAdapter using ListAdapter + DiffUtil.
 * Supports: colored cards, multi-image preview, label chips,
 *           voice/image badges, multi-select highlighting.
 */
public class NotesAdapter extends ListAdapter<Note, NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClicked(Note note, View sharedElement);
    }

    public interface OnNoteLongClickListener {
        boolean onNoteLongClicked(Note note);
    }

    private final OnNoteClickListener     clickListener;
    private final OnNoteLongClickListener longClickListener;

    // ── Multi-select state ────────────────────────────────────────────────
    private Set<Long> selectedIds = Collections.emptySet();

    /** Called from Fragment when selection changes. Triggers a full rebind. */
    public void setSelectedIds(Set<Long> ids) {
        this.selectedIds = ids != null ? ids : Collections.emptySet();
        notifyDataSetChanged();
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────

    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Note>() {
                @Override
                public boolean areItemsTheSame(@NonNull Note a, @NonNull Note b) {
                    return a.getId() == b.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Note a, @NonNull Note b) {
                    return a.getTimestamp() == b.getTimestamp()
                            && safeEquals(a.getTitle(),   b.getTitle())
                            && safeEquals(a.getContent(), b.getContent())
                            && a.getColor() == b.getColor();
                }

                private boolean safeEquals(String s1, String s2) {
                    if (s1 == null && s2 == null) return true;
                    if (s1 == null || s2 == null) return false;
                    return s1.equals(s2);
                }
            };

    // ── Constructor ───────────────────────────────────────────────────────

    public NotesAdapter(OnNoteClickListener     clickListener,
                        OnNoteLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener     = clickListener;
        this.longClickListener = longClickListener;
    }

    // ── RecyclerView ──────────────────────────────────────────────────────

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note     = getItem(position);
        boolean selected = selectedIds.contains(note.getId());
        holder.bind(note, selected, clickListener, longClickListener);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final TextView         titleText;
        private final TextView         contentText;
        private final TextView         timestampText;
        private final ImageView        previewImage;
        private final TextView         imageCountBadge;
        private final ImageView        voiceIcon;
        private final ChipGroup        labelChipGroup;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView        = itemView.findViewById(R.id.noteCard);
            titleText       = itemView.findViewById(R.id.titleTextView);
            contentText     = itemView.findViewById(R.id.contentTextView);
            timestampText   = itemView.findViewById(R.id.timestampTextView);
            previewImage    = itemView.findViewById(R.id.itemNoteImage);
            imageCountBadge = itemView.findViewById(R.id.imageCountBadge);
            voiceIcon       = itemView.findViewById(R.id.voiceIcon);
            labelChipGroup  = itemView.findViewById(R.id.labelChipGroup);
        }

        void bind(Note note,
                  boolean selected,
                  OnNoteClickListener     clickListener,
                  OnNoteLongClickListener longClickListener) {

            // ── Title ──────────────────────────────────────────────────────
            String title = note.getTitle();
            if (title != null && !title.isEmpty()) {
                titleText.setVisibility(View.VISIBLE);
                titleText.setText(title);
            } else {
                titleText.setVisibility(View.GONE);
            }

            // ── Content preview (strip HTML) ───────────────────────────────
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                contentText.setVisibility(View.VISIBLE);
                contentText.setText(
                        Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_LEGACY),
                        TextView.BufferType.SPANNABLE);
            } else {
                contentText.setVisibility(View.GONE);
            }

            // ── Timestamp ──────────────────────────────────────────────────
            timestampText.setText(formatTimestamp(note.getTimestamp()));

            // ── Card color + selection state ───────────────────────────────
            if (selected) {
                // Selected: strong primary-tinted stroke + slight dim
                cardView.setCardBackgroundColor(
                        blendColor(0xFF2979FF, note.getColor() != -1 ? note.getColor()
                                : 0xFFFFFFFF, 0.18f));
                cardView.setStrokeWidth(dpToPx(2));
                cardView.setStrokeColor(0xFF2979FF);   // primary blue
                itemView.setAlpha(0.88f);
            } else {
                itemView.setAlpha(1f);
                cardView.setStrokeWidth(dpToPx(1));
                if (note.getColor() != -1) {
                    cardView.setCardBackgroundColor(note.getColor());
                    cardView.setStrokeColor(darkenColor(note.getColor(), 0.85f));
                } else {
                    cardView.setCardBackgroundColor(Color.TRANSPARENT);
                    cardView.setStrokeColor(Color.parseColor("#1A000000"));
                }
            }

            // ── Image preview ──────────────────────────────────────────────
            List<String> images = note.getImagePaths();
            if (images != null && !images.isEmpty()) {
                previewImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(images.get(0))
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .centerCrop()
                        .into(previewImage);

                if (images.size() > 1) {
                    imageCountBadge.setVisibility(View.VISIBLE);
                    imageCountBadge.setText("+" + (images.size() - 1));
                } else {
                    imageCountBadge.setVisibility(View.GONE);
                }
            } else {
                previewImage.setVisibility(View.GONE);
                imageCountBadge.setVisibility(View.GONE);
            }

            // ── Voice badge ────────────────────────────────────────────────
            voiceIcon.setVisibility(note.hasVoice() ? View.VISIBLE : View.GONE);

            // ── Label chips ────────────────────────────────────────────────
            labelChipGroup.removeAllViews();
            List<Label> labels = note.getLabels();
            if (labels != null && !labels.isEmpty()) {
                labelChipGroup.setVisibility(View.VISIBLE);
                for (Label label : labels) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(label.getName());
                    chip.setTextSize(10f);
                    chip.setChipMinHeight(24f * itemView.getContext()
                            .getResources().getDisplayMetrics().density);
                    chip.setClickable(false);
                    chip.setFocusable(false);
                    chip.setChipStrokeWidth(0f);

                    if (label.getColorHex() != null) {
                        try {
                            int labelColor = Color.parseColor(label.getColorHex());
                            chip.setChipBackgroundColor(
                                    android.content.res.ColorStateList.valueOf(labelColor));
                            chip.setTextColor(isColorDark(labelColor)
                                    ? Color.WHITE : Color.BLACK);
                        } catch (Exception ignored) {}
                    }
                    labelChipGroup.addView(chip);
                }
            } else {
                labelChipGroup.setVisibility(View.GONE);
            }

            // ── Shared element transition name ─────────────────────────────
            cardView.setTransitionName("note_card_" + note.getId());

            // ── Click / long-click ─────────────────────────────────────────
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onNoteClicked(note, cardView);
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) return longClickListener.onNoteLongClicked(note);
                return false;
            });
        }

        // ── Helpers ────────────────────────────────────────────────────────

        private String formatTimestamp(long epochMs) {
            if (epochMs == 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault());
            return sdf.format(new Date(epochMs));
        }

        private int darkenColor(int color, float factor) {
            return Color.rgb(
                    (int) (Color.red(color)   * factor),
                    (int) (Color.green(color) * factor),
                    (int) (Color.blue(color)  * factor));
        }

        /**
         * Blend two ARGB colors. ratio=0 → all color1, ratio=1 → all color2.
         */
        private int blendColor(int color1, int color2, float ratio) {
            float inv = 1f - ratio;
            return Color.rgb(
                    (int) (Color.red(color1)   * inv + Color.red(color2)   * ratio),
                    (int) (Color.green(color1) * inv + Color.green(color2) * ratio),
                    (int) (Color.blue(color1)  * inv + Color.blue(color2)  * ratio));
        }

        private boolean isColorDark(int color) {
            double lum = (0.299 * Color.red(color)
                    + 0.587 * Color.green(color)
                    + 0.114 * Color.blue(color)) / 255.0;
            return lum < 0.5;
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext()
                    .getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}