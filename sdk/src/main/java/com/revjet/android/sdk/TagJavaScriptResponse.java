/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.TagResponseException;

import java.util.List;

public final class TagJavaScriptResponse extends AbstractTagResponse {
    @Nullable private final TagJsonResponse mTagJsonResponse;

    public TagJavaScriptResponse(@NonNull final TagContext tagContext, @NonNull final String responseBody) {
        super(tagContext);

        String jsonpCallback = "REVJET_SDK(";
        if (responseBody.startsWith(jsonpCallback)) {
            String jsonResponseBody = responseBody.substring(jsonpCallback.length(), responseBody.length() - 1);
            mTagJsonResponse = new TagJsonResponse(tagContext, jsonResponseBody);
        } else {
            mTagJsonResponse = null;
        }
    }

    @Override
    public void parse(@Nullable final TagResponseParseListener listener) throws TagResponseException {
        if (mTagJsonResponse != null) {
            mTagJsonResponse.parse(listener);
        } else {
            throw new TagResponseException("Invalid JSONP response", getTagContext());
        }
    }

    @Nullable
    @Override
    public List<AdNetwork> getNetworks() {
        if (mTagJsonResponse != null) {
            return mTagJsonResponse.getNetworks();
        } else {
            return null;
        }
    }
}
