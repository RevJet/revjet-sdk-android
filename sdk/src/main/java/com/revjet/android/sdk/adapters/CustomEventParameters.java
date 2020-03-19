/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import com.revjet.android.sdk.annotations.NetworkParameter;

public final class CustomEventParameters {
    @NetworkParameter
    public String function;

    @NetworkParameter(required = false)
    public String data;
}
