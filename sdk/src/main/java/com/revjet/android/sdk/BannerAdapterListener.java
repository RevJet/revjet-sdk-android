/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

public interface BannerAdapterListener extends AdapterListener {
    void onReceiveAd(@NonNull View view);

    void onShowAd(@NonNull View view);

    void onFailedToReceiveAd(@Nullable View view);

    void onPresentScreen(@NonNull View view);

    void onDismissScreen(@NonNull View view);

    void onLeaveApplication(@NonNull View view);

    void onClick(@NonNull View view);

    boolean shouldOpenURL(@NonNull View view, @NonNull String url);

    void onClose(@NonNull View view);
}
