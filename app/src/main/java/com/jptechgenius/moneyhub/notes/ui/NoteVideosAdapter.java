package com.jptechgenius.moneyhub.notes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jptechgenius.moneyhub.R;

import java.util.List;

/**
 * Horizontal video strip inside the Note Editor.
 * Each item shows a video thumbnail with a play-button overlay and a remove (×) button.
 * Tapping the thumbnail fires onVideoClick for the caller to open a player.
 */
public class NoteVideosAdapter extends RecyclerView.Adapter<NoteVideosAdapter.VideoViewHolder> {

    public interface OnVideoRemovedListener {
        void onRemove(String videoPath);
    }

    public interface OnVideoClickListener {
        void onClick(String videoPath);
    }

    private final List<String> paths;
    private final OnVideoRemovedListener removeListener;
    private OnVideoClickListener clickListener;

    public NoteVideosAdapter(List<String> paths, OnVideoRemovedListener removeListener) {
        this.paths = paths;
        this.removeListener = removeListener;
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String path = paths.get(position);

        // Load video thumbnail via Glide (handles both file paths and content URIs)
        Glide.with(holder.thumbnail.getContext())
                .load(path)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(R.drawable.ic_video_placeholder)
                .into(holder.thumbnail);

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(path);
        });

        holder.thumbnail.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(path);
        });

        holder.playOverlay.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(path);
        });
    }

    @Override
    public int getItemCount() { return paths.size(); }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageView playOverlay;
        final ImageButton removeButton;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail    = itemView.findViewById(R.id.videoThumb);
            playOverlay  = itemView.findViewById(R.id.videoPlayOverlay);
            removeButton = itemView.findViewById(R.id.removeVideoButton);
        }
    }
}