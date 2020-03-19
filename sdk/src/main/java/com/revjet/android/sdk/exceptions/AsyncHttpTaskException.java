/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;

public class AsyncHttpTaskException extends TagException {
    private static final long serialVersionUID = 1L;

    public AsyncHttpTaskException(ErrorCode errorCode, String message, TagContext tagContext) {
        super(errorCode, message, tagContext);
    }

    public AsyncHttpTaskException(String message, TagContext tagContext) {
        this(ErrorCode.LOAD_TAG_EXCEPTION, message, tagContext);
    }
}
