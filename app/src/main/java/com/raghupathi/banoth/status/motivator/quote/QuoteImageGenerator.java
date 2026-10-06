package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;

import androidx.annotation.DrawableRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class QuoteImageGenerator {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final float HORIZONTAL_MARGIN = 108f;
    private static final float SAFE_TOP = 300f;
    private static final float SAFE_BOTTOM = 1500f;
    private static final int MIN_DECODED_DIMENSION = 1600;

    private QuoteImageGenerator() {
    }

    public static File generate(Context context, QuoteRepository.QuoteItem quote, @DrawableRes int backgroundRes)
            throws IOException {
        Bitmap image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        drawBackground(context, canvas, backgroundRes);
        drawOverlay(canvas);
        drawQuote(context, canvas, quote);
        File directory = new File(context.getCacheDir(), "shared_quotes");
        if (!directory.exists() && !directory.mkdirs()) {
            image.recycle();
            throw new IOException("Unable to create image cache directory");
        }
        File output = new File(directory, "daily_quote_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!image.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new IOException("Unable to encode quote image");
            }
        } finally {
            image.recycle();
        }
        return output;
    }

    private static void drawBackground(Context context, Canvas canvas, @DrawableRes int backgroundRes) {
        Bitmap bitmap = decodeBackground(context, backgroundRes);
        if (bitmap == null) {
            canvas.drawColor(Color.rgb(27, 33, 48));
            return;
        }
        float scale = Math.max((float) WIDTH / bitmap.getWidth(), (float) HEIGHT / bitmap.getHeight());
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        canvas.drawBitmap(bitmap, null, new Rect((WIDTH - width) / 2, (HEIGHT - height) / 2,
                (WIDTH - width) / 2 + width, (HEIGHT - height) / 2 + height), null);
        bitmap.recycle();
    }

    private static Bitmap decodeBackground(Context context, @DrawableRes int backgroundRes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), backgroundRes, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        return BitmapFactory.decodeResource(context.getResources(), backgroundRes, options);
    }

    private static int calculateSampleSize(int width, int height) {
        int sampleSize = 1;
        while (width / (sampleSize * 2) >= MIN_DECODED_DIMENSION
                && height / (sampleSize * 2) >= MIN_DECODED_DIMENSION) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static void drawOverlay(Canvas canvas) {
        Paint overlay = new Paint();
        overlay.setShader(new LinearGradient(0, 0, 0, HEIGHT,
                new int[]{0x7007111F, 0x3D07111F, 0xA607111F, 0xD907111F},
                new float[]{0f, .28f, .7f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, overlay);
    }

    private static void drawQuote(Context context, Canvas canvas, QuoteRepository.QuoteItem quote) {
        Paint quotePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        quotePaint.setColor(Color.WHITE);
        quotePaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        quotePaint.setTextAlign(Paint.Align.CENTER);
        float maximumWidth = WIDTH - (2 * HORIZONTAL_MARGIN);
        String quoteText = quote.text.isEmpty() ? context.getString(R.string.no_quotes_available) : quote.text;
        float size = quoteTextLength(quote) < 80 ? 76f : quoteTextLength(quote) < 180 ? 60f : 48f;
        List<String> lines = wrap(quoteText, quotePaint, maximumWidth, size);
        while (lines.size() * (size * 1.32f) > SAFE_BOTTOM - SAFE_TOP - 110f && size > 18f) {
            size -= 2f;
            lines = wrap(quoteText, quotePaint, maximumWidth, size);
        }
        quotePaint.setTextSize(size);
        float lineHeight = size * 1.32f;
        List<String> authorLines = quote.author.isEmpty()
                ? new ArrayList<>() : wrap("— " + quote.author, quotePaint, maximumWidth, Math.max(32f, size * .52f));
        float authorHeight = authorLines.isEmpty() ? 0f : authorLines.size() * Math.max(32f, size * .52f) * 1.45f + 36f;
        float textHeight = lines.size() * lineHeight + authorHeight;
        float baseline = Math.max(SAFE_TOP + size, (SAFE_TOP + SAFE_BOTTOM - textHeight) / 2f + size);
        for (String line : lines) {
            canvas.drawText(line, WIDTH / 2f, baseline, quotePaint);
            baseline += lineHeight;
        }
        if (!authorLines.isEmpty()) {
            Paint authorPaint = new Paint(quotePaint);
            authorPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            authorPaint.setTextSize(Math.max(34f, size * .52f));
            authorPaint.setColor(0xE6FFFFFF);
            baseline += 34f;
            float authorLineHeight = authorPaint.getTextSize() * 1.45f;
            for (String line : authorLines) {
                canvas.drawText(line, WIDTH / 2f, baseline, authorPaint);
                baseline += authorLineHeight;
            }
        }
        Paint brandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brandPaint.setColor(0xCCFFFFFF);
        brandPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brandPaint.setTextAlign(Paint.Align.CENTER);
        brandPaint.setTextSize(28f);
        canvas.drawText(context.getString(R.string.app_name), WIDTH / 2f, HEIGHT - 94f, brandPaint);
    }

    private static List<String> wrap(String text, Paint paint, float width, float size) {
        paint.setTextSize(size);
        List<String> lines = new ArrayList<>();
        String trimmed = text.trim();
        String[] words = trimmed.contains(" ") ? trimmed.split("\\s+") : splitCodePoints(trimmed);
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String separator = trimmed.contains(" ") ? " " : "";
            String candidate = line.length() == 0 ? word : line + separator + word;
            if (paint.measureText(candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                appendBrokenWord(lines, line, word, paint, width);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static int quoteTextLength(QuoteRepository.QuoteItem quote) {
        return quote.text.isEmpty() ? 0 : quote.text.codePointCount(0, quote.text.length());
    }

    private static String[] splitCodePoints(String text) {
        List<String> glyphs = new ArrayList<>();
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            glyphs.add(new String(Character.toChars(codePoint)));
            index += Character.charCount(codePoint);
        }
        return glyphs.toArray(new String[0]);
    }

    private static void appendBrokenWord(List<String> lines, StringBuilder line, String word,
                                         Paint paint, float width) {
        for (String glyph : splitCodePoints(word)) {
            String candidate = line.toString() + glyph;
            if (line.length() > 0 && paint.measureText(candidate) > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(glyph);
        }
    }
}
