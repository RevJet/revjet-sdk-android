/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

public interface AdListener {
    void onReceiveAd(@NonNull AdView view);

    void onShowAd(@NonNull AdView view);

    void onFailedToReceiveAd(@Nullable AdView view, @Nullable String errorMessage);

    void onPresentScreen(@NonNull AdView view);

    void onDismissScreen(@NonNull AdView view);

    void onLeaveApplication(@NonNull AdView view);

    void onClick();

    void shouldOpenURL(@NonNull AdView view, @NonNull String url, @NonNull Function<Boolean, Void> callback);

    void onClose();
}
