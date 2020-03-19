/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.toolbar.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

public class VASTCloseButtonDrawable extends VASTCircleDrawable {
    private final Paint closeButtonPaint;

    public VASTCloseButtonDrawable() {
        super();

        closeButtonPaint = new Paint(getPaint());
        closeButtonPaint.setStrokeWidth(4.5f);
        closeButtonPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);

        int mDisplacement = (int) (0.5f * getRadius() / (float) Math.sqrt(2f));

        Point centerPoint = new Point(getCenterX(), getCenterY());

        Point bottomLeftPoint = new Point(centerPoint);
        bottomLeftPoint.offset(-mDisplacement, mDisplacement);

        Point topLeftPoint = new Point(centerPoint);
        topLeftPoint.offset(-mDisplacement, -mDisplacement);

        Point topRightPoint = new Point(centerPoint);
        topRightPoint.offset(mDisplacement, -mDisplacement);

        Point bottomRightPoint = new Point(centerPoint);
        bottomRightPoint.offset(mDisplacement, mDisplacement);

        canvas.drawLine(bottomLeftPoint.x, bottomLeftPoint.y, topRightPoint.x, topRightPoint.y, closeButtonPaint);
        canvas.drawLine(topLeftPoint.x, topLeftPoint.y, bottomRightPoint.x, bottomRightPoint.y, closeButtonPaint);
    }
}
