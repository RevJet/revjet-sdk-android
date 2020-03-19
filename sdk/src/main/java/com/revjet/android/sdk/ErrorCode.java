/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

public enum ErrorCode {
    NO_ERROR(0),
    UNHANDLED_EXCEPTION(1),
    LOAD_NEXT_INTERSTITIAL_ADAPTER_EXCEPTION(2),
    LOAD_NEXT_ADAPTER_EXCEPTION(3),
    LOAD_TAG_EXCEPTION(4),
    MISSING_NETWORK_TYPE(5),
    ADVIEW_TRANSITION_FAILED(6),
    BAD_STATUS_CODE(7),
    EMPTY_NETWORKS_ARRAY(8),
    EMPTY_RESPONSE(9),
    UNKNOWN_NETWORK_TYPE(10),
    NETWORK_INFO_INVALID(11),
    UNKNOWN_AD_TYPE(12),
    UNKNOWN_CONTENT_TYPE(13),
    NO_TAG(14),
    RESUME_AD_EXCEPTION(15),
    PAUSE_AD_EXCEPTION(16);

    private final int mIntCode;

    private ErrorCode(int intCode) {
        mIntCode = intCode;
    }

    public int toInt() {
        return mIntCode;
    }
}
