/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;

public class TagException extends Exception {
    private static final long serialVersionUID = 1L;

    private final ErrorCode mErrorCode;
    private final TagContext mTagContext;

    public TagException(ErrorCode errorCode, String message, TagContext tagContext) {
        super(message);
        mErrorCode = errorCode;
        mTagContext = tagContext;
    }

    public ErrorCode getErrorCode() {
        return mErrorCode;
    }

    public TagContext getTagContext() {
        return mTagContext;
    }
}
