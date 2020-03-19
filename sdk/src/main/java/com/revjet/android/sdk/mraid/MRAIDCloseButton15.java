/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class MRAIDCloseButton15 extends MRAIDCloseButton {
    @SuppressWarnings("deprecation")
    public MRAIDCloseButton15(Context context) {
        super(context);
        setBackgroundDrawable((Drawable) null);
    }
}
