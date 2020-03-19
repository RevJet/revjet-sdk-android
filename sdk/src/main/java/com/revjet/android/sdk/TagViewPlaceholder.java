/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * This class is intended for Android XML Graphical Layout viewer.
 */
public final class TagViewPlaceholder {
    private float mWidth;
    private float mHeight;
    private final DisplayMetrics mDisplayMetrics;
    private final Paint mPaint = new Paint();

    public TagViewPlaceholder(DisplayMetrics displayMetrics) {
        mDisplayMetrics = displayMetrics;
    }

    public void draw(Canvas canvas) {
        if (mWidth != 0 && mHeight != 0) {
            mPaint.setLinearText(true);
            mPaint.setAntiAlias(true);

            // Fill the background with a solid color
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, mWidth, mHeight, mPaint);

            // Draw the frame around it
            mPaint.setStrokeWidth(2);
            mPaint.setColor(Color.GRAY);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, 0, mWidth - 1, mHeight - 1, mPaint);

            // Draw the "RevJet" log
            String logoTextPart1 = "REV";
            String logoTextPart2 = "JET";
            String logoText = logoTextPart1 + logoTextPart2;

            Rect bounds = new Rect();
            float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26, mDisplayMetrics);

            mPaint.setTypeface(Typeface.SANS_SERIF);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTextSize(textSize);
            mPaint.getTextBounds(logoText, 0, logoText.length(), bounds);

            float textWidth = mPaint.measureText(logoText);
            float startX = ((mWidth / 2) - (textWidth / 4));
            float startY = ((mHeight / 2) - ((mPaint.descent() + mPaint.ascent()) / 2));

            mPaint.setColor(Color.rgb(253, 64, 13));
            canvas.drawText(logoTextPart1, startX, startY, mPaint);

            mPaint.setColor(Color.BLACK);
            canvas.drawText(logoTextPart2, startX + (textWidth / 2), startY, mPaint);
        }
    }

    public void setSize(float width, float height) {
        mWidth = width;
        mHeight = height;
    }
}
