/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

import com.revjet.android.sdk.TagController.LoadingState;
import com.revjet.android.sdk.ads.AdSize;
import com.revjet.android.sdk.commons.Utils;

import java.util.Map;

/**
 * A view that shows banner ads inside of it. Also, this view is responsible for
 * displaying interstitial ads, which can interleave with other banner ads.
 */
public final class TagView extends FrameLayout {
    @Nullable private AdSize mTagSize;
    private boolean mVisible = true;

    @Nullable private final TagController mTagController;
    @Nullable private final TagViewPlaceholder mTagViewPlaceholder;

    {
        // If the view is in edit mode then it means that a user opened the
        // Android XML Graphical Layout viewer. In that case ads will not be
        // displayed.

        if (isInEditMode()) {
            mTagController = null;
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            mTagViewPlaceholder = new TagViewPlaceholder(displayMetrics);
            setWillNotDraw(false);
        } else {
            mTagController = new TagController(TagType.NORMAL, getContext());
            mTagController.setTagView(this);
            mTagViewPlaceholder = null;
        }
    }

    public TagView(Context context) {
        super(context);
    }

    public TagView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
            attrs, R.styleable.TagView, 0, 0);

        try {
            setAutoRefreshEnabled(attributes.getBoolean(R.styleable.TagView_autoRefresh, false));
            setTagUrl(attributes.getString(R.styleable.TagView_tagUrl));

            int tagWidth = attributes.getInteger(R.styleable.TagView_tagWidth, 0);
            int tagHeight = attributes.getInteger(R.styleable.TagView_tagHeight, 0);

            if (tagWidth > 0 && tagHeight > 0) {
                setTagSize(AdSize.findAdSizeThatFits(tagWidth, tagHeight));
            }
        } finally {
            attributes.recycle();
        }
    }

    /**
     * When in edit mode draws a stub instead of displaying ads.
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTagViewPlaceholder != null) {
            mTagViewPlaceholder.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mTagViewPlaceholder != null) {
            mTagViewPlaceholder.setSize(w, h);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (mTagController != null) {
            if (!mVisible && visibility == VISIBLE) {
                mTagController.resumeTag();
                mVisible = true;
            } else if (mVisible && (visibility == GONE || visibility == INVISIBLE)) {
                mTagController.pauseTag();
                mVisible = false;
            }
        }
    }

    /**
     * Destroys any resources associated with this view. Call this method when
     * the view is no longer needed.
     */
    public void destroy() {
        LOGGER.info("Destroy");

        setAutoRefreshEnabled(false);
        cleanup();

        if (mTagController != null) {
            mTagController.destroy();
        }
    }

    public void setTagUrl(@Nullable String tagUrl) {
        if (mTagController != null) {
            mTagController.setTagUrl(tagUrl);
        }
    }

    @Nullable
    public String getTagUrl() {
        return (mTagController != null) ? mTagController.getTagUrl() : null;
    }

    /**
     * Pause the slot view. No new ads will be loaded. It is recommended to call
     * this methods when the slot view is obscured by another view or a window.
     */
    public void pause() {
        LOGGER.info("Pause");

        if (mTagController != null) {
            mTagController.pauseTag();
        }
    }

    /**
     * Resume loading new ads.
     *
     * @see #pause()
     */
    public void resume() {
        LOGGER.info("Resume");

        if (mTagController != null) {
            mTagController.resumeTag();
        }
    }

    /**
     * Load and display a new ad.
     */
    public synchronized void loadAd() {
        if (mTagController != null) {
            mTagController.loadTag(true);
        }
    }

    /**
     * Fetch a new ad. You need to call {@link #showAd()} method in order to
     * display the fetched ad.
     * <p>
     * To determine whether an ad is successfully loaded use
     * {@link TagListener} methods:
     * {@link TagListener#onReceiveAd(BannerAdapter, View)} and/or to
     * {@link TagListener#onReceiveInterstitialAd(InterstitialAdapter, Object)}.
     *
     * @see #showAd()
     * @see #setListener(TagListener)
     */
    public synchronized void fetchAd() {
        if (mTagController != null) {
            mTagController.loadTag(false);
        }
    }

    /**
     * Display an ad. The ad must be fetched before it can be displayed.
     *
     * @see #fetchAd()
     */
    public void showAd() {
        if (mTagController != null) {
            mTagController.showTag();
        }
    }

    public void setTagSize(@Nullable AdSize tagSize) {
        mTagSize = tagSize;
    }

    @Nullable
    public AdSize getTagSize() {
        return mTagSize;
    }

    /**
     * Returns slot view width in dips.
     */
    public float getWidthInDips() {
        if (mTagSize != null) {
            return mTagSize.getWidth();
        } else {
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            return Utils.pixelsToDips(getWidth(), displayMetrics);
        }
    }

    /**
     * Returns slot view height in dips.
     */
    public float getHeightInDips() {
        if (mTagSize != null) {
            return mTagSize.getHeight();
        } else {
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            return Utils.pixelsToDips(getHeight(), displayMetrics);
        }
    }

    @Nullable
    public TagListener getListener() {
        return (mTagController != null) ? mTagController.getTagListener() : null;
    }

    /**
     * Register a listener to be notified about various slot events.
     *
     * @see TagListener
     * @see #getListener()
     */
    public void setListener(TagListener listener) {
        if (mTagController != null) {
            mTagController.setTagListener(listener);
        }
    }

    @Nullable
    public TagTargeting getTargeting() {
        return (mTagController != null) ? mTagController.getTargeting() : null;
    }

    /**
     * Slot targeting can improve the quality of ads and increase your revenue
     * (All fields are optional).
     */
    public void setTargeting(TagTargeting targeting) {
        if (mTagController != null) {
            mTagController.setTargeting(targeting);
        }
    }

    @Nullable
    public Map<String, String> getAdditionalInfo() {
        return (mTagController != null) ? mTagController.getAdditionalInfo() : null;
    }

    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        if (mTagController != null) {
            mTagController.setAdditionalInfo(additionalInfo);
        }
    }

    public boolean isAutoRefreshEnabled() {
        return mTagController != null &&
                mTagController.isAutoRefreshEnabled();
    }

    /**
     * Automatically reload the slot view after a certain period of time. Each
     * ad may have its own display duration. By default auto refresh is set to
     * <code>true</code>.
     */
    public void setAutoRefreshEnabled(boolean autoRefreshEnabled) {
        if (mTagController != null) {
            mTagController.setAutoRefreshEnabled(autoRefreshEnabled);
        }
    }

    /**
     * Returns <code>true</code> if the ad is ready to be displayed. Makes sense
     * only in conjunction with {@link #fetchAd()} method.
     */
    public boolean isAdReady() {
        return mTagController != null &&
                (mTagController.getLoadingState() == LoadingState.LOADED);
    }

    public void setShowCloseButton(boolean showCloseButton) {
        if (mTagController != null) {
            mTagController.setShowCloseButton(showCloseButton);
        }
    }

    public void cleanup() {
        removeAllViews();
    }

    public void transitionToView(@Nullable View view, @Nullable TransitionAnimation animation) {
        cleanup();

        if (view != null) {
            addView(view);
        }
    }

    public void setIntegrationType(IntegrationType integrationType) {
        if (mTagController != null) {
            mTagController.setIntegrationType(integrationType);
        }
    }
}
