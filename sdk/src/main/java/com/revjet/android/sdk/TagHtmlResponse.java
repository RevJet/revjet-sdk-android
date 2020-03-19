/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.exceptions.TagResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TagHtmlResponse extends AbstractTagResponse {
    public static final Pattern META_PATTERN =
        Pattern.compile("<meta\\s+([^>]+)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public static final Pattern NAME_PATTERN =
        Pattern.compile("name\\s*=\\s*\"Parameter-([^>\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public static final Pattern CONTENT_PATTERN =
        Pattern.compile("content\\s*=\\s*\"([^>\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @NonNull private final String mResponseBody;

    public TagHtmlResponse(@NonNull final TagContext tagContext, @NonNull final String responseBody) {
        super(tagContext);
        mResponseBody = responseBody;
    }

    @Override
    public void parse(@Nullable final TagResponseParseListener listener) throws TagResponseException {
        Map<String, String> metaTags = parseMetaTags(mResponseBody);

        String networkType = metaTags.get("NETWORKTYPE");
        if (networkType == null || networkType.length() == 0) {
            networkType = "RJ";
        }

        Map<String, String> networkObject = new HashMap<>(metaTags);
        String responseBody = replaceWithMacros(mResponseBody);
        networkObject.put("CONTENT", responseBody);

        List<AdNetwork> networks = new ArrayList<>();
        networks.add(createNetworkInstanceFromMap(networkObject, networkType, networkObject));

        setNetworks(networks);

        if (listener != null) {
            listener.onParse(TAG_RESPONSE_RESULT_SUCCESS);
        }
    }

    @NonNull
    private Map<String, String> parseMetaTags(@NonNull final String responseBody) {
        Map<String, String> metaTags = new HashMap<>();

        Matcher metaMatcher = META_PATTERN.matcher(responseBody);
        while (metaMatcher.find()) {
            String metaString = metaMatcher.group(1);

            String nameString = null;
            Matcher nameMatcher = NAME_PATTERN.matcher(metaString);
            if (nameMatcher.find()) {
                nameString = nameMatcher.group(1);
            }

            String contentString = null;
            Matcher contentMatcher = CONTENT_PATTERN.matcher(metaString);
            if (contentMatcher.find()) {
                contentString = contentMatcher.group(1);
            }

            if (nameString != null && contentString != null) {
                metaTags.put(nameString.toUpperCase(), contentString);
            }
        }

        return metaTags;
    }

    @NonNull
    private String replaceWithMacros(@NonNull final String responseBody) {
        TagContext tagContext = getTagContext();

        Map<String, String> macros = Utils.getMapWithQueryParams(tagContext.getTargeting(),
            tagContext.getDeviceInfo(), tagContext.getIntegrationType(), tagContext.getContext());

        Map<String, String> additionalInfo = tagContext.getAdditionalInfo();
        if (additionalInfo != null) {
            macros.putAll(additionalInfo);
        }

        String replacedResponse = responseBody;
        for (String macroName : macros.keySet()) {
            String macroValue = macros.get(macroName);
            if (macroValue != null) {
                replacedResponse = replacedResponse.replace("{" + macroName + "}", macroValue);
            }
        }

        return replacedResponse;
    }
}
