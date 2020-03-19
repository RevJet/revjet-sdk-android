/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;

public final class InvalidNetworkParameterException extends TagResponseException {
    private static final long serialVersionUID = 1L;

    public InvalidNetworkParameterException(ErrorCode errorCode, String message,
            TagContext tagContext) {
        super(errorCode, message, tagContext);
    }

    public InvalidNetworkParameterException(String message, TagContext tagContext) {
        super(message, tagContext);
    }
}
