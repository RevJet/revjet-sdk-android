/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.toolbar.drawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import com.revjet.android.sdk.commons.Utils;

public class VASTCountdownDrawable extends VASTCircleDrawable implements VASTTextDrawable {
    private final static float TEXT_SIZE_SP = 18f;
    private final Paint mTextPaint;
    private String mSecondsRemaining;
    private Rect mTextRect;

    public VASTCountdownDrawable(final Context context) {
        super();

        mSecondsRemaining = "";

        mTextPaint = new Paint();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float textSizePixels = Utils.pixelsToDips(TEXT_SIZE_SP, displayMetrics);

        mTextPaint.setTextSize(textSizePixels);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.GRAY);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mTextRect = new Rect();
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);

        final String text = String.valueOf(mSecondsRemaining);

        mTextPaint.getTextBounds(text, 0, text.length(), mTextRect);

        final int x = getCenterX() - mTextRect.width() / 2;
        final int y = getCenterY() + mTextRect.height() / 2;

        canvas.drawText(text, x, y, mTextPaint);
    }

    /**
     * TextDrawable implementation
     */

    public void updateText(final String text) {
        if (!mSecondsRemaining.equals(text)) {
            mSecondsRemaining = text;
            invalidateSelf();
        }
    }
}
