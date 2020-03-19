/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;

public class TagResponseException extends TagException {
    private static final long serialVersionUID = 1L;

    public TagResponseException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public TagResponseException(String message, TagContext tagContext) {
        this(ErrorCode.NETWORK_INFO_INVALID, message, tagContext);
    }

    public TagResponseException(ErrorCode errorCode, String message, TagContext tagContext) {
        super(errorCode, message, tagContext);
    }
}
