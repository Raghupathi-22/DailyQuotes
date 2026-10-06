package com.raghupathi.banoth.status.motivator.quote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class QuoteImageGenerator {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1920;
    private static final float HORIZONTAL_MARGIN = 108f;

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
        Drawable drawable = ContextCompat.getDrawable(context, backgroundRes);
        if (drawable == null) {
            canvas.drawColor(Color.rgb(27, 33, 48));
            return;
        }
        int intrinsicWidth = Math.max(1, drawable.getIntrinsicWidth());
        int intrinsicHeight = Math.max(1, drawable.getIntrinsicHeight());
        float scale = Math.max((float) WIDTH / intrinsicWidth, (float) HEIGHT / intrinsicHeight);
        int width = Math.round(intrinsicWidth * scale);
        int height = Math.round(intrinsicHeight * scale);
        int left = (WIDTH - width) / 2;
        int top = (HEIGHT - height) / 2;
        drawable.setBounds(left, top, left + width, top + height);
        drawable.draw(canvas);
    }

    private static void drawOverlay(Canvas canvas) {
        Paint overlay = new Paint();
        overlay.setShader(new LinearGradient(0, 0, 0, HEIGHT,
                new int[]{0xAA07111F, 0x6607111F, 0xD907111F},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, overlay);
    }

    private static void drawQuote(Context context, Canvas canvas, QuoteRepository.QuoteItem quote) {
        Paint quotePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        quotePaint.setColor(Color.WHITE);
        quotePaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        quotePaint.setTextAlign(Paint.Align.CENTER);
        float maximumWidth = WIDTH - (2 * HORIZONTAL_MARGIN);
        float size = quote.text.length() < 80 ? 74f : quote.text.length() < 180 ? 60f : 48f;
        String quoteText = quote.text.isEmpty() ? context.getString(R.string.no_quotes_available) : quote.text;
        List<String> lines = wrap(quoteText, quotePaint, maximumWidth, size);
        while ((lines.size() * (size * 1.3f) > 920f || lines.size() > 12) && size > 32f) {
            size -= 2f;
            lines = wrap(quoteText, quotePaint, maximumWidth, size);
        }
        quotePaint.setTextSize(size);
        float lineHeight = size * 1.32f;
        float authorHeight = quote.author.isEmpty() ? 0f : 100f;
        float textHeight = lines.size() * lineHeight + authorHeight;
        float baseline = (HEIGHT - textHeight) / 2f + size;
        for (String line : lines) {
            canvas.drawText(line, WIDTH / 2f, baseline, quotePaint);
            baseline += lineHeight;
        }
        if (!quote.author.isEmpty()) {
            Paint authorPaint = new Paint(quotePaint);
            authorPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            authorPaint.setTextSize(Math.max(34f, size * .52f));
            authorPaint.setColor(0xE6FFFFFF);
            canvas.drawText("— " + quote.author, WIDTH / 2f, baseline + 36f, authorPaint);
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
        String[] words = text.trim().contains(" ") ? text.trim().split("\\s+") : text.trim().split("");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String separator = text.trim().contains(" ") ? " " : "";
            String candidate = line.length() == 0 ? word : line + separator + word;
            if (paint.measureText(candidate) <= width || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
