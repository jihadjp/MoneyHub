package com.jptechgenius.moneyhub.notes.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.jptechgenius.moneyhub.R;
import com.jptechgenius.moneyhub.dialogfragments.OcrLoadingDialog;
import com.jptechgenius.moneyhub.dialogfragments.PdfExportLoadingDialog;
import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;
import com.jptechgenius.moneyhub.notes.utils.NoteColorPicker;
import com.jptechgenius.moneyhub.notes.utils.PdfExportHelper;
import com.jptechgenius.moneyhub.notes.utils.VoiceNoteHelper;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoteEditorFragment extends Fragment {

    public static final String ARG_NOTE = "arg_note";

    private NotesViewModel viewModel;
    private Note currentNote;
    private boolean isNewNote;

    // ── Auto-save ─────────────────────────────────────────────────────────
    private static final long AUTO_SAVE_DELAY_MS = 3_000L;
    private final Handler     autoSaveHandler  = new Handler(Looper.getMainLooper());
    private final Runnable    autoSaveRunnable = this::performAutoSave;
    private TextView          autoSaveIndicator;  // subtle "Saved" label in toolbar

    // ── Views ──────────────────────────────────────────────────────────────
    private EditText           titleEditText;
    private PasteAwareWebView  contentWebView;
    private HorizontalScrollView formattingToolbar;
    private androidx.core.widget.NestedScrollView nestedScrollView;
    private RecyclerView       imagesRecyclerView;
    private RecyclerView       videosRecyclerView;
    private RecyclerView       voicesRecyclerView;
    private ChipGroup          labelsChipGroup;
    private View               colorIndicator;

    // ── Media toggle ──────────────────────────────────────────────────────
    private View               mediaToggleBar;
    private TextView mediaToggleText;
    private android.widget.ImageView mediaToggleArrow;
    private View               mediaPanel;
    private boolean            mediaPanelExpanded = false;

    // ── Media lists ────────────────────────────────────────────────────────
    private final List<String> imagePaths = new ArrayList<>();
    private final List<String> videoPaths = new ArrayList<>();
    private final List<String> voicePaths = new ArrayList<>();

    private NoteImagesAdapter imagesAdapter;
    private NoteVideosAdapter videosAdapter;
    private NoteVoicesAdapter voicesAdapter;

    // ── Voice recording ────────────────────────────────────────────────────
    private VoiceNoteHelper voiceHelper;
    private boolean isRecording    = false;

    // ── OCR ───────────────────────────────────────────────────────────────
    private Uri    ocrPhotoUri;           // temp camera capture URI
    private int    selectedOcrLang = 0;   // 0=Latin, persisted per session

    /**
     * Language entries for OCR.
     * Names shown in the picker; the recognizer is selected in getOcrRecognizer().
     */
    private static final String[] OCR_LANG_NAMES = {
            "🌍 English / Latin",
            "🇮🇳 Hindi / Devanagari",
            "🇧🇩 Bangla / Devanagari",
            "🇨🇳 Chinese",
            "🇯🇵 Japanese",
            "🇰🇷 Korean"
    };

    // ── Launchers ─────────────────────────────────────────────────���────────

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    forEachUri(result.getData(), uri -> {
                        String s = persistUri(uri);
                        if (s != null) imagePaths.add(s);
                    });
                    if (imagesAdapter != null) imagesAdapter.notifyDataSetChanged();
                    syncVisibility(imagesRecyclerView, imagePaths);
                    scheduleAutoSave();
                }
            });

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    forEachUri(result.getData(), uri -> {
                        String s = persistUri(uri);
                        if (s != null) videoPaths.add(s);
                    });
                    if (videosAdapter != null) videosAdapter.notifyDataSetChanged();
                    syncVisibility(videosRecyclerView, videoPaths);
                    scheduleAutoSave();
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (!granted.containsValue(false)) doStartRecording();
                else {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // ── OCR launchers ─────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) showOcrSourceChooser();
                else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> ocrCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && ocrPhotoUri != null) {
                    showOcrLanguagePickerThenProcess(ocrPhotoUri);
                }
            });

    private final ActivityResultLauncher<Intent> ocrGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    showOcrLanguagePickerThenProcess(result.getData().getData());
                }
            });

    // ── Factory ────────────────────────────────────────────────────────────

    public static NoteEditorFragment newInstance() { return new NoteEditorFragment(); }

    public static NoteEditorFragment newInstance(Note note) {
        NoteEditorFragment f = new NoteEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_NOTE, note);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel   = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
        voiceHelper = new VoiceNoteHelper(requireContext());
        if (getArguments() != null) currentNote = getArguments().getParcelable(ARG_NOTE);
        isNewNote = (currentNote == null);
        if (isNewNote) currentNote = new Note();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_note_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar(view);
        bindViews(view);
        setupWebView();
        setupFormattingToolbar();
        setupImagesRecyclerView();
        setupVideosRecyclerView();
        setupVoicesRecyclerView();
        setupColorIndicator();
        populateFields();
        observeLabels();
        setupKeyboardInset(view);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) stopVoiceRecording();
        stopAnyVoicePlayback();
        // Flush any pending auto-save immediately on pause
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        performAutoSave();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        voiceHelper.release();
    }

    // ── Toolbar ────────────────────────────────────────────────────────────

    private void setupToolbar(View root) {
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        Activity activity = getActivity();
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            appCompatActivity.setSupportActionBar(toolbar);
            if (appCompatActivity.getSupportActionBar() != null) {
                appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                appCompatActivity.getSupportActionBar().setTitle("");
            }
        }
        toolbar.setNavigationOnClickListener(v -> saveAndExit());

        // "Saved" indicator – small text injected into toolbar
        autoSaveIndicator = new TextView(requireContext());
        autoSaveIndicator.setTextSize(11f);
        autoSaveIndicator.setTextColor(0xFF9E9E9E);
        autoSaveIndicator.setAlpha(0f);
        Toolbar.LayoutParams lp = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
        lp.rightMargin = 56; // leave room for colorIndicator
        autoSaveIndicator.setLayoutParams(lp);
        autoSaveIndicator.setText("Saved");
        toolbar.addView(autoSaveIndicator);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.note_editor_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if      (id == R.id.action_save)          { saveNote();        return true; }
        else if (id == R.id.action_export_pdf)    { exportToPdf();     return true; }
        else if (id == R.id.action_pick_color)    { showColorPicker(); return true; }
        else if (id == R.id.action_manage_labels) { showLabelPicker(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── View binding ───────────────────────────────────────────────────────

    private void bindViews(View root) {
        titleEditText      = root.findViewById(R.id.titleEditText);
        contentWebView     = root.findViewById(R.id.contentWebView);
        formattingToolbar  = root.findViewById(R.id.formattingToolbar);
        nestedScrollView   = root.findViewById(R.id.nestedScrollView);
        imagesRecyclerView = root.findViewById(R.id.imagesRecyclerView);
        videosRecyclerView = root.findViewById(R.id.videosRecyclerView);
        voicesRecyclerView = root.findViewById(R.id.voicesRecyclerView);
        labelsChipGroup    = root.findViewById(R.id.labelsChipGroup);
        colorIndicator     = root.findViewById(R.id.colorIndicator);

        // Media toggle
        mediaToggleBar     = root.findViewById(R.id.mediaToggleBar);
        mediaToggleText    = root.findViewById(R.id.mediaToggleText);
        mediaToggleArrow   = root.findViewById(R.id.mediaToggleArrow);
        mediaPanel         = root.findViewById(R.id.mediaPanel);

        if (mediaToggleBar != null) {
            mediaToggleBar.setOnClickListener(v -> toggleMediaPanel());
        }
    }

    // ── WebView ────────────────────────────────────────────────────────────

    private void setupWebView() {
        WebSettings s = contentWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDefaultFontSize(16);

        // JS bridge so the WebView can notify us of content changes → auto-save
        contentWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onContentChanged() {
                new Handler(Looper.getMainLooper()).post(() -> scheduleAutoSave());
            }

            @JavascriptInterface
            public void onLinkClicked(String url) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded() || url == null || url.isEmpty()) return;
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Cannot open link", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @JavascriptInterface
            public void onFormatStateChanged(String json) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded() || formattingToolbar == null) return;
                    try {
                        JSONObject s = new JSONObject(json);
                        setFmtBtnSelected(R.id.btnBold,          s.optBoolean("bold"));
                        setFmtBtnSelected(R.id.btnItalic,        s.optBoolean("italic"));
                        setFmtBtnSelected(R.id.btnUnderline,     s.optBoolean("underline"));
                        setFmtBtnSelected(R.id.btnStrikethrough, s.optBoolean("strikeThrough"));
                        setFmtBtnSelected(R.id.btnBulletList,    s.optBoolean("unorderedList"));
                        setFmtBtnSelected(R.id.btnOrderedList,   s.optBoolean("orderedList"));
                        setFmtBtnSelected(R.id.btnCheckbox,      s.optBoolean("checkbox"));
                    } catch (Exception ignored) {}
                });
            }
        }, "Android");

        contentWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Don't let the editor navigate away — open links in browser
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {}
                    return true;
                }
                return url != null && !url.startsWith("file:///android_asset/");
            }

            @Override public void onPageFinished(WebView view, String url) {
                // Inject content
                String html    = currentNote.getContent();
                if (html == null) html = "";
                String escaped = html.replace("`", "\\`");
                contentWebView.evaluateJavascript(
                        "document.getElementById('editor').innerHTML = `" + escaped + "`;", null);

                // Rebind checkbox listeners for saved to-do items
                contentWebView.evaluateJavascript("setTimeout(rebindCheckboxes, 200);", null);

                // Linkify any plain-text URLs from older notes
                contentWebView.evaluateJavascript("setTimeout(autoLinkify, 300);", null);

                // Inject auto-save trigger on every editor input
                contentWebView.evaluateJavascript(
                        "document.getElementById('editor').addEventListener('input', function(){" +
                                "  if(window.Android) Android.onContentChanged();" +
                                "});", null);
            }
        });

        // Clipboard image paste support
        contentWebView.setOnImagePasteListener(uri -> {
            String path = persistUri(uri);
            if (path != null) {
                currentRecordingPath = path;
                imagePaths.add(path);
                if (imagesAdapter != null) imagesAdapter.notifyDataSetChanged();
                syncVisibility(imagesRecyclerView, imagePaths);
                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, "Image added from clipboard", Toast.LENGTH_SHORT).show();
                }
                scheduleAutoSave();
            }
        });

        contentWebView.loadUrl("file:///android_asset/note_editor.html");

        // Title auto-save
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { scheduleAutoSave(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void execCommand(String cmd, String val) {
        String js = val != null
                ? "document.execCommand('" + cmd + "', false, '" + val + "');"
                : "document.execCommand('" + cmd + "', false, null);";
        contentWebView.evaluateJavascript(js, null);
        contentWebView.requestFocus();
    }

    private void getEditorContent(ContentCallback cb) {
        contentWebView.evaluateJavascript(
                "document.getElementById('editor').innerHTML;",
                value -> {
                    if (value != null && value.length() >= 2 && value.startsWith("\"")) {
                        value = value.substring(1, value.length() - 1)
                                .replace("\\u003C", "<").replace("\\u003E", ">")
                                .replace("\\\"", "\"").replace("\\/", "/");
                    }
                    cb.onContent(value);
                });
    }

    private interface ContentCallback { void onContent(String html); }

    // ── Auto-save ──────────────────────────────────────────────────────────

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY_MS);
    }

    private void performAutoSave() {
        if (titleEditText == null || contentWebView == null) return;
        getEditorContent(html -> new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            String title = titleEditText.getText().toString().trim();
            // Only save if the note has any content
            if (title.isEmpty() && (html == null || html.trim().isEmpty())
                    && imagePaths.isEmpty() && videoPaths.isEmpty() && voicePaths.isEmpty()) return;

            currentNote.setTitle(title);
            currentNote.setContent(html);
            currentNote.setImagePaths(new ArrayList<>(imagePaths));
            currentNote.setVideoPaths(new ArrayList<>(videoPaths));
            currentNote.setVoicePaths(new ArrayList<>(voicePaths));
            currentNote.setTimestamp(System.currentTimeMillis());

            viewModel.saveNote(currentNote, id -> {
                if (!isAdded()) return;
                if (id > 0 && isNewNote) { currentNote.setId(id); isNewNote = false; }
                showSavedIndicator();
            });
        }));
    }

    private void showSavedIndicator() {
        if (!isAdded() || autoSaveIndicator == null) return;
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (autoSaveIndicator != null) {
                    autoSaveIndicator.animate().cancel();
                    autoSaveIndicator.setAlpha(1f);
                    autoSaveIndicator.animate()
                            .alpha(0f).setDuration(1200).setStartDelay(800).start();
                }
            });
        }
    }

    // ── Formatting toolbar ─────────────────────────────────────────────────

    private void setupFormattingToolbar() {
        setupFmtBtn(R.id.btnBold,          () -> execCommand("bold", null));
        setupFmtBtn(R.id.btnItalic,        () -> execCommand("italic", null));
        setupFmtBtn(R.id.btnUnderline,     () -> execCommand("underline", null));
        setupFmtBtn(R.id.btnStrikethrough, () -> execCommand("strikeThrough", null));
        setupFmtBtn(R.id.btnBulletList,    () -> execCommand("insertUnorderedList", null));
        setupFmtBtn(R.id.btnOrderedList,   () -> execCommand("insertOrderedList", null));
        setupFmtBtn(R.id.btnCheckbox,      () -> contentWebView.evaluateJavascript("toggleCheckbox();", null));
        setupFmtBtn(R.id.btnTextColor,     this::showTextColorPicker);
        setupFmtBtn(R.id.btnHighlight,     this::showHighlightColorPicker);
        setupFmtBtn(R.id.btnAddImage,      this::launchImagePicker);
        setupFmtBtn(R.id.btnPasteImage,    this::pasteImageFromClipboard);
        setupFmtBtn(R.id.btnAddVideo,      this::launchVideoPicker);
        setupFmtBtn(R.id.btnAddVoice,      this::requestMicAndRecord);
        setupFmtBtn(R.id.btnOcrCamera,     this::requestCameraAndOcr);
        setupFmtBtn(R.id.btnInsertLink,    this::showInsertLinkDialog);
    }

    private void setupFmtBtn(int id, Runnable action) {
        View btn = formattingToolbar.findViewById(id);
        if (btn != null) btn.setOnClickListener(v -> action.run());
    }

    private void setFmtBtnSelected(int id, boolean selected) {
        View btn = formattingToolbar.findViewById(id);
        if (btn != null) btn.setSelected(selected);
    }

    private void showTextColorPicker() {
        String[] colors = {"#000000","#FF1744","#2979FF","#00C853","#FF6D00","#AA00FF"};
        String[] names  = {"Black","Red","Blue","Green","Orange","Purple"};
        new AlertDialog.Builder(requireContext()).setTitle("Text Color")
                .setItems(names, (d, i) -> execCommand("foreColor", colors[i])).show();
    }

    private void showHighlightColorPicker() {
        String[] colors = {"#FFFF00","#69FF47","#18FFFF","#FF6EC7","transparent"};
        String[] names  = {"Yellow","Green","Cyan","Pink","Remove"};
        new AlertDialog.Builder(requireContext()).setTitle("Highlight")
                .setItems(names, (d, i) -> {
                    if ("transparent".equals(colors[i])) execCommand("removeFormat", null);
                    else execCommand("hiliteColor", colors[i]);
                }).show();
    }

    private void showInsertLinkDialog() {
        // Build a simple two-field dialog: URL + display text
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);


        EditText textInput = new EditText(requireContext());
        textInput.setHint("Display text (optional)");
        textInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        textInput.setSingleLine(true);
        layout.addView(textInput);

        EditText urlInput = new EditText(requireContext());
        urlInput.setHint("https://example.com");
        urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setSingleLine(true);
        layout.addView(urlInput);

        // Check clipboard for URL and pre-fill
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
                if (clip != null) {
                    String clipStr = clip.toString().trim();
                    if (clipStr.startsWith("http://") || clipStr.startsWith("https://")
                            || clipStr.startsWith("www.")) {
                        urlInput.setText(clipStr);
                        urlInput.selectAll();
                    }
                }
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(requireContext())
                .setTitle("🔗 Insert Link")
                .setView(layout)
                .setPositiveButton("Insert", (d, w) -> {
                    String url = urlInput.getText().toString().trim();
                    if (url.isEmpty()) return;
                    String text = textInput.getText().toString().trim();
                    // Escape for JS
                    String escapedUrl = url.replace("\\", "\\\\").replace("'", "\\'");
                    String escapedText = text.isEmpty() ? "" : text.replace("\\", "\\\\").replace("'", "\\'");
                    contentWebView.evaluateJavascript(
                            "insertLink('" + escapedUrl + "', '" + escapedText + "');", null);
                    contentWebView.requestFocus();
                    scheduleAutoSave();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Images ─────────────────────────────────────────────────────────────

    private void setupImagesRecyclerView() {
        imagesAdapter = new NoteImagesAdapter(imagePaths, path -> {
            imagePaths.remove(path);
            imagesAdapter.notifyDataSetChanged();
            syncVisibility(imagesRecyclerView, imagePaths);
            scheduleAutoSave();
        });
        // Premium full-screen preview — swipeable gallery
        imagesAdapter.setOnImageClickListener((path, pos) ->
                MediaViewerDialog.images(imagePaths, pos)
                        .show(getParentFragmentManager(), "img_viewer"));

        imagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        imagesRecyclerView.setAdapter(imagesAdapter);
        syncVisibility(imagesRecyclerView, imagePaths);
    }

    private void launchImagePicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(i);
    }

    private void pasteImageFromClipboard() {
        if (!contentWebView.checkClipboardForImage()) {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "No image found in clipboard", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── Videos ─────────────────────────────────────────────────────────────

    private void setupVideosRecyclerView() {
        videosAdapter = new NoteVideosAdapter(videoPaths, path -> {
            videoPaths.remove(path);
            videosAdapter.notifyDataSetChanged();
            syncVisibility(videosRecyclerView, videoPaths);
            scheduleAutoSave();
        });
        // Premium full-screen player
        videosAdapter.setOnVideoClickListener(path ->
                MediaViewerDialog.video(path)
                        .show(getParentFragmentManager(), "vid_player"));

        videosRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        videosRecyclerView.setAdapter(videosAdapter);
        syncVisibility(videosRecyclerView, videoPaths);
    }

    private void launchVideoPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("video/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(i);
    }

    // ── Voice recordings ────────────────────────────────────────────────────

    private void setupVoicesRecyclerView() {
        voicesAdapter = new NoteVoicesAdapter(voicePaths);

        voicesAdapter.setOnDeleteClickListener((path, pos) ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Remove voice memo?")
                        .setMessage("The recording will be permanently deleted.")
                        .setPositiveButton("Remove", (d, w) -> {
                            stopAnyVoicePlayback();
                            deleteVoiceFile(path);
                            voicePaths.remove(pos);
                            voicesAdapter.notifyItemRemoved(pos);
                            syncVisibility(voicesRecyclerView, voicePaths);
                            scheduleAutoSave();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());

        voicesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        voicesRecyclerView.setAdapter(voicesAdapter);
        syncVisibility(voicesRecyclerView, voicePaths);
    }

    private void requestMicAndRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
        }
    }

    private void startVoiceRecording() {
        if (isRecording) {
            stopVoiceRecording();   // second tap = stop
        } else {
            doStartRecording();
        }
    }

    private void doStartRecording() {
        stopAnyVoicePlayback();
        String path = voiceHelper.startRecording();
        if (path != null) {
            currentRecordingPath = path;
            isRecording = true;
            // Update toolbar mic button to show stop state
            View btn = formattingToolbar.findViewById(R.id.btnAddVoice);
            if (btn instanceof ImageButton)
                ((ImageButton) btn).setImageResource(R.drawable.ic_stop);
            Toast.makeText(requireContext(), "Recording… tap 🎤 again to stop",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceRecording() {
        voiceHelper.stopRecording();
        isRecording = false;

        // Restore mic icon
        View btn = formattingToolbar.findViewById(R.id.btnAddVoice);
        if (btn instanceof ImageButton)
            ((ImageButton) btn).setImageResource(R.drawable.ic_mic);

        if (currentRecordingPath != null) {
            voicePaths.add(currentRecordingPath);
            currentRecordingPath = null;
            voicesAdapter.notifyItemInserted(voicePaths.size() - 1);
            syncVisibility(voicesRecyclerView, voicePaths);
            scheduleAutoSave();
            Toast.makeText(requireContext(), "Voice memo added", Toast.LENGTH_SHORT).show();
        }
    }

    private String currentRecordingPath = null;  // tracks the path of the in-progress recording

    private void stopAnyVoicePlayback() {
        if (voicesAdapter != null) voicesAdapter.stopPlayback();
    }

    private void deleteVoiceFile(String path) {
        try { File f = new File(path); if (f.exists()) f.delete(); }
        catch (Exception ignored) {}
    }

    // ── Color ──────────────────────────────────────────────────────────────

    private void setupColorIndicator() {
        colorIndicator.setOnClickListener(v -> showColorPicker());
        updateColorIndicator();
    }

    private void updateColorIndicator() {
        colorIndicator.setBackgroundColor(
                currentNote.getColor() != -1 ? currentNote.getColor() : Color.TRANSPARENT);
    }

    private void showColorPicker() {
        Activity activity = getActivity();
        if (activity != null) {
            NoteColorPicker.show(activity, currentNote.getColor(), color -> {
                currentNote.setColor(color);
                updateColorIndicator();
                String bg = color != -1
                        ? "'" + String.format("#%06X", 0xFFFFFF & color) + "'"
                        : "''";
                contentWebView.evaluateJavascript(
                        "document.body.style.backgroundColor = " + bg + ";", null);
                scheduleAutoSave();
            });
        }
    }

    // ── Labels ─────────────────────────────────────────────────────────────

    private void observeLabels() {
        viewModel.getLabels().observe(getViewLifecycleOwner(), this::rebuildLabelChips);
    }

    private void rebuildLabelChips(List<Label> allLabels) {
        labelsChipGroup.removeAllViews();
        for (Label label : allLabels) {
            Chip chip = new Chip(requireContext());
            chip.setText(label.getName());
            chip.setCheckable(true);
            chip.setChecked(currentNote.getLabelIds().contains(label.getId()));
            chip.setOnCheckedChangeListener((btn, checked) -> {
                List<Long> ids = currentNote.getLabelIds();
                if (checked) { if (!ids.contains(label.getId())) ids.add(label.getId()); }
                else ids.remove(label.getId());
                currentNote.setLabelIds(ids);
                scheduleAutoSave();
            });
            labelsChipGroup.addView(chip);
        }
    }

    private void showLabelPicker() {
        labelsChipGroup.setVisibility(
                labelsChipGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    // ── Populate ───────────────────────────────────────────────────────────

    private void populateFields() {
        titleEditText.setText(currentNote.getTitle());

        imagePaths.clear();
        if (currentNote.getImagePaths() != null) imagePaths.addAll(currentNote.getImagePaths());
        if (imagesAdapter != null) imagesAdapter.notifyDataSetChanged();
        syncVisibility(imagesRecyclerView, imagePaths);

        videoPaths.clear();
        if (currentNote.getVideoPaths() != null) videoPaths.addAll(currentNote.getVideoPaths());
        if (videosAdapter != null) videosAdapter.notifyDataSetChanged();
        syncVisibility(videosRecyclerView, videoPaths);

        voicePaths.clear();
        if (currentNote.getVoicePaths() != null) voicePaths.addAll(currentNote.getVoicePaths());
        if (voicesAdapter != null) voicesAdapter.notifyDataSetChanged();
        syncVisibility(voicesRecyclerView, voicePaths);
    }

    // ── Save ───────────────────────────────────────────────────────────────

    private void saveNote() {
        getEditorContent(html -> new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            currentNote.setTitle(titleEditText.getText().toString().trim());
            currentNote.setContent(html);
            currentNote.setImagePaths(new ArrayList<>(imagePaths));
            currentNote.setVideoPaths(new ArrayList<>(videoPaths));
            currentNote.setVoicePaths(new ArrayList<>(voicePaths));
            currentNote.setTimestamp(System.currentTimeMillis());
            viewModel.saveNote(currentNote, id -> {
                if (!isAdded()) return;
                boolean wasNew = isNewNote;
                if (id > 0 && isNewNote) { currentNote.setId(id); isNewNote = false; }
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity,
                                    wasNew ? "Note created" : "Note updated",
                                    Toast.LENGTH_SHORT).show());
                }
            });
        }));
    }

    private void saveAndExit() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        getEditorContent(html -> new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            String title = titleEditText.getText().toString().trim();
            if (!title.isEmpty() || (html != null && !html.trim().isEmpty())
                    || !imagePaths.isEmpty() || !videoPaths.isEmpty() || !voicePaths.isEmpty()) {
                currentNote.setTitle(title);
                currentNote.setContent(html);
                currentNote.setImagePaths(new ArrayList<>(imagePaths));
                currentNote.setVideoPaths(new ArrayList<>(videoPaths));
                currentNote.setVoicePaths(new ArrayList<>(voicePaths));
                currentNote.setTimestamp(System.currentTimeMillis());
                viewModel.saveNote(currentNote, null);
            }
            Activity activity = getActivity();
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).getSupportFragmentManager().popBackStack();
            }
        }));
    }

    // ── PDF export ─────────────────────────────────────────────────────────

    private void exportToPdf() {
        getEditorContent(html -> new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            currentNote.setTitle(titleEditText.getText().toString().trim());
            currentNote.setContent(html);
            currentNote.setImagePaths(new ArrayList<>(imagePaths));
            Context context = getContext();
            if (context == null) return;

            // Show Lottie loading dialog
            PdfExportLoadingDialog loadingDialog = PdfExportLoadingDialog.newInstance();
            try {
                loadingDialog.show(getParentFragmentManager(), "PDF_LOADING");
            } catch (IllegalStateException ignored) {}

            new PdfExportHelper(context).export(currentNote, pdfFile -> {
                if (!isAdded()) return;

                // Dismiss loading dialog
                try { loadingDialog.dismissAllowingStateLoss(); }
                catch (Exception ignored) {}

                if (pdfFile != null) {
                    try {
                        Bundle bundle = new Bundle();
                        bundle.putString(PDFViewerFragment.ARG_PDF_PATH, pdfFile.getAbsolutePath());
                        bundle.putIntArray(PDFViewerFragment.ARG_PAGES, null);
                        bundle.putString(PDFViewerFragment.ARG_TITLE, currentNote.getTitle());
                        bundle.putBoolean(PDFViewerFragment.ARG_IS_LOCAL, true);

                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_noteEditor_to_pdfViewer, bundle);

                    } catch (IllegalStateException ignored) {}
                } else {
                    Context ctx = getContext();
                    if (ctx != null) Toast.makeText(ctx, "PDF export failed", Toast.LENGTH_SHORT).show();
                }
            });
        }));
    }

    // ── OCR — Image to Text ─────────────────────────────────────────────────

    private void requestCameraAndOcr() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showOcrSourceChooser();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showOcrSourceChooser() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Scan Text from Image")
                .setItems(new String[]{"📸  Take Photo", "🖼️  Pick from Gallery"}, (d, which) -> {
                    if (which == 0) launchOcrCamera();
                    else            launchOcrGallery();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchOcrCamera() {
        try {
            File photoDir = new File(requireContext().getCacheDir(), "ocr_photos");
            if (!photoDir.exists()) //noinspection ResultOfMethodCallIgnored
                photoDir.mkdirs();
            File photoFile = new File(photoDir, "ocr_" + System.currentTimeMillis() + ".jpg");
            ocrPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            ocrCameraLauncher.launch(ocrPhotoUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchOcrGallery() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        ocrGalleryLauncher.launch(i);
    }

    /**
     * Shows a language picker, then processes OCR on the selected image.
     */
    private void showOcrLanguagePickerThenProcess(Uri imageUri) {
        new AlertDialog.Builder(requireContext())
                .setTitle("🌐 Select Text Language")
                .setSingleChoiceItems(OCR_LANG_NAMES, selectedOcrLang, (d, which) -> {
                    selectedOcrLang = which;
                })
                .setPositiveButton("Scan", (d, w) -> processOcrImage(imageUri))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Returns the ML Kit TextRecognizer for the currently selected language.
     */
    private TextRecognizer getOcrRecognizer() {
        switch (selectedOcrLang) {
            case 1: // Hindi / Devanagari
            case 2: // Bangla / Devanagari
                return TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
            case 3: // Chinese
                return TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            case 4: // Japanese
                return TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            case 5: // Korean
                return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            default: // Latin (English etc.)
                return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
    }

    /**
     * Core OCR pipeline:
     * 1. Show loading dialog
     * 2. Create InputImage from URI
     * 3. Run ML Kit text recognition
     * 4. Insert result into editor
     */
    private void processOcrImage(Uri imageUri) {
        // Show loading Lottie dialog
        OcrLoadingDialog loadingDialog = OcrLoadingDialog.newInstance();
        try {
            loadingDialog.show(getParentFragmentManager(), "OCR_LOADING");
        } catch (IllegalStateException ignored) {}

        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            TextRecognizer recognizer = getOcrRecognizer();

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // Dismiss loading
                        try { loadingDialog.dismissAllowingStateLoss(); }
                        catch (Exception ignored) {}

                        String text = visionText.getText();
                        if (text == null || text.trim().isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "No text found in the image", Toast.LENGTH_SHORT).show();
                        } else {
                            insertOcrText(text);
                            Toast.makeText(requireContext(),
                                    "✅ Text extracted successfully!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        try { loadingDialog.dismissAllowingStateLoss(); }
                        catch (Exception ignored) {}
                        Toast.makeText(requireContext(),
                                "❌ Text recognition failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            try { loadingDialog.dismissAllowingStateLoss(); }
            catch (Exception ignored) {}
            Toast.makeText(requireContext(),
                    "Failed to load image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inserts OCR-extracted text into the WebView editor at the cursor position.
     * Uses insertHTML to preserve line breaks.
     */
    private void insertOcrText(String text) {
        if (text == null || contentWebView == null) return;

        // Convert plain text to HTML (preserve line breaks)
        String html = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");

        // Escape for JS string
        String escaped = html
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        contentWebView.evaluateJavascript(
                "document.execCommand('insertHTML', false, '" + escaped + "');", null);
        contentWebView.requestFocus();
        scheduleAutoSave();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void setupKeyboardInset(View root) {
        // Apply insets to the formatting toolbar so it sticks exactly above
        // the keyboard (or nav bar when keyboard is closed) — zero gap.
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int bottom = Math.max(imeBottom, navBottom);

            // Set toolbar bottom margin so it sits right on top of keyboard
            if (formattingToolbar != null) {
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) formattingToolbar.getLayoutParams();
                if (lp.bottomMargin != bottom) {
                    lp.bottomMargin = bottom;
                    formattingToolbar.setLayoutParams(lp);
                }
            }

            // Pad the scroll view so content isn't hidden behind toolbar + keyboard
            if (nestedScrollView != null) {
                int toolbarH = formattingToolbar != null
                        ? formattingToolbar.getHeight() : (int) (48 * getResources().getDisplayMetrics().density);
                nestedScrollView.setPadding(
                        nestedScrollView.getPaddingLeft(),
                        nestedScrollView.getPaddingTop(),
                        nestedScrollView.getPaddingRight(),
                        bottom + toolbarH);
            }

            return insets;
        });
    }

    private void syncVisibility(RecyclerView rv, List<?> list) {
        if (rv != null) rv.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        updateMediaToggle();
    }

    // ── Media panel toggle ────────────────────────────────────────────────

    private void toggleMediaPanel() {
        mediaPanelExpanded = !mediaPanelExpanded;
        if (mediaPanel != null) {
            mediaPanel.setVisibility(mediaPanelExpanded ? View.VISIBLE : View.GONE);
        }
        if (mediaToggleArrow != null) {
            mediaToggleArrow.setImageResource(
                    mediaPanelExpanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
        }
    }

    private void updateMediaToggle() {
        int totalMedia = imagePaths.size() + videoPaths.size() + voicePaths.size();
        boolean hasMedia = totalMedia > 0;

        if (mediaToggleBar != null) {
            mediaToggleBar.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
        }

        if (mediaToggleText != null && hasMedia) {
            StringBuilder sb = new StringBuilder();
            if (!imagePaths.isEmpty()) sb.append(imagePaths.size()).append(" image").append(imagePaths.size() > 1 ? "s" : "");
            if (!videoPaths.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(videoPaths.size()).append(" video").append(videoPaths.size() > 1 ? "s" : "");
            }
            if (!voicePaths.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(voicePaths.size()).append(" voice").append(voicePaths.size() > 1 ? "s" : "");
            }
            mediaToggleText.setText(sb.toString());
        }

        // If no media, collapse the panel
        if (!hasMedia) {
            mediaPanelExpanded = false;
            if (mediaPanel != null) mediaPanel.setVisibility(View.GONE);
            if (mediaToggleArrow != null) {
                mediaToggleArrow.setImageResource(R.drawable.ic_chevron_down);
            }
        }
    }

    private String persistUri(Uri uri) {
        if (uri == null) return null;
        Context context = getContext();
        if (context != null) {
            try {
                context.getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
        }
        return uri.toString();
    }

    private interface UriConsumer { void accept(Uri uri); }

    private void forEachUri(Intent data, UriConsumer consumer) {
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++)
                consumer.accept(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            consumer.accept(data.getData());
        }
    }
}