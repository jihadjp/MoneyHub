package com.jptechgenius.moneyhub.notes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jptechgenius.moneyhub.R;

import java.util.List;

/**
 * Horizontal image strip inside the Note Editor.
 * Each item shows a thumbnail with a remove (×) button.
 * Tapping the image opens a full-screen preview callback.
 */
public class NoteImagesAdapter extends RecyclerView.Adapter<NoteImagesAdapter.ImageViewHolder> {

    public interface OnImageRemovedListener {
        void onRemove(String imagePath);
    }

    public interface OnImageClickListener {
        void onClick(String imagePath, int position);
    }

    private final List<String> paths;
    private final OnImageRemovedListener removeListener;
    private OnImageClickListener clickListener;

    public NoteImagesAdapter(List<String> paths, OnImageRemovedListener removeListener) {
        this.paths = paths;
        this.removeListener = removeListener;
    }

    /** Optional: set to handle fullscreen image preview. */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String path = paths.get(position);

        Glide.with(holder.imageView.getContext())
                .load(path)
                .centerCrop()
                .into(holder.imageView);

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(path);
        });

        // Tap image → fullscreen preview
        holder.imageView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(path, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return paths.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ImageButton removeButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView    = itemView.findViewById(R.id.noteImageThumb);
            removeButton = itemView.findViewById(R.id.removeImageButton);
        }
    }
}