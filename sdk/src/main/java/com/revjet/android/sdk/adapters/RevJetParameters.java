/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.adapters;

import com.revjet.android.sdk.annotations.NetworkParameter;

public final class RevJetParameters {
    @NetworkParameter
    public String content;

    @NetworkParameter
    public String width;

    @NetworkParameter
    public String height;

    @NetworkParameter(required = false)
    public String delivery_method; // TODO: remove

    @NetworkParameter(required = false)
    public String close_button; // TODO: remove

    @NetworkParameter(required = false)
    public String autoscale; // TODO: remove
}
