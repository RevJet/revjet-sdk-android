/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.toolbar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.vast.toolbar.drawable.VASTTextDrawable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class VASTVideoToolbarElement extends RelativeLayout {
    static class Configuration {
        private final Context context;
        private float weight;
        private int widgetGravity;

        private boolean hasText;
        private String defaultText;

        private boolean hasDrawable;
        private Drawable drawable;

        private OnTouchListener onTouchListener;
        private int visibility;
        private int textAlign;
        private int drawableAlign;

        public Configuration(Context context) {
            this.context = context;
            this.weight = 1f;
            this.widgetGravity = Gravity.CENTER;

            this.visibility = View.VISIBLE;

            this.textAlign = ALIGN_PARENT_LEFT;
            this.drawableAlign = ALIGN_PARENT_RIGHT;
        }

        public Context getContext() {
            return context;
        }

        public float getWeight() {
            return weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public int getWidgetGravity() {
            return widgetGravity;
        }

        public void setWidgetGravity(int widgetGravity) {
            this.widgetGravity = widgetGravity;
        }

        public boolean hasText() {
            return hasText;
        }

        public void setHasText(boolean hasText) {
            this.hasText = hasText;
        }

        public String getDefaultText() {
            return defaultText;
        }

        public void setDefaultText(String defaultText) {
            if (null != defaultText) {
                hasText = true;
            }
            this.defaultText = defaultText;
        }

        public boolean hasDrawable() {
            return hasDrawable;
        }

        public void setHasDrawable(boolean hasDrawable) {
            this.hasDrawable = hasDrawable;
        }

        public Drawable getDrawable() {
            return drawable;
        }

        public void setDrawable(Drawable drawable) {
            if (null != drawable) {
                hasDrawable = true;
            }
            this.drawable = drawable;
        }

        public OnTouchListener getOnTouchListener() {
            return onTouchListener;
        }

        public void setOnTouchListener(OnTouchListener onTouchListener) {
            this.onTouchListener = onTouchListener;
        }

        public int getVisibility() {
            return visibility;
        }

        public void setVisibility(int visibility) {
            this.visibility = visibility;
        }

        public int getTextAlign() {
            return textAlign;
        }

        public void setTextAlign(int textAlign) {
            this.textAlign = textAlign;
        }

        public int getDrawableAlign() {
            return drawableAlign;
        }

        public void setDrawableAlign(int drawableAlign) {
            this.drawableAlign = drawableAlign;
        }
    }

    private static final int TEXT_PADDING_DIPS = 5;
    private static final int IMAGE_PADDING_DIPS = 5;
    private static final int IMAGE_SIDE_LENGTH_DIPS = 37;

    private ImageView mImageView;
    private TextView mTextView;

    private int mTextPadding;
    private int mImagePadding;
    private int mImageSideLength;

    public VASTVideoToolbarElement(Configuration configuration) {
        super(configuration.getContext());

        LinearLayout.LayoutParams toolbarLayoutParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT,
                configuration.getWeight());
        toolbarLayoutParams.gravity = configuration.getWidgetGravity();
        setLayoutParams(toolbarLayoutParams);

        DisplayMetrics displayMetrics = configuration.getContext().getResources().getDisplayMetrics();
        mTextPadding = (int)Utils.dipsToPixels(TEXT_PADDING_DIPS, displayMetrics);
        mImagePadding = (int)Utils.dipsToPixels(IMAGE_PADDING_DIPS, displayMetrics);
        mImageSideLength = (int)Utils.dipsToPixels(IMAGE_SIDE_LENGTH_DIPS, displayMetrics);

        setVisibility(configuration.getVisibility());

        if (configuration.hasDrawable()) {
            if (null != configuration.getDrawable()) {
                mImageView = new ImageView(getContext());
                mImageView.setId((int) Utils.generateUniqueId());

                final RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                        mImageSideLength,
                        mImageSideLength);

                iconLayoutParams.addRule(CENTER_VERTICAL);
                iconLayoutParams.addRule(configuration.getDrawableAlign());

                mImageView.setPadding(mImagePadding, mImagePadding, mImagePadding, mImagePadding);

                mImageView.setBackgroundColor(Color.BLACK);
                mImageView.getBackground().setAlpha(0);
                mImageView.setImageDrawable(configuration.getDrawable());
                addView(mImageView, iconLayoutParams);
            }
        }

        if (configuration.hasText()) {
            mTextView = new TextView(getContext());
            mTextView.setSingleLine();
            mTextView.setEllipsize(TextUtils.TruncateAt.END);
            mTextView.setText(configuration.getDefaultText());
            mTextView.setTextColor(Color.GRAY);

            final RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            textLayoutParams.addRule(CENTER_VERTICAL);

            if (mImageView != null) {
                textLayoutParams.addRule(LEFT_OF, mImageView.getId());
            } else {
                textLayoutParams.addRule(configuration.getTextAlign());
            }

            mTextView.setPadding(mTextPadding, mTextPadding, mTextPadding, mTextPadding);

            addView(mTextView, textLayoutParams);
        }

        if (null != configuration.getOnTouchListener()) {
            setOnTouchListener(configuration.getOnTouchListener());
        }
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setImageView(ImageView mImageView) {
        this.mImageView = mImageView;
    }

    public TextView getTextView() {
        return mTextView;
    }

    public void setTextView(TextView mTextView) {
        this.mTextView = mTextView;
    }

    void updateText(final String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    void updateImageText(final String text) {
        try {
            VASTTextDrawable textDrawable = (VASTTextDrawable) mImageView.getDrawable();
            textDrawable.updateText(text);
        } catch (Exception e) {
            LOGGER.warning("Unable to update ToolbarWidget text.");
        }
    }
}
