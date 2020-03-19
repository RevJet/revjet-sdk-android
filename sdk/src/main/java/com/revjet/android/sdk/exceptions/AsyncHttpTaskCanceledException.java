/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;

public final class AsyncHttpTaskCanceledException extends AsyncHttpTaskException {
    private static final long serialVersionUID = 1L;

    public AsyncHttpTaskCanceledException() {
        super(ErrorCode.NO_ERROR, "Http task has been cancelled", null);
    }
}
