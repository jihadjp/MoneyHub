package com.jptechgenius.moneyhub.notes.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.jptechgenius.moneyhub.R;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Full-screen media viewer.
 *  • Images → ViewPager2 swipe + pinch-zoom + double-tap zoom
 *  • Videos → VideoView with custom controls; URI resolved correctly for
 *             both file paths and content:// URIs (fixes video glitch)
 */
public class MediaViewerDialog extends DialogFragment {

    private static final String ARG_PATHS    = "paths";
    private static final String ARG_START    = "start";
    private static final String ARG_IS_VIDEO = "is_video";

    // ── Factory methods ───────────────────────────────────────────────────

    public static MediaViewerDialog images(List<String> paths, int startIndex) {
        MediaViewerDialog d = new MediaViewerDialog();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PATHS, new java.util.ArrayList<>(paths));
        args.putInt(ARG_START, startIndex);
        args.putBoolean(ARG_IS_VIDEO, false);
        d.setArguments(args);
        return d;
    }

    public static MediaViewerDialog video(String path) {
        MediaViewerDialog d = new MediaViewerDialog();
        Bundle args = new Bundle();
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        list.add(path);
        args.putStringArrayList(ARG_PATHS, list);
        args.putInt(ARG_START, 0);
        args.putBoolean(ARG_IS_VIDEO, true);
        d.setArguments(args);
        return d;
    }

    // ── Style ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle b) {
        Dialog dialog = super.onCreateDialog(b);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        assert getArguments() != null;
        List<String> paths = getArguments().getStringArrayList(ARG_PATHS);
        int startIndex     = getArguments().getInt(ARG_START, 0);
        boolean isVideo    = getArguments().getBoolean(ARG_IS_VIDEO, false);
        return isVideo ? buildVideoView(paths.get(0)) : buildImageView(paths, startIndex);
    }

    // ══════════════════════════════════════════════════════════════════════
    // IMAGE GALLERY
    // ══════════════════════════════════════════════════════════════════════

    private View buildImageView(List<String> paths, int startIndex) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(0xFF000000);

        // ViewPager2
        ViewPager2 pager = new ViewPager2(requireContext());
        pager.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        pager.setAdapter(new ImagePagerAdapter(paths));
        pager.setCurrentItem(startIndex, false);
        root.addView(pager);

        // Counter "2 / 5"
        TextView counter = new TextView(requireContext());
        counter.setTextColor(0xFFFFFFFF);
        counter.setTextSize(14);
        counter.setBackgroundColor(0x88000000);
        counter.setPadding(24, 10, 24, 10);
        FrameLayout.LayoutParams counterLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        counterLp.topMargin = 56;
        root.addView(counter, counterLp);

        // Close button
        ImageButton close = makeCloseButton();
        root.addView(close);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                counter.setText((position + 1) + " / " + paths.size());
                counter.setVisibility(paths.size() <= 1 ? View.GONE : View.VISIBLE);
            }
        });
        counter.setText((startIndex + 1) + " / " + paths.size());
        counter.setVisibility(paths.size() <= 1 ? View.GONE : View.VISIBLE);

        close.setOnClickListener(v -> dismissWithAnim());
        return root;
    }

    // ViewPager2 adapter — each page is a ZoomableImageView
    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.PH> {
        private final List<String> paths;
        ImagePagerAdapter(List<String> p) { this.paths = p; }

        @NonNull @Override
        public PH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomableImageView iv = new ZoomableImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new PH(iv);
        }

        @Override public void onBindViewHolder(@NonNull PH h, int pos) {
            String p = paths.get(pos);
            Object src = p.startsWith("content://") ? Uri.parse(p) : new File(p);
            Glide.with(h.iv.getContext())
                    .load(src)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            // Reset matrix AFTER image is loaded so dimensions are known
                            h.iv.post(h.iv::resetMatrix);
                            return false;
                        }
                    })
                    .into(h.iv);
        }

        @Override public int getItemCount() { return paths.size(); }

        class PH extends RecyclerView.ViewHolder {
            final ZoomableImageView iv;
            PH(ZoomableImageView v) { super(v); iv = v; }
        }
    }

    // ── Zoomable ImageView ────────────────────────────────────────────────

    private static class ZoomableImageView extends AppCompatImageView {

        private final Matrix  matrix       = new Matrix();
        private final float[] matrixValues = new float[9];

        private static final float MIN_ZOOM = 1f;
        private static final float MAX_ZOOM = 5f;

        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector      gestureDetector;
        private float   lastX, lastY;

        public ZoomableImageView(android.content.Context ctx) {
            super(ctx);
            setScaleType(ScaleType.MATRIX);

            scaleDetector = new ScaleGestureDetector(ctx,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override public boolean onScale(ScaleGestureDetector d) {
                            float factor  = d.getScaleFactor();
                            float current = getScale();
                            float next    = current * factor;
                            if (next < MIN_ZOOM) factor = MIN_ZOOM / current;
                            if (next > MAX_ZOOM) factor = MAX_ZOOM / current;
                            matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                            clamp();
                            setImageMatrix(matrix);
                            return true;
                        }
                    });

            gestureDetector = new GestureDetector(ctx,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override public boolean onDoubleTap(MotionEvent e) {
                            if (getScale() > MIN_ZOOM + 0.1f) {
                                resetMatrix();
                            } else {
                                float factor = 2.5f / getScale();
                                matrix.postScale(factor, factor, e.getX(), e.getY());
                                clamp();
                            }
                            setImageMatrix(matrix);
                            return true;
                        }
                    });

            addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> resetMatrix());
        }

        void resetMatrix() {
            if (getDrawable() == null) return;
            int dw = getDrawable().getIntrinsicWidth();
            int dh = getDrawable().getIntrinsicHeight();
            int vw = getWidth(), vh = getHeight();
            if (dw <= 0 || dh <= 0 || vw <= 0 || vh <= 0) return;
            float scale = Math.min((float) vw / dw, (float) vh / dh);
            matrix.reset();
            matrix.setScale(scale, scale);
            matrix.postTranslate((vw - dw * scale) / 2f, (vh - dh * scale) / 2f);
            setImageMatrix(matrix);
        }

        private float getScale() {
            matrix.getValues(matrixValues);
            return matrixValues[Matrix.MSCALE_X];
        }

        private void clamp() {
            if (getDrawable() == null) return;
            matrix.getValues(matrixValues);
            float scale   = matrixValues[Matrix.MSCALE_X];
            float transX  = matrixValues[Matrix.MTRANS_X];
            float transY  = matrixValues[Matrix.MTRANS_Y];
            int dw = getDrawable().getIntrinsicWidth();
            int dh = getDrawable().getIntrinsicHeight();
            int vw = getWidth(), vh = getHeight();
            float sw = dw * scale, sh = dh * scale;

            float minX = sw > vw ? vw - sw : (vw - sw) / 2f;
            float maxX = sw > vw ? 0        : (vw - sw) / 2f;
            float minY = sh > vh ? vh - sh  : (vh - sh) / 2f;
            float maxY = sh > vh ? 0        : (vh - sh) / 2f;

            matrixValues[Matrix.MTRANS_X] = Math.max(minX, Math.min(maxX, transX));
            matrixValues[Matrix.MTRANS_Y] = Math.max(minY, Math.min(maxY, transY));
            matrix.setValues(matrixValues);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            scaleDetector.onTouchEvent(e);
            gestureDetector.onTouchEvent(e);

            boolean zoomed = getScale() > MIN_ZOOM + 0.05f;

            // Prevent ViewPager2 from stealing touch events when zoomed in
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(
                        zoomed || scaleDetector.isInProgress());
            }

            if (!scaleDetector.isInProgress()) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = e.getX();
                        lastY = e.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (zoomed) {
                            float dx = e.getX() - lastX;
                            float dy = e.getY() - lastY;
                            matrix.postTranslate(dx, dy);
                            clamp();
                            setImageMatrix(matrix);
                        }
                        lastX = e.getX();
                        lastY = e.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // If back to min zoom, re-allow ViewPager2 swiping
                        if (!zoomed && getParent() != null)
                            getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
            }
            return true;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // VIDEO PLAYER
    // ══════════════════════════════════════════════════════════════════════

    private VideoView videoView;
    private final Handler  progressHandler  = new Handler(Looper.getMainLooper());
    private Runnable       progressRunnable = null;

    private View buildVideoView(String path) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(0xFF000000);

        // VideoView (centered)
        videoView = new VideoView(requireContext());
        FrameLayout.LayoutParams videoLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        root.addView(videoView, videoLp);

        // Control overlay at bottom
        LinearLayout controls = new LinearLayout(requireContext());
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER_HORIZONTAL);
        controls.setBackgroundColor(0xCC000000);
        controls.setPadding(32, 16, 32, 32);
        FrameLayout.LayoutParams ctrlLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        root.addView(controls, ctrlLp);

        // Time row: [0:00] [seekbar] [3:45]
        LinearLayout timeRow = new LinearLayout(requireContext());
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView currentTime = makeTimeText("0:00");
        SeekBar  seekBar     = new SeekBar(requireContext());
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView totalTime = makeTimeText("0:00");
        timeRow.addView(currentTime);
        timeRow.addView(seekBar);
        timeRow.addView(totalTime);
        controls.addView(timeRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Play/Pause button
        ImageButton playPauseBtn = new ImageButton(requireContext());
        playPauseBtn.setImageResource(R.drawable.ic_play);
        playPauseBtn.setBackgroundColor(0);
        playPauseBtn.setColorFilter(0xFFFFFFFF);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                dpToPx(56), dpToPx(56));
        btnLp.gravity = Gravity.CENTER_HORIZONTAL;
        btnLp.topMargin = dpToPx(4);
        controls.addView(playPauseBtn, btnLp);

        // Close button
        root.addView(makeCloseButton());

        // ── Resolve URI correctly ─────────────────────────────────────────
        // Uri.parse() on a bare file path returns an opaque URI; VideoView
        // needs a properly-formed file:// or content:// URI.
        Uri videoUri;
        if (path.startsWith("content://") || path.startsWith("file://")) {
            videoUri = Uri.parse(path);
        } else {
            videoUri = Uri.fromFile(new File(path));  // /storage/... → file:///storage/...
        }
        videoView.setVideoURI(videoUri);

        // ── Wire video ────────────────────────────────────────────────────
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            // Request exact rendering size to avoid glitch on some codecs
            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);

            int dur = mp.getDuration();
            totalTime.setText(formatMs(dur > 0 ? dur : 0));
            seekBar.setMax(dur > 0 ? dur : 1);
            videoView.start();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            startProgressUpdates(seekBar, currentTime);
        });

        videoView.setOnCompletionListener(mp -> {
            stopProgressUpdates();
            playPauseBtn.setImageResource(R.drawable.ic_play);
            seekBar.setProgress(0);
            currentTime.setText("0:00");
        });

        // ── Play / Pause ──────────────────────────────────────────────────
        playPauseBtn.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                stopProgressUpdates();
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                videoView.start();
                playPauseBtn.setImageResource(R.drawable.ic_pause);
                startProgressUpdates(seekBar, currentTime);
            }
        });

        // ── Seekbar ───────────────────────────────────────────────────────
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    currentTime.setText(formatMs(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { stopProgressUpdates(); }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (videoView.isPlaying()) startProgressUpdates(seekBar, currentTime);
            }
        });

        // ── Close ─────────────────────────────────────────────────────────
        root.getChildAt(root.getChildCount() - 1).setOnClickListener(v -> {
            videoView.stopPlayback();
            dismissWithAnim();
        });

        return root;
    }

    private void startProgressUpdates(SeekBar seekBar, TextView currentTime) {
        stopProgressUpdates();
        progressRunnable = new Runnable() {
            @Override public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    try {
                        int pos = videoView.getCurrentPosition();
                        seekBar.setProgress(pos);
                        currentTime.setText(formatMs(pos));
                    } catch (Exception ignored) {}
                    progressHandler.postDelayed(this, 300);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ImageButton makeCloseButton() {
        ImageButton btn = new ImageButton(requireContext());
        btn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btn.setBackgroundColor(0x88000000);
        btn.setColorFilter(0xFFFFFFFF);
        btn.setPadding(16, 16, 16, 16);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dpToPx(48), dpToPx(48), Gravity.TOP | Gravity.END);
        lp.topMargin   = dpToPx(48);
        lp.rightMargin = dpToPx(16);
        btn.setLayoutParams(lp);
        return btn;
    }

    private TextView makeTimeText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(12);
        tv.setPadding(8, 0, 8, 0);
        return tv;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String formatMs(int ms) {
        long m = TimeUnit.MILLISECONDS.toMinutes(ms);
        long s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    private void dismissWithAnim() {
        View v = getView();
        if (v == null) { dismiss(); return; }
        v.animate().alpha(0f).setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) { dismiss(); }
                }).start();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void onStart() {
        super.onStart();
        View v = getView();
        if (v != null) { v.setAlpha(0f); v.animate().alpha(1f).setDuration(250).start(); }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopProgressUpdates();
        if (videoView != null) videoView.stopPlayback();
    }
}