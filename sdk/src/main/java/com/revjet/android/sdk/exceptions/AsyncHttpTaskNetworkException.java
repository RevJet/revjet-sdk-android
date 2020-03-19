/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;

public class AsyncHttpTaskNetworkException extends AsyncHttpTaskException {
    private static final long serialVersionUID = 1L;

    public AsyncHttpTaskNetworkException(String message) {
        super(ErrorCode.NO_ERROR, "Network error: " + message, null);
    }
}
