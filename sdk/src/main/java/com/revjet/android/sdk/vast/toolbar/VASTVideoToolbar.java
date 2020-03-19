/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.toolbar;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.vast.toolbar.drawable.VASTCloseButtonDrawable;
import com.revjet.android.sdk.vast.toolbar.drawable.VASTCountdownDrawable;
import com.revjet.android.sdk.vast.toolbar.drawable.VASTLearnMoreDrawable;

import static android.graphics.Color.BLACK;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

public class VASTVideoToolbar extends LinearLayout {
    private static final int TOOLBAR_HEIGHT_DIPS = 44;
    private static final int THRESHOLD_FOR_HIDING_VIDEO_DURATION = 200;

    private VASTVideoToolbarElement mDurationWidget;
    private VASTVideoToolbarElement mLearnMoreWidget;
    private VASTVideoToolbarElement mCountdownWidget;
    private VASTVideoToolbarElement mCloseButtonWidget;

    public VASTVideoToolbar(Context context) {
        super(context);

        setId((int) Utils.generateUniqueId());

        // Consume all click events on the video toolbar
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final int videoToolbarHeight = (int) Utils.dipsToPixels(TOOLBAR_HEIGHT_DIPS, displayMetrics);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                FILL_PARENT,
                videoToolbarHeight);
        setLayoutParams(layoutParams);

        setBackgroundColor(BLACK);
        getBackground().setAlpha(180);

        mDurationWidget = createDurationToolbarElement();
        mLearnMoreWidget = createLearnMoreToolbarElement();
        mCountdownWidget = createCountdownToolbarElement();
        mCloseButtonWidget = createCloseButtonToolbarElement();

        addView(mDurationWidget);
        addView(mLearnMoreWidget);
        addView(mCountdownWidget);
        addView(mCloseButtonWidget);
    }

    String getDisplaySeconds(final long millisecondsRemaining) {
        return String.valueOf(Math.round(Math.ceil(millisecondsRemaining / 1000f)));
    }

    public void updateDurationToolbarElement(final int remainingTime) {
        if (remainingTime >= THRESHOLD_FOR_HIDING_VIDEO_DURATION) {
            String secondsString = getDisplaySeconds(remainingTime);
            String secondsSuffix = " seconds";
            if (secondsString.equals("1")) {
                secondsSuffix = " second";
            }
            mDurationWidget.updateText("Ends in " + secondsString + secondsSuffix);
        } else if (remainingTime >= 0) {
            mDurationWidget.updateText("Thanks for watching");
        }
    }

    public void updateCountdownToolbarElement(final int remainingTime) {
        if (remainingTime >= 0 && mCountdownWidget.getVisibility() == View.INVISIBLE) {
            mCloseButtonWidget.setVisibility(View.GONE);
            mCountdownWidget.setVisibility(View.VISIBLE);
        }

        mCountdownWidget.updateImageText(getDisplaySeconds(remainingTime));
    }

    public void makeInteractable() {
        // The countdown timer has ended and user can interact with close and learn more button
        mCountdownWidget.setVisibility(View.GONE);
        mLearnMoreWidget.setVisibility(View.VISIBLE);
        mCloseButtonWidget.setVisibility(View.VISIBLE);
    }

    public void setCloseButtonOnTouchListener(final OnTouchListener onTouchListener) {
        mCloseButtonWidget.setOnTouchListener(onTouchListener);
    }

    public void setLearnMoreButtonOnTouchListener(final OnTouchListener onTouchListener) {
        mLearnMoreWidget.setOnTouchListener(onTouchListener);
    }

    private VASTVideoToolbarElement createDurationToolbarElement() {
        VASTVideoToolbarElement.Configuration configuration = new VASTVideoToolbarElement.Configuration(getContext());
        configuration.setWeight(2);
        configuration.setWidgetGravity(CENTER_VERTICAL | LEFT);
        configuration.setHasText(true);
        configuration.setTextAlign(RelativeLayout.ALIGN_PARENT_LEFT);
        return new VASTVideoToolbarElement(configuration);
    }

    private VASTVideoToolbarElement createLearnMoreToolbarElement() {
        VASTVideoToolbarElement.Configuration configuration = new VASTVideoToolbarElement.Configuration(getContext());
        configuration.setWeight(1);
        configuration.setWidgetGravity(CENTER_VERTICAL | RIGHT);
        configuration.setDefaultText("Learn More");
        configuration.setDrawable(new VASTLearnMoreDrawable());
        configuration.setVisibility(View.INVISIBLE);
        return new VASTVideoToolbarElement(configuration);
    }

    private VASTVideoToolbarElement createCountdownToolbarElement() {
        VASTVideoToolbarElement.Configuration configuration = new VASTVideoToolbarElement.Configuration(getContext());
        configuration.setWeight(1);
        configuration.setWidgetGravity(CENTER_VERTICAL | RIGHT);
        configuration.setDefaultText("Skip in");
        configuration.setDrawable(new VASTCountdownDrawable(getContext()));
        configuration.setVisibility(View.INVISIBLE);
        return new VASTVideoToolbarElement(configuration);
    }

    private VASTVideoToolbarElement createCloseButtonToolbarElement() {
        VASTVideoToolbarElement.Configuration configuration = new VASTVideoToolbarElement.Configuration(getContext());
        configuration.setWeight(1);
        configuration.setWidgetGravity(CENTER_VERTICAL | RIGHT);
        configuration.setDefaultText("Close");
        configuration.setDrawable(new VASTCloseButtonDrawable());
        configuration.setVisibility(View.GONE);
        return new VASTVideoToolbarElement(configuration);
    }
}
