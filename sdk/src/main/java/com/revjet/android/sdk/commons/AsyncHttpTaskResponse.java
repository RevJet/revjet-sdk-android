/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import java.util.List;
import java.util.Map;

public class AsyncHttpTaskResponse {
    private final String mResponseBody;
    private final String mContentType;
    private final Map<String, List<String>> mResponseHeaders;

    public AsyncHttpTaskResponse(String responseBody, String contentType, Map<String, List<String>> responseHeaders) {
        mResponseBody = responseBody;
        mContentType = contentType;
        mResponseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return mResponseBody;
    }

    public String getContentType() {
        return mContentType;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    public String getResponseHeader(String headerName) {
        if (mResponseHeaders == null) {
            return null;
        }

        for (String name : mResponseHeaders.keySet()) {
            if (name != null && name.equalsIgnoreCase(headerName)) {
                List<String> value = mResponseHeaders.get(name);
                if (value != null) {
                    return value.get(0);
                }
            }
        }

        return null;
    }
}
