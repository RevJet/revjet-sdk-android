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

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MRAIDCloseButton16 extends MRAIDCloseButton {
    public MRAIDCloseButton16(Context context) {
        super(context);
        setBackground((Drawable) null);
    }
}
