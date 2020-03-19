/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.toolbar.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

public class VASTLearnMoreDrawable extends VASTCircleDrawable {
    private final Paint learnMorePaint;

    public VASTLearnMoreDrawable() {
        super();

        learnMorePaint = new Paint(getPaint());
        learnMorePaint.setStrokeWidth(4.5f);
        learnMorePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);

        int mDisplacement = (int) (0.5f * getRadius() / Math.sqrt(2f));
        int mBarbLength = (int) (1.5f * mDisplacement);

        Point centerPoint = new Point(getCenterX(), getCenterY());

        Point bottomLeftPoint = new Point(centerPoint);
        bottomLeftPoint.offset(-mDisplacement, mDisplacement);

        Point topRightPoint = new Point(centerPoint);
        topRightPoint.offset(mDisplacement, -mDisplacement);

        Point leftBarbPoint = new Point(topRightPoint);
        leftBarbPoint.offset(-mBarbLength, 0);

        Point rightBarbPoint = new Point(topRightPoint);
        rightBarbPoint.offset(0, mBarbLength);

        canvas.drawLine(bottomLeftPoint.x, bottomLeftPoint.y, topRightPoint.x, topRightPoint.y, learnMorePaint);
        canvas.drawLine(topRightPoint.x, topRightPoint.y, leftBarbPoint.x, leftBarbPoint.y, learnMorePaint);
        canvas.drawLine(topRightPoint.x, topRightPoint.y, rightBarbPoint.x, rightBarbPoint.y, learnMorePaint);
    }
}
