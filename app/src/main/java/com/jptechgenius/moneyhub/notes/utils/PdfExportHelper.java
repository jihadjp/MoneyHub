package com.jptechgenius.moneyhub.notes.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.Log;

import com.jptechgenius.moneyhub.notes.model.Note;

import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exports a Note to a real-text PDF.
 *
 * Rendering:
 *   • Title / timestamp  → Canvas drawText()  (crisp, vector)
 *   • Note content       → Html.fromHtml() → StaticLayout → PDF canvas
 *                          Text is REAL TEXT (not a bitmap image).
 *   • Text colors        → <span style="color:X"> pre-processed to <font color="X">
 *                          so Html.fromHtml() applies ForegroundColorSpan natively.
 *   • Highlights         → background-color converted to custom <highlight> tag,
 *                          applied as BackgroundColorSpan, drawn as filled rects on PDF canvas.
 *   • Images in content  → Html.ImageGetter loads bitmap (content:// + file paths),
 *                          scales to page width, drawn inline via ImageSpan.
 *   • Multi-page         → StaticLayout sliced line-by-line across pages.
 */
public class PdfExportHelper {

    public interface ExportCallback { void onExported(File pdfFile); }

    private static final String TAG = "PdfExportHelper";

    /* A4 @ 72 dpi — same unit system as PdfDocument */
    private static final int   PAGE_W    = 595;
    private static final int   PAGE_H    = 842;
    private static final int   MARGIN    = 48;
    private static final int   CONT_W    = PAGE_W - 2 * MARGIN;   // 499
    private static final float TEXT_SIZE = 13f;  // points

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PdfExportHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Entry ─────────────────────────────────────────────────────────────

    public void export(Note note, ExportCallback callback) {
        executor.execute(() -> {
            File pdf = buildPdf(note);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onExported(pdf);
            });
        });
    }

    // ── Build PDF ─────────────────────────────────────────────────────────

    private File buildPdf(Note note) {
        // 1. Pre-process HTML → Spanned (with real color spans + inline images + highlights)
        CharSequence spanned = htmlToSpanned(note);

        // 1b. Convert URLSpans (invisible) to blue + underline (visible on PDF canvas)
        spanned = styleLinksForPdf(spanned);

        // 2. Build StaticLayout (text engine — renders on any canvas)
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.BLACK);
        tp.setTextSize(TEXT_SIZE);

        StaticLayout layout = StaticLayout.Builder
                .obtain(spanned, 0, spanned.length(), tp, CONT_W)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.1f)
                .setIncludePad(false)
                .build();

        // 3. Compose PDF
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(
                PAGE_W, PAGE_H, 1).create();

        PdfDocument.Page page = doc.startPage(pi);
        Canvas cv = page.getCanvas();
        int y = MARGIN;

        // ── Title ─────────────────────────────────────────────────────────
        String title = (note.getTitle() != null && !note.getTitle().isEmpty())
                ? note.getTitle() : "";
        if (!title.isEmpty()) {
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextSize(20f);
            cv.drawText(title, MARGIN, y + 20, titlePaint);
            y += 32;
        }

        // ── Timestamp ─────────────────────────────────────────────────────
        Paint tsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tsPaint.setColor(Color.GRAY);
        tsPaint.setTextSize(10f);
        String ts = new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
                .format(new Date(note.getTimestamp()));
        cv.drawText(ts, MARGIN, y, tsPaint);
        y += 14;

        // Divider
        Paint divPaint = new Paint();
        divPaint.setColor(Color.LTGRAY);
        divPaint.setStrokeWidth(1f);
        cv.drawLine(MARGIN, y, PAGE_W - MARGIN, y, divPaint);
        y += 14;

        // ── Content via StaticLayout — sliced across pages ─────────────────
        int lineCount = layout.getLineCount();
        int line      = 0;

        while (line < lineCount) {
            int available = PAGE_H - MARGIN - y;
            if (available < 20) {
                doc.finishPage(page);
                page = doc.startPage(pi);
                cv   = page.getCanvas();
                cv.drawColor(Color.WHITE);
                y    = MARGIN;
                available = PAGE_H - MARGIN - y;
            }

            // Count lines that fit in available space
            int startLine     = line;
            int usedHeight    = 0;
            while (line < lineCount) {
                int lh = layout.getLineBottom(line) - layout.getLineTop(line);
                if (usedHeight + lh > available) break;
                usedHeight += lh;
                line++;
            }
            if (line == startLine) {
                // Single line taller than remaining space — force a new page
                if (startLine > 0) {
                    doc.finishPage(page);
                    page = doc.startPage(pi);
                    cv   = page.getCanvas();
                    cv.drawColor(Color.WHITE);
                    y    = MARGIN;
                }
                // Draw at least one line no matter what
                usedHeight = layout.getLineBottom(line) - layout.getLineTop(line);
                line++;
            }

            // Draw the slice [startLine, line)
            int clipTop    = layout.getLineTop(startLine);
            int clipBottom = layout.getLineBottom(line - 1);

            cv.save();
            cv.translate(MARGIN, y - clipTop);
            cv.clipRect(0, clipTop, CONT_W, clipBottom);

            // Draw highlight backgrounds BEFORE text so they appear behind
            drawHighlightBackgrounds(cv, layout, spanned, startLine, line);

            layout.draw(cv);
            cv.restore();

            y += usedHeight;
        }

        // ── Voice indicator ───────────────────────────────────────────────
        if (note.hasVoice()) {
            y += 10;
            if (y + 24 > PAGE_H - MARGIN) {
                doc.finishPage(page);
                page = doc.startPage(pi);
                cv   = page.getCanvas();
                cv.drawColor(Color.WHITE);
                y    = MARGIN;
            }
            int vCount = note.getVoicePaths() != null ? note.getVoicePaths().size() : 1;
            Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
            vp.setColor(Color.parseColor("#1565C0"));
            vp.setTextSize(12f);
            cv.drawText("\uD83C\uDFA4 " + vCount + " voice memo"
                            + (vCount > 1 ? "s" : "") + " attached",
                    MARGIN, y + 16, vp);
        }

        doc.finishPage(page);

        // ── Write to file ─────────────────────────────────────────────────
        try {
            File dir = new File(context.getCacheDir(), "pdf_previews");
            if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            File file = new File(dir, "Note_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            doc.writeTo(fos);
            fos.close();
            doc.close();
            return file;
        } catch (Exception e) {
            doc.close();
            return null;
        }
    }

    // ── Draw highlight backgrounds on PDF canvas ──────────────────────────

    /**
     * For every BackgroundColorSpan in the visible line range, draw a filled
     * rectangle behind the text on the canvas BEFORE StaticLayout.draw().
     * Canvas is already translated so layout coords map directly.
     */
    private void drawHighlightBackgrounds(Canvas cv, StaticLayout layout,
                                          CharSequence text, int fromLine, int toLine) {
        if (!(text instanceof Spanned)) return;
        Spanned sp = (Spanned) text;
        BackgroundColorSpan[] spans = sp.getSpans(0, sp.length(), BackgroundColorSpan.class);
        if (spans == null || spans.length == 0) return;

        Paint hlPaint = new Paint();
        hlPaint.setStyle(Paint.Style.FILL);

        for (BackgroundColorSpan span : spans) {
            int spanStart = sp.getSpanStart(span);
            int spanEnd   = sp.getSpanEnd(span);
            hlPaint.setColor(span.getBackgroundColor());

            for (int ln = fromLine; ln < toLine; ln++) {
                int lineStart = layout.getLineStart(ln);
                int lineEnd   = layout.getLineEnd(ln);

                // Check overlap between span and this line
                int overlapStart = Math.max(spanStart, lineStart);
                int overlapEnd   = Math.min(spanEnd, lineEnd);
                if (overlapStart >= overlapEnd) continue;

                float left   = layout.getPrimaryHorizontal(overlapStart);
                float right  = layout.getPrimaryHorizontal(overlapEnd);
                if (right < left) { float tmp = left; left = right; right = tmp; }

                int top    = layout.getLineTop(ln);
                int bottom = layout.getLineBottom(ln);

                cv.drawRect(new RectF(left, top, right, bottom), hlPaint);
            }
        }
    }

    // ── Style links for PDF rendering ─────────────────────────────────────

    private static final int LINK_COLOR = Color.parseColor("#1565C0");

    /**
     * Html.fromHtml() converts {@code <a href>} to URLSpan, but StaticLayout
     * does NOT render URLSpan with any visible color or underline on PDF canvas.
     * This method replaces every URLSpan with a blue ForegroundColorSpan +
     * UnderlineSpan so links appear correctly in the exported PDF.
     */
    private CharSequence styleLinksForPdf(CharSequence text) {
        if (!(text instanceof Spanned)) return text;
        Spanned source = (Spanned) text;
        URLSpan[] urlSpans = source.getSpans(0, source.length(), URLSpan.class);
        if (urlSpans == null || urlSpans.length == 0) return text;

        // Copy into a mutable SpannableStringBuilder
        SpannableStringBuilder sb = new SpannableStringBuilder(source);

        for (URLSpan urlSpan : urlSpans) {
            int start = sb.getSpanStart(urlSpan);
            int end   = sb.getSpanEnd(urlSpan);
            int flags = sb.getSpanFlags(urlSpan);
            if (start < 0 || end < 0) continue;

            // Remove the URLSpan (it's invisible on PDF anyway)
            sb.removeSpan(urlSpan);

            // Add visible styling: blue color + underline
            sb.setSpan(new ForegroundColorSpan(LINK_COLOR), start, end, flags);
            sb.setSpan(new UnderlineSpan(), start, end, flags);
        }

        return sb;
    }

    // ── HTML → Spanned ────────────────────────────────────────────────────

    private CharSequence htmlToSpanned(Note note) {
        String raw = note.getContent() != null ? note.getContent() : "";
        raw = appendMissingImageTags(raw, note.getImagePaths());

        // -- Pre-processing: make Html.fromHtml understand more tags --

        // 1a. Convert <span style="...background-color: X ...;... color: Y ...">
        //     into <highlight color="X"><font color="Y">...</font></highlight>
        //     so both highlight and text color are preserved.
        // 1b. <span style="color: X"> (no bg) → <font color="X">
        // 1c. <span style="background-color: X"> (no fg) → <highlight color="X">
        raw = convertSpanTags(raw);

        // 2. <strong> → <b>,  <em> → <i>  (fromHtml handles <b> and <i>)
        raw = raw.replaceAll("(?i)<strong>", "<b>").replaceAll("(?i)</strong>", "</b>");
        raw = raw.replaceAll("(?i)<em>",     "<i>").replaceAll("(?i)</em>",     "</i>");

        // 3. Newline normalisation
        raw = raw.replaceAll("(?i)<br\\s*/?>", "<br>");

        final String finalHtml = raw;

        // ImageGetter: loads bitmaps for <img src="..."> inline
        Html.ImageGetter imageGetter = src -> {
            try {
                Bitmap bmp = loadBitmap(src);
                if (bmp == null) {
                    Log.w(TAG, "Skipping image in PDF, unable to decode src=" + src);
                    return null;
                }

                // Scale to fit CONT_W (499 PDF units)
                float aspect = (float) bmp.getHeight() / bmp.getWidth();
                int   dstW   = CONT_W;
                int   dstH   = Math.round(dstW * aspect);

                // Cap tall images to half page height
                if (dstH > (PAGE_H / 2)) {
                    dstH = PAGE_H / 2;
                    dstW = Math.round(dstH / aspect);
                }

                Bitmap scaled = Bitmap.createScaledBitmap(bmp, dstW, dstH, true);
                if (scaled != bmp) bmp.recycle();

                BitmapDrawable drawable = new BitmapDrawable(
                        context.getResources(), scaled);
                drawable.setBounds(0, 0, dstW, dstH);
                return drawable;
            } catch (Exception e) {
                Log.e(TAG, "ImageGetter failed for src=" + src, e);
                return null;
            }
        };

        // TagHandler for <highlight color="..."> → BackgroundColorSpan
        Html.TagHandler highlightTagHandler = new HighlightTagHandler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(finalHtml,
                    Html.FROM_HTML_MODE_LEGACY,
                    imageGetter, highlightTagHandler);
        }
        //noinspection deprecation
        return Html.fromHtml(finalHtml, imageGetter, highlightTagHandler);
    }

    // ── Convert <span style="..."> to <font> and/or <highlight> ───────────

    /** Pattern to match opening <span ...style="..."> tags */
    private static final Pattern SPAN_OPEN = Pattern.compile(
            "(?i)<span[^>]*style=\"([^\"]*)\"[^>]*>");

    /** Pattern to extract background-color value */
    private static final Pattern BG_COLOR = Pattern.compile(
            "(?i)background-color:\\s*([^;\"]+)");

    /** Pattern to extract foreground color value */
    private static final Pattern FG_COLOR = Pattern.compile(
            "(?i)(?<!background-)color:\\s*([^;\"]+)");

    /**
     * Converts every <span style="..."> in the HTML into a combination of:
     *   • <highlight color="X"> for background-color
     *   • <font color="X">     for text color
     * Closing </span> is replaced with the matching closing tags.
     */
    private String convertSpanTags(String html) {
        StringBuilder sb = new StringBuilder();
        Matcher m = SPAN_OPEN.matcher(html);

        // Track what replacement closing tags each <span> needs
        Deque<String> closingStack = new ArrayDeque<>();
        int lastEnd = 0;
        // We also need to replace </span> in order, so collect positions
        // Step 1: replace opening <span> tags
        while (m.find()) {
            sb.append(html, lastEnd, m.start());

            String style = m.group(1);
            Matcher bgM = BG_COLOR.matcher(style != null ? style : "");
            Matcher fgM = FG_COLOR.matcher(style != null ? style : "");

            String bgColor = bgM.find() ? bgM.group(1).trim() : null;
            String fgColor = fgM.find() ? fgM.group(1).trim() : null;

            StringBuilder openTags = new StringBuilder();
            StringBuilder closeTags = new StringBuilder();

            if (bgColor != null && !bgColor.isEmpty()) {
                String hexBg = cssColorToHex(bgColor);
                openTags.append("<highlight color=\"").append(hexBg).append("\">");
                closeTags.insert(0, "</highlight>");
            }
            if (fgColor != null && !fgColor.isEmpty()) {
                String hexFg = cssColorToHex(fgColor);
                openTags.append("<font color=\"").append(hexFg).append("\">");
                closeTags.insert(0, "</font>");
            }

            sb.append(openTags);
            closingStack.push(closeTags.toString());

            lastEnd = m.end();
        }
        sb.append(html, lastEnd, html.length());

        // Step 2: replace </span> tags with stacked closing tags
        String result = sb.toString();
        sb = new StringBuilder();
        Pattern closeSpan = Pattern.compile("(?i)</span>");
        Matcher cm = closeSpan.matcher(result);
        lastEnd = 0;
        while (cm.find()) {
            sb.append(result, lastEnd, cm.start());
            if (!closingStack.isEmpty()) {
                sb.append(closingStack.pop());
            } else {
                // orphan </span>, just remove
            }
            lastEnd = cm.end();
        }
        sb.append(result, lastEnd, result.length());

        return sb.toString();
    }

    /**
     * Best-effort conversion of CSS color values to hex.
     * Handles: #xxx, #xxxxxx, rgb(r,g,b), rgba(r,g,b,a), named colors.
     */
    private String cssColorToHex(String css) {
        if (css == null) return "#000000";
        css = css.trim().toLowerCase(Locale.US);

        // Already hex
        if (css.startsWith("#")) return css;

        // rgb(r, g, b) or rgba(r, g, b, a)
        if (css.startsWith("rgb")) {
            Pattern nums = Pattern.compile("(\\d+)");
            Matcher nm = nums.matcher(css);
            int[] rgb = new int[3];
            for (int i = 0; i < 3 && nm.find(); i++) {
                rgb[i] = Integer.parseInt(nm.group(1));
            }
            return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        }

        // Common named colors used by the editor
        switch (css) {
            case "yellow":      return "#FFFF00";
            case "green":       return "#69FF47";
            case "cyan":        return "#18FFFF";
            case "pink":        return "#FF6EC7";
            case "red":         return "#FF0000";
            case "blue":        return "#0000FF";
            case "white":       return "#FFFFFF";
            case "black":       return "#000000";
            case "orange":      return "#FFA500";
            case "transparent": return "#00000000";
            default:
                // Try to parse with Android Color
                try { Color.parseColor(css); return css; }
                catch (Exception e) { return "#000000"; }
        }
    }

    // ── Custom TagHandler for <highlight> ─────────────────────────────────

    /**
     * Handles <highlight color="X">...</highlight> tags by applying
     * BackgroundColorSpan to the spanned text. This span is then used
     * in drawHighlightBackgrounds() to paint rects on the PDF canvas.
     */
    private static class HighlightTagHandler implements Html.TagHandler {
        @Override
        public void handleTag(boolean opening, String tag, Editable output,
                              XMLReader xmlReader) {
            if (!"highlight".equalsIgnoreCase(tag)) return;

            if (opening) {
                // Read the "color" attribute from the XMLReader
                String color = getAttribute(xmlReader, "color");
                // Mark start position
                int start = output.length();
                output.setSpan(new HighlightMark(color), start, start,
                        Spannable.SPAN_MARK_MARK);
            } else {
                // Find the opening mark
                HighlightMark mark = getLast(output, HighlightMark.class);
                if (mark != null) {
                    int start = output.getSpanStart(mark);
                    output.removeSpan(mark);
                    int end = output.length();
                    if (start != end && mark.color != null) {
                        try {
                            int c = Color.parseColor(mark.color);
                            output.setSpan(new BackgroundColorSpan(c),
                                    start, end,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } catch (Exception ignored) { }
                    }
                }
            }
        }

        /** Temporary marker span to track opening position + color */
        private static class HighlightMark {
            final String color;
            HighlightMark(String color) { this.color = color; }
        }

        private static <T> T getLast(Editable text, Class<T> kind) {
            T[] spans = text.getSpans(0, text.length(), kind);
            if (spans == null || spans.length == 0) return null;
            return spans[spans.length - 1];
        }

        /**
         * Read an attribute from the XMLReader by reflection.
         * Html.fromHtml internally uses an XMLReader; the attributes of
         * custom tags can only be read via reflection on the "theAttributes" field.
         */
        private static String getAttribute(XMLReader xmlReader, String attrName) {
            try {
                Field f = xmlReader.getClass().getDeclaredField("theNewElement");
                f.setAccessible(true);
                Object element = f.get(xmlReader);
                if (element != null) {
                    Field attsF = element.getClass().getDeclaredField("theAtts");
                    attsF.setAccessible(true);
                    Object atts = attsF.get(element);
                    if (atts != null) {
                        // atts is an org.ccil.cowan.tagsoup.AttributesImpl
                        java.lang.reflect.Method getIndex = atts.getClass()
                                .getMethod("getIndex", String.class);
                        int idx = (int) getIndex.invoke(atts, attrName);
                        if (idx >= 0) {
                            java.lang.reflect.Method getValue = atts.getClass()
                                    .getMethod("getValue", int.class);
                            return (String) getValue.invoke(atts, idx);
                        }
                    }
                }
            } catch (Exception ignored) { }

            // Fallback: try "theAtts" directly on xmlReader
            try {
                Field f = xmlReader.getClass().getDeclaredField("theAtts");
                f.setAccessible(true);
                Object atts = f.get(xmlReader);
                if (atts != null) {
                    java.lang.reflect.Method getIndex = atts.getClass()
                            .getMethod("getIndex", String.class);
                    int idx = (int) getIndex.invoke(atts, attrName);
                    if (idx >= 0) {
                        java.lang.reflect.Method getValue = atts.getClass()
                                .getMethod("getValue", int.class);
                        return (String) getValue.invoke(atts, idx);
                    }
                }
            } catch (Exception ignored) { }

            return null;
        }
    }

    // ── Image loading ─────────────────────────────────────────────────────

    private Bitmap loadBitmap(String src) {
        try {
            byte[] bytes = readBytes(src);
            if (bytes == null || bytes.length == 0) return null;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private byte[] readBytes(String path) {
        HttpURLConnection connection = null;
        try {
            if (path == null) return null;
            String source = Html.fromHtml(path).toString().trim();
            if (source.isEmpty()) return null;

            if (source.startsWith("data:image")) {
                int comma = source.indexOf(',');
                if (comma < 0 || comma + 1 >= source.length()) return null;
                String b64 = source.substring(comma + 1);
                return Base64.decode(b64, Base64.DEFAULT);
            }

            InputStream is;
            if (source.startsWith("content://") || source.startsWith("file://")) {
                is = context.getContentResolver().openInputStream(Uri.parse(source));
            } else if (source.startsWith("http://") || source.startsWith("https://")) {
                URL url = new URL(source);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setDoInput(true);
                connection.connect();
                is = connection.getInputStream();
            } else {
                is = new java.io.FileInputStream(source);
            }

            if (is == null) return null;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
            is.close();
            return buf.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "readBytes failed for image source=" + path, e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String appendMissingImageTags(String html, List<String> imagePaths) {
        String safeHtml = html != null ? html : "";
        if (imagePaths == null || imagePaths.isEmpty()) return safeHtml;
        if (safeHtml.toLowerCase(Locale.US).contains("<img")) return safeHtml;

        StringBuilder out = new StringBuilder(safeHtml);
        for (String path : imagePaths) {
            if (path == null || path.trim().isEmpty()) continue;
            out.append("<br><img src=\"")
                    .append(escapeHtmlAttr(path.trim()))
                    .append("\"/><br>");
        }
        return out.toString();
    }

    private String escapeHtmlAttr(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

