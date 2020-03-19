/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import com.revjet.android.sdk.exceptions.AdapterException;

public interface AdapterListener {
    void onReceiveCustomMethod(@NonNull String method, @NonNull String data) throws AdapterException;

    void onDestroyCustomEventAdapter(@NonNull String method);
}
