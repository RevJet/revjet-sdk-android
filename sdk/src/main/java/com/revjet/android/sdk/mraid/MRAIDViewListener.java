/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import androidx.arch.core.util.Function;

public interface MRAIDViewListener {
    void onReceiveAd(MRAIDView view);

    void onShowAd(MRAIDView view);

    void onFailedToReceiveAd(MRAIDView view);

    void onLeaveApplication(MRAIDView view);

    void onClick(MRAIDView view);

    void shouldOpenURL(MRAIDView view, String url, Function<Boolean, Void> callback);

    void onExpand(MRAIDView view);

    void onClose(MRAIDView view);
}
