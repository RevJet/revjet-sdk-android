/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import com.revjet.android.sdk.exceptions.AdapterException;

public interface AdapterController {
    void setAdapter(Adapter<?> adapter);

    void requestAd() throws AdapterException;

    void showAd();

    void onRequestAdFailed();

    void onDestroy();

    void onNotResponding();
}
