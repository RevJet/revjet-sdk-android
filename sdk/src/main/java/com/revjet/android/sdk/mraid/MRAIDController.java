/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.commons.CustomURLConnection;
import com.revjet.android.sdk.commons.IOUtils;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.mraid.MRAIDView.ViewState;
import com.revjet.android.sdk.mraid.events.MRAIDExpandOrientation;
import com.revjet.android.sdk.mraid.events.MRAIDNativeEvent;
import com.revjet.android.sdk.mraid.events.MRAIDNativeEventFactory;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static android.graphics.Color.TRANSPARENT;
import static com.revjet.android.sdk.ads.AdWebView.WEBVIEW_DID_APPEAR;
import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDController {
    public static final Handler sHandler = new Handler(Looper.getMainLooper());

    private static final Pattern sHtmlTagPattern = Pattern.compile("(?mi)<html([>]|([\\s]+[^>]*[>])|(/>))");
    private static final Pattern sHeadTagPattern = Pattern.compile("(?mi)<head([>]|([\\s]+[^>]*[>])|(/>))");
    private static final Pattern sBodyTagPattern = Pattern.compile("(?mi)<body([>]|([\\s]+[^>]*[>])|(/>))");

    @Nullable private Activity mActivity;
    @Nullable private Integer mOriginalActivityOrientation;
    private boolean mAllowOrientationChange = true;
    @NonNull private MRAIDExpandOrientation mForceOrientation = MRAIDExpandOrientation.NONE;

    private boolean mUrlLoading = false;
    private boolean mUseCustomClose = false;
    private boolean mViewable;
    private boolean mDidAppear = false;
    @NonNull private ViewState mViewState = ViewState.HIDDEN;
    @NonNull private MRAIDPlacementType mPlacementType = MRAIDPlacementType.INLINE;

    public int mScreenWidth = -1;
    public int mScreenHeight = -1;

    @Nullable private FrameLayout mMainView;
    private int mViewIndexInParent;
    @NonNull private final FrameLayout mPlaceholderView;
    @NonNull private final FrameLayout mAdContainerLayout;
    @NonNull private final RelativeLayout mExpansionLayout;

    @NonNull private final MRAIDCloseButton mCloseButton;

    private float mDensity;

    @NonNull private final WeakReference<Context> mContextRef;
    @NonNull private final WeakReference<MRAIDView> mMraidViewRef;

    public enum MRAIDPlacementType {
        INLINE, MRAIDPlacementType, INTERSTITIAL
    }

    public MRAIDController(@NonNull final Context context, @NonNull final MRAIDView mraidView) {
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }

        mContextRef = new WeakReference<>(context);
        mMraidViewRef = new WeakReference<>(mraidView);
        mViewable = (mraidView.getVisibility() == View.VISIBLE);
        setViewState(ViewState.LOADING);
        initializeScreenSize();

        mAdContainerLayout = createAdContainerLayout();
        mExpansionLayout = createExpansionLayout();
        mPlaceholderView = createPlaceholderView();

        mCloseButton = new MRAIDCloseButton(context);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeExpandedView();
            }
        });
    }

    public void destroy() {
        if (null != mMainView) {
            resetViewToDefaultState();
        }
    }

    @Nullable
    public Context getContext() {
        return mContextRef.get();
    }

    public void setPlacementType(@NonNull MRAIDPlacementType placementType) {
        mPlacementType = placementType;
    }

    public boolean isInterstitial() {
        return (mPlacementType == MRAIDPlacementType.INTERSTITIAL);
    }

    public void closeExpandedView() {
        unApplyOrientation();

        MRAIDView mraidView = getMraidView();

        if (getViewState() == ViewState.EXPANDED) {
            resetViewToDefaultState();
            setViewState(ViewState.DEFAULT);
            fireChangeViewStateJS();
        } else if (getViewState() == ViewState.DEFAULT) {
            if (mraidView != null) {
                mraidView.setVisibility(View.INVISIBLE);
            }
            setViewState(ViewState.HIDDEN);
            fireChangeViewStateJS();
        }

        if (mraidView != null) {
            MRAIDViewListener listener = mraidView.getListener();
            if (null != listener) {
                listener.onClose(mraidView);
            }
        }
    }

    public void startActivityWithUrl(@NonNull String url) {
        Context context = getContext();

        if (context != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public void showHideCloseButton() {
        Context context = getContext();
        if (isUseCustomClose() && context != null) {
            final float closeButtonSize = Utils.dipsToPixels(MRAIDCloseButton.BUTTON_SIZE,
                context.getResources().getDisplayMetrics());
            FrameLayout.LayoutParams buttonLayout = new FrameLayout.LayoutParams(
                    Math.round(closeButtonSize), Math.round(closeButtonSize), Gravity.RIGHT);
            mAdContainerLayout.addView(mCloseButton, buttonLayout);
        } else {
            mAdContainerLayout.removeView(mCloseButton);
        }
    }

    @Nullable
    public MRAIDView getMraidView() {
        return mMraidViewRef.get();
    }

    @NonNull
    public ViewState getViewState() {
        return mViewState;
    }

    public void setViewState(@NonNull ViewState state) {
        mViewState = state;
    }

    @NonNull
    public String prepareHtml(@NonNull String html) {
        boolean htmlTagPresent = sHtmlTagPattern.matcher(html).find();
        boolean headTagPresent = sHeadTagPattern.matcher(html).find();
        boolean bodyTagPresent = sBodyTagPattern.matcher(html).find();

        if (!htmlTagPresent && !headTagPresent && !bodyTagPresent) {
            html = convertToHtml(html);
        }

        return insertMRAIDScript(html);
    }

    @NonNull
    private String convertToHtml(@NonNull String html) {
        return "<html><head>\n"
                + "<meta name=\"viewport\" content=\"initial-scale=1.0,user-scalable=no\"/></head>\n"
                + "<body style=\"background-color: transparent; margin: 0; padding: 0; overflow: hidden;\">\n"
                + html + "\n" + "</body></html>";
    }

    public boolean isUseCustomClose() {
        return mUseCustomClose;
    }

    public void setUseCustomClose(boolean useCustomClose) {
        mUseCustomClose = useCustomClose;
    }

    @NonNull
    private String insertMRAIDScript(@NonNull String html) {
        // If the string data lacks the HTML boilerplate, add it.
        if (!html.contains("<html")) {
            html = "<html><head></head><body style='margin:0;padding:0;'>" + html +
                    "</body></html>";
        }

        // Inject the MRAID JavaScript bridge.
        html = html.replace("<head>", "<head><script>" + MRAIDJavascript.JAVASCRIPT_SOURCE + "</script>");

        return html;
    }

    public void setDefaultExpandProperties() {
        LOGGER.info("setDefaultExpandProperties");

        evalJS("mraid.setExpandProperties({" + "width: " + mScreenWidth + ", "
                + "height: " + mScreenHeight + " });");
    }

    public void setPlacementType() {
        LOGGER.info("setPlacementType");
        evalJS("window.mraidbridge.fireChangeEvent({placementType: '" + mPlacementType.toString().toLowerCase() + "'});");
    }

    @NonNull
    public MRAIDPlacementType getPlacementType() {
        return mPlacementType;
    }

    public synchronized void ready() {
        LOGGER.info("ready");

        evalJS("window.mraidbridge.fireReadyEvent();");

        if (!mViewable) {
            MRAIDView mraidView = getMraidView();
            if (mraidView != null && mraidView.isWindowVisible()) {
                fireViewableChangeJS();
            }
        }
    }

    public void initializeMRAIDView() {
        String initializeScript = "screenSize: { width: " + mScreenWidth + ", height: " + mScreenHeight + " }";
        initializeScript += ", viewable: '" + mViewable + "'";
        evalJS("window.mraidbridge.fireChangeEvent({" + initializeScript + "});");

        setViewState(ViewState.DEFAULT);
        fireChangeViewStateJS();

        Context context = getContext();
        if (context != null) {
            String supportScript = "supports: {sms: " + Utils.isSmsAvailable(context) +
                ", tel: " + Utils.isTelAvailable(context) +
                ", calendar: " + Utils.isCalendarAvailable(context) +
                ", storePicture: " + Utils.isStorePictureAvailable(context) +
                ", inlineVideo: " + Utils.isInlineVideoAvailable(context) +
                "}";
            evalJS("window.mraidbridge.fireChangeEvent({" + supportScript + "});");
        }
    }

    public synchronized void fireViewableChangeJS() {
        MRAIDView mraidView = getMraidView();
        if (mraidView != null) {
            mViewable = mraidView.getVisibility() == View.VISIBLE;
            evalJS("window.mraidbridge.fireChangeEvent({viewable: '" + mViewable + "'});");

            if (!mDidAppear) {
                mDidAppear = true;
                LOGGER.info("webviewDidAppear");
                evalJS(WEBVIEW_DID_APPEAR);

                MRAIDViewListener listener = mraidView.getListener();
                if (null != listener) {
                    listener.onShowAd(mraidView);
                }
            }
        }
    }

    public void fireNativeEventComplete(@NonNull String eventName) {
        evalJS("window.mraidbridge.nativeCallComplete('" + eventName + "');");
    }

    public void reportError(@NonNull String msg, @NonNull String action) {
        LOGGER.info("Reporting error: " + msg + " (" + action + ")");
        evalJS("window.mraidbridge.fireErrorEvent(\"" + msg + "\", \"" + action + "\");");
    }

    public void callNativeEvent(@NonNull URI uri) {
        String eventName = uri.getHost();

        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(uri.toString());
        List<UrlQuerySanitizer.ParameterValuePair> list = sanitizer.getParameterList();
        Map<String, String> parameters = new HashMap<>();
        for (UrlQuerySanitizer.ParameterValuePair pair : list) {
            parameters.put(pair.mParameter, pair.mValue);
        }

        MRAIDNativeEvent nativeEvent = MRAIDNativeEventFactory.createEvent(eventName);
        if (null != nativeEvent) {
            nativeEvent.execute(parameters, this);
            fireNativeEventComplete(eventName);
        } else {
            reportError("Native event not found", eventName);
        }
    }

    @Nullable
    private String httpGet(@NonNull String url, @Nullable String userAgent) {
        String responseBody = null;
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = CustomURLConnection.openConnection(url);

            if (userAgent != null) {
                urlConnection.setRequestProperty("User-Agent", userAgent);
            }

            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Bad status");
            }

            responseBody = IOUtils.toString(urlConnection.getInputStream(), "UTF-8");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading content from URL", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return responseBody;
    }

    private void fireChangeViewStateJS() {
        String stateString = getViewState().toString().toLowerCase();
        evalJS("window.mraidbridge.fireChangeEvent({state: '" + stateString + "'});");
    }

    public void evalJS(@NonNull String script) {
        MRAIDView mraidView = getMraidView();
        if (mraidView != null) {
            mraidView.evaluateJavaScriptString(script);
        }
    }

    private void initializeScreenSize() {
        Context context = getContext();
        if (context != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            mDensity = dm.density;
            mScreenWidth = Math.round(dm.widthPixels / mDensity);
            mScreenHeight = Math.round(dm.heightPixels / mDensity);

            LOGGER.info("mScreenWidth: " + mScreenWidth);
            LOGGER.info("mScreenHeight: " + mScreenHeight);
        }
    }

    public void expandToSize(
      @Nullable final String url,
      final int width,
      final int height,
      final boolean shouldUseCustomClose) {
        MRAIDView mraidView = getMraidView();
        if (mraidView == null) {
            return;
        }

        mMainView = (FrameLayout) mraidView.getRootView().findViewById(android.R.id.content);
        if (null == mMainView) {
            return;
        }

        applyOrientation();

        setUseCustomClose(shouldUseCustomClose);
        swapViewWithPlaceholderView();

        if (null != url) {
            WebView webView = mraidView.getWebView();
            final String userAgent = webView != null ? webView.getSettings().getUserAgentString() : null;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mUrlLoading) {
                        mUrlLoading = true;

                        final String responseBody = httpGet(url, userAgent);
                        if (responseBody != null) {
                            sHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    MRAIDView mraidView = getMraidView();
                                    if (mraidView != null) {
                                        mraidView.expandToSizeWithContent(responseBody, width, height);
                                    }
                                }
                            });
                        }

                        mUrlLoading = false;
                    }
                }
            }).start();
        } else {
            expandToSize(mraidView, width, height);
        }
    }

    public void expandToSize(@NonNull final View expansionContentView, final int width, final int height) {
        if (expansionContentView.getParent() == null) {
            expandLayouts(expansionContentView, (int) (width * mDensity), (int) (height * mDensity));
            if (null != mMainView) {
                mMainView.addView(mExpansionLayout, new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                showHideCloseButton();
                setViewState(ViewState.EXPANDED);
                fireChangeViewStateJS();
                MRAIDView mraidView = getMraidView();
                if (mraidView != null) {
                    MRAIDViewListener listener = mraidView.getListener();
                    if (null != listener) {
                        listener.onExpand(getMraidView());
                    }
                }
            }
        }
    }

    public void handleSetOrientationProperties(
      final boolean allowOrientationChange,
      @NonNull final MRAIDExpandOrientation forceOrientation) {
        if (!shouldAllowForceOrientation(forceOrientation)) {
            LOGGER.info("Unable to force orientation to " + forceOrientation.toString());
            return;
        }

        mAllowOrientationChange = allowOrientationChange;
        mForceOrientation = forceOrientation;

        if (getViewState() == ViewState.EXPANDED || mPlacementType == MRAIDPlacementType.INTERSTITIAL) {
            applyOrientation();
        }
    }

    private void unApplyOrientation() {
        if (mActivity != null && mOriginalActivityOrientation != null) {
            mActivity.setRequestedOrientation(mOriginalActivityOrientation);
        }
        mOriginalActivityOrientation = null;
    }

    private void applyOrientation() {
        if (mForceOrientation == MRAIDExpandOrientation.NONE) {
            if (mAllowOrientationChange) {
                unApplyOrientation();
            } else {
                if (mActivity == null) {
                    LOGGER.info("Unable to set MRAID expand orientation to " +
                            "'none'; expected passed in Activity Context.");
                    return;
                }
                lockOrientation(Utils.getScreenOrientation(mActivity));
            }
        } else {
            lockOrientation(mForceOrientation.getActivityInfoOrientation());
        }
    }

    private void lockOrientation(final int screenOrientation){
        if (mActivity == null || !shouldAllowForceOrientation(mForceOrientation)) {
            LOGGER.info("Attempted to lock orientation to unsupported value: " +
                    mForceOrientation.name());
            return;
        }

        if (mOriginalActivityOrientation == null) {
            mOriginalActivityOrientation = mActivity.getRequestedOrientation();
        }

        mActivity.setRequestedOrientation(screenOrientation);
    }

    private boolean shouldAllowForceOrientation(final MRAIDExpandOrientation newOrientation) {
        if (newOrientation == MRAIDExpandOrientation.NONE) {
            return true;
        }

        if (null == mActivity) {
            return false;
        }

        final ActivityInfo activityInfo;
        try {
            activityInfo = mActivity.getPackageManager().getActivityInfo(
                    new ComponentName(mActivity, mActivity.getClass()), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        final int activityOrientation = activityInfo.screenOrientation;
        if (activityOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            return activityOrientation == newOrientation.getActivityInfoOrientation();
        }

        boolean containsNecessaryConfigChanges =
                Utils.bitMaskContainsFlag(activityInfo.configChanges, CONFIG_ORIENTATION);

        containsNecessaryConfigChanges = containsNecessaryConfigChanges
            && Utils.bitMaskContainsFlag(activityInfo.configChanges, CONFIG_SCREEN_SIZE);

        return containsNecessaryConfigChanges;
    }

    @NonNull
    private FrameLayout createAdContainerLayout() {
        return new FrameLayout(getContext());
    }

    @NonNull
    private RelativeLayout createExpansionLayout() {
        return new RelativeLayout(getContext());
    }

    @NonNull
    private FrameLayout createPlaceholderView() {
        return new FrameLayout(getContext());
    }

    private void expandLayouts(
      @NonNull final View expansionContentView,
      final int expandWidth,
      final int expandHeight) {
        View dimmingView = new View(getContext());
        dimmingView.setBackgroundColor(TRANSPARENT);
        dimmingView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mExpansionLayout.addView(dimmingView, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));

        mAdContainerLayout.addView(expansionContentView, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT));

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(expandWidth, expandHeight);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mExpansionLayout.addView(mAdContainerLayout, lp);
    }

    private void swapViewWithPlaceholderView() {
        MRAIDView mraidView = getMraidView();
        if (mraidView == null) return;

        ViewGroup parent = (ViewGroup) mraidView.getParent();
        if (parent == null) return;

        int index;
        int count = parent.getChildCount();
        for (index = 0; index < count; index++) {
            if (parent.getChildAt(index) == getMraidView()) break;
        }

        mViewIndexInParent = index;
        if (mPlaceholderView.getParent() == null) {
            parent.addView(mPlaceholderView, index,
                    new ViewGroup.LayoutParams(getMraidView().getWidth(), getMraidView().getHeight()));
            parent.removeView(getMraidView());
        }
    }

    private void resetViewToDefaultState() {
        setUseCustomClose(false);
        showHideCloseButton();
        mAdContainerLayout.removeAllViewsInLayout();

        mExpansionLayout.removeAllViewsInLayout();

        if (mMainView != null) {
            mMainView.removeView(mExpansionLayout);
        }

        MRAIDView mraidView = getMraidView();
        if (mraidView == null) return;

        mraidView.requestLayout();

        ViewGroup parent = (ViewGroup) mPlaceholderView.getParent();
        if (null != parent) {
            parent.addView(mraidView, mViewIndexInParent);
            parent.removeView(mPlaceholderView);
            parent.invalidate();
        }
    }
}
