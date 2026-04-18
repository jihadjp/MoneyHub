package com.jptechgenius.moneyhub.notes.ui;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jptechgenius.moneyhub.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Voice memo list adapter.
 * Seekbar updates via notifyItemChanged so no stale ViewHolder references.
 */
public class NoteVoicesAdapter extends RecyclerView.Adapter<NoteVoicesAdapter.VoiceViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(String path, int position);
    }

    private final List<String>   paths;
    private OnDeleteClickListener deleteListener;

    // ── Playback state ────────────────────────────────────────────────────
    private int         playingIndex = -1;
    private boolean     isPaused     = false;
    private boolean     isPrepared   = false;  // guards getDuration/getCurrentPosition
    private MediaPlayer mediaPlayer  = null;

    private final Handler  seekHandler  = new Handler(Looper.getMainLooper());
    private Runnable       seekRunnable = null;

    // ── Attach RecyclerView reference so we can find live ViewHolder ──────
    private RecyclerView recyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        this.recyclerView = rv;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        this.recyclerView = null;
    }

    public NoteVoicesAdapter(List<String> paths) { this.paths = paths; }

    public void setOnDeleteClickListener(OnDeleteClickListener l) { this.deleteListener = l; }

    // ── RecyclerView ──────────────────────────────────────────────────────

    @NonNull
    @Override
    public VoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_voice, parent, false);
        return new VoiceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VoiceViewHolder h, int position) {
        String  path   = paths.get(position);
        boolean active = (position == playingIndex);

        // ── Label ─────────────────────────────────────────────────────────
        String label = "Voice memo " + (position + 1);
        try {
            File f = new File(path);
            if (f.exists()) label += "  ·  " + (f.length() / 1024) + " KB";
        } catch (Exception ignored) {}
        h.label.setText(label);

        // ── Play button icon ──────────────────────────────────────────────
        if (active && mediaPlayer != null && mediaPlayer.isPlaying()) {
            h.playBtn.setImageResource(R.drawable.ic_pause);
        } else {
            h.playBtn.setImageResource(R.drawable.ic_play);
        }

        // ── Seekbar row ───────────────────────────────────────────────────
        h.seekRow.setVisibility(active ? View.VISIBLE : View.GONE);
        if (active && mediaPlayer != null && isPrepared) {
            try {
                int dur = mediaPlayer.getDuration();
                int cur = mediaPlayer.getCurrentPosition();
                h.seekBar.setMax(dur > 0 ? dur : 1);
                h.seekBar.setProgress(cur);
                h.currentTime.setText(formatMs(cur));
                h.totalTime.setText(formatMs(dur > 0 ? dur : 0));
            } catch (Exception ignored) {}
        } else {
            h.seekBar.setMax(100);
            h.seekBar.setProgress(0);
            h.currentTime.setText("0:00");
            h.totalTime.setText("0:00");
        }

        // ── Play / Pause ──────────────────────────────────────────────────
        h.playBtn.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;

            if (playingIndex == pos) {
                // Same item → toggle pause / resume
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPaused = true;
                    stopSeekUpdates();
                    notifyItemChanged(pos);
                } else if (mediaPlayer != null) {
                    mediaPlayer.start();
                    isPaused = false;
                    startSeekUpdates();
                    notifyItemChanged(pos);
                }
            } else {
                // New item → stop previous, start fresh
                stopPlayback();
                playingIndex = pos;
                isPaused     = false;
                isPrepared   = false;
                notifyItemChanged(pos);
                startMediaPlayer(paths.get(pos));
            }
        });

        // ── Seekbar drag ──────────────────────────────────────────────────
        h.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    try {
                        mediaPlayer.seekTo(progress);
                        h.currentTime.setText(formatMs(progress));
                    } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { stopSeekUpdates(); }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) startSeekUpdates();
            }
        });

        // ── Delete ────────────────────────────────────────────────────────
        h.deleteBtn.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            if (pos == playingIndex) stopPlayback();
            if (deleteListener != null) deleteListener.onDelete(paths.get(pos), pos);
        });
    }

    @Override
    public int getItemCount() { return paths.size(); }

    // ── MediaPlayer ───────────────────────────────────────────────────────

    private void startMediaPlayer(String path) {
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        try {
            // Use FileDescriptor — avoids error -38 on app-private paths (Android 10+)
            FileInputStream fis = new FileInputStream(path);
            mediaPlayer.setDataSource(fis.getFD());
            fis.close();
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                notifyItemChanged(playingIndex);   // refresh seekbar max/icon
                startSeekUpdates();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                stopSeekUpdates();
                int finished = playingIndex;
                playingIndex = -1;
                isPaused     = false;
                isPrepared   = false;
                // DON'T release here — keep mp alive briefly so duration stays readable
                seekHandler.postDelayed(() -> {
                    releasePlayer();
                    if (finished >= 0 && finished < paths.size())
                        notifyItemChanged(finished);
                }, 1500);
                if (finished >= 0 && finished < paths.size())
                    notifyItemChanged(finished);   // reset icon immediately
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopPlayback(); return true;
            });
        } catch (IOException e) {
            releasePlayer();
            playingIndex = -1;
        }
    }

    /**
     * Seekbar ticker: uses notifyItemChanged so it always hits the *live* ViewHolder,
     * avoiding stale-reference crashes after scroll/recycle.
     */
    private void startSeekUpdates() {
        stopSeekUpdates();
        seekRunnable = new Runnable() {
            @Override public void run() {
                if (mediaPlayer != null && playingIndex >= 0) {
                    try {
                        if (mediaPlayer.isPlaying()) {
                            // Update only the active row without full rebind
                            updateSeekbarInPlace();
                            seekHandler.postDelayed(this, 250);
                        }
                    } catch (Exception ignored) {}
                }
            }
        };
        seekHandler.post(seekRunnable);
    }

    /** Directly writes to the live ViewHolder's seekbar without notifyItemChanged. */
    private void updateSeekbarInPlace() {
        if (recyclerView == null || playingIndex < 0 || mediaPlayer == null || !isPrepared) return;
        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(playingIndex);
        if (!(vh instanceof VoiceViewHolder)) return;
        VoiceViewHolder h = (VoiceViewHolder) vh;
        try {
            int pos = mediaPlayer.getCurrentPosition();
            int dur = mediaPlayer.getDuration();
            h.seekBar.setProgress(pos);
            h.currentTime.setText(formatMs(pos));
            if (h.seekBar.getMax() != dur && dur > 0) {
                h.seekBar.setMax(dur);
                h.totalTime.setText(formatMs(dur));
            }
        } catch (Exception ignored) {}
    }

    private void stopSeekUpdates() {
        if (seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
        }
    }

    /** Stop playback and reset state. Call from Fragment onPause/onDestroy. */
    public void stopPlayback() {
        stopSeekUpdates();
        int prev = playingIndex;
        playingIndex = -1;
        isPaused     = false;
        isPrepared   = false;
        releasePlayer();
        if (prev >= 0 && prev < paths.size()) notifyItemChanged(prev);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String formatMs(int ms) {
        long m = TimeUnit.MILLISECONDS.toMinutes(ms);
        long s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class VoiceViewHolder extends RecyclerView.ViewHolder {
        final TextView    label;
        final ImageButton playBtn;
        final ImageButton deleteBtn;
        final View        seekRow;
        final SeekBar     seekBar;
        final TextView    currentTime;
        final TextView    totalTime;

        VoiceViewHolder(@NonNull View v) {
            super(v);
            label       = v.findViewById(R.id.voiceLabel);
            playBtn     = v.findViewById(R.id.voiceItemPlayBtn);
            deleteBtn   = v.findViewById(R.id.voiceItemDeleteBtn);
            seekRow     = v.findViewById(R.id.seekRow);
            seekBar     = v.findViewById(R.id.voiceSeekBar);
            currentTime = v.findViewById(R.id.voiceCurrentTime);
            totalTime   = v.findViewById(R.id.voiceTotalTime);
        }
    }
}