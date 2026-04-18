package com.jptechgenius.moneyhub.notes.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Wraps MediaRecorder and MediaPlayer for in-note voice memos.
 * Always call {@link #release()} in Activity/Fragment onDestroy.
 */
public class VoiceNoteHelper {

    private static final String TAG = "VoiceNoteHelper";

    public interface PlaybackCallback {
        void onStarted();
        void onCompleted();
    }

    private final Context context;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private String currentOutputPath;

    public VoiceNoteHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Starts recording. Returns the output file path, or null on failure.
     * The file is saved in the app's private files/voice_notes directory.
     */
    public String startRecording() {
        File dir = new File(context.getFilesDir(), "voice_notes");
        if (!dir.exists()) dir.mkdirs();

        currentOutputPath = new File(dir, "voice_" + System.currentTimeMillis() + ".m4a")
            .getAbsolutePath();

        recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? new MediaRecorder(context)
            : new MediaRecorder();

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(currentOutputPath);
            recorder.prepare();
            recorder.start();
            return currentOutputPath;
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Failed to start recording", e);
            releaseRecorder();
            return null;
        }
    }

    /**
     * Stops an active recording. Safe to call even if recording never started.
     */
    public void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "Recorder stop error (no data written?)", e);
                // Delete the empty file
                if (currentOutputPath != null) new File(currentOutputPath).delete();
                currentOutputPath = null;
            } finally {
                releaseRecorder();
            }
        }
    }

    /**
     * Plays back a voice memo at the given file path.
     *
     * @param path     absolute path to the .m4a file
//     * @param callback optional start/complete callbacks
     */
    public void play(String path, Runnable onStart, Runnable onComplete) {
        if (path == null || path.isEmpty()) return;
        releasePlayer();

        player = new MediaPlayer();
        try {
            player.setDataSource(path);
            player.setOnPreparedListener(mp -> {
                mp.start();
                if (onStart != null) onStart.run();
            });
            player.setOnCompletionListener(mp -> {
                releasePlayer();
                if (onComplete != null) onComplete.run();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what);
                releasePlayer();
                return true;
            });
            player.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Playback failed", e);
            releasePlayer();
        }
    }

    /** Stops playback if currently playing. */
    public void stopPlayback() {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        releasePlayer();
    }

    /** Releases all resources. Must be called in onDestroy. */
    public void release() {
        stopRecording();
        releasePlayer();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void releaseRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
