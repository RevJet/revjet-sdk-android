/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import com.revjet.android.sdk.AdType;
import com.revjet.android.sdk.ErrorCode;
import com.revjet.android.sdk.TagContext;

public class AdapterException extends Exception {
    private static final long serialVersionUID = 1L;

    private ErrorCode mErrorCode;
    private TagContext mTagContext;

    public AdapterException(String message) {
        super(message);
    }

    public TagContext getTagContext() {
        return mTagContext;
    }

    public void setTagContext(TagContext tagContext) {
        mTagContext = tagContext;
    }

    public ErrorCode getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        mErrorCode = errorCode;
    }

    public void setErrorCode(AdType adType) {
        mErrorCode = getErrorCode(adType);
    }

    private ErrorCode getErrorCode(AdType adType) {
        return (adType == AdType.INTERSTITIAL ? ErrorCode.LOAD_NEXT_INTERSTITIAL_ADAPTER_EXCEPTION
                : ErrorCode.LOAD_NEXT_ADAPTER_EXCEPTION);
    }
}
