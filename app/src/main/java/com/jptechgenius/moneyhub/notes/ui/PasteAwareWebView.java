package com.jptechgenius.moneyhub.notes.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * WebView that detects image pastes from Ctrl+V (keyboard)
 * and ensures touch events work properly for cursor placement.
 */
public class PasteAwareWebView extends WebView {

    public interface OnImagePasteListener {
        void onImagePasted(Uri imageUri);
    }

    private OnImagePasteListener imagePasteListener;

    public PasteAwareWebView(Context context) { super(context); }
    public PasteAwareWebView(Context context, AttributeSet attrs) { super(context, attrs); }
    public PasteAwareWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnImagePasteListener(OnImagePasteListener listener) {
        this.imagePasteListener = listener;
    }

    // ── Touch handling for cursor placement ──────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Tell parent (NestedScrollView) to not steal touch events
        // so the user can tap anywhere to place cursor
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // ── Ctrl+V on hardware / Chromebook keyboards ──────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.isCtrlPressed()
                && event.getKeyCode() == KeyEvent.KEYCODE_V) {
            if (checkClipboardForImage()) return true; // consumed
        }
        return super.dispatchKeyEvent(event);
    }

    // ── Public: called by the toolbar "Paste Image" button ─────────────────

    /**
     * Call this from the fragment when the user taps the "Paste Image" toolbar button.
     * @return true if an image was found in the clipboard and the listener fired.
     */
    public boolean checkClipboardForImage() {
        if (imagePasteListener == null) return false;

        ClipboardManager cm = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return false;

        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return false;

        // 1. Check MIME types declared on the ClipDescription
        ClipDescription desc = clip.getDescription();
        for (int i = 0; i < desc.getMimeTypeCount(); i++) {
            String mime = desc.getMimeType(i);
            if (mime != null && mime.startsWith("image/")) {
                Uri uri = clip.getItemAt(0).getUri();
                if (uri != null) { imagePasteListener.onImagePasted(uri); return true; }
            }
        }

        // 2. Fallback: resolve URI MIME type via ContentResolver
        Uri uri = clip.getItemAt(0).getUri();
        if (uri != null) {
            String type = getContext().getContentResolver().getType(uri);
            if (type != null && type.startsWith("image/")) {
                imagePasteListener.onImagePasted(uri);
                return true;
            }
        }

        return false;
    }
}