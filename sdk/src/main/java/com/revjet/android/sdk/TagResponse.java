/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.TagResponseException;

import java.util.List;

public interface TagResponse {
    int TAG_RESPONSE_RESULT_SUCCESS = 0;
    int TAG_RESPONSE_RESULT_FAIL = 1;

    interface TagResponseParseListener {
        void onParse(int result);
    }

    @Nullable
    List<AdNetwork> getNetworks();

    void parse(@Nullable final TagResponseParseListener listener) throws TagResponseException;
}
