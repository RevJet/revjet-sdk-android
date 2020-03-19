/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.commons.AsyncHttpTaskResponse;
import com.revjet.android.sdk.exceptions.InvalidNetworkParameterException;
import com.revjet.android.sdk.exceptions.TagResponseException;

import java.lang.reflect.Constructor;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractTagResponse implements TagResponse {
    public static final String sPropertyNetworks = "networks";
    public static final String sPropertyNetworkType = "network_type";
    public static final String sPropertyDeliveryMethod = "delivery_method";
    public static final String sPropertyTransitionAnimation = "transition_animation";
    public static final String sPropertyRefreshRate = "refreshTime";
    public static final String sPropertyImpressionUrl = "impression_url";
    public static final String sPropertyNoBidUrl = "nobid_url";
    public static final String sPropertyClickUrl = "click_url";
    public static final String sPropertyParameters = "parameters";

    private static final Map<ResponseType, Class<? extends TagResponse>> sResponseTypeMap;
    static {
        sResponseTypeMap = new EnumMap<>(ResponseType.class);
        sResponseTypeMap.put(ResponseType.JSON, TagJsonResponse.class);
        sResponseTypeMap.put(ResponseType.HTML, TagHtmlResponse.class);
        sResponseTypeMap.put(ResponseType.JAVASCRIPT, TagJavaScriptResponse.class);
        //sResponseTypeMap.put(ResponseType.XML, TagXmlResponse.class);
    }

    private enum ResponseType {
        JSON, HTML, JAVASCRIPT /*, XML*/
    }

    @NonNull private final TagContext mTagContext;
    @Nullable private List<AdNetwork> mNetworks;

    public AbstractTagResponse(@NonNull final TagContext tagContext) {
        mTagContext = tagContext;
    }

    @NonNull
    public static TagResponse newInstance(
      @NonNull final TagContext tagContext,
      @NonNull final AsyncHttpTaskResponse httpResponse)
        throws TagResponseException {
        String responseBody = httpResponse.getResponseBody();
        String contentType = httpResponse.getContentType();

        if (responseBody == null || contentType == null) {
            throw new TagResponseException(ErrorCode.EMPTY_RESPONSE, "Empty response", tagContext);
        }

        ResponseType responseType = getResponseType(contentType);

        if (responseType == null) {
            throw new TagResponseException(ErrorCode.UNKNOWN_CONTENT_TYPE,
                    "Unknown Content-Type: " + contentType, tagContext);
        }

        Class<? extends TagResponse> tagResponseClass = sResponseTypeMap.get(responseType);

        try {
            Constructor<? extends TagResponse> constructor = tagResponseClass.getConstructor(
                    TagContext.class, String.class);
            return constructor.newInstance(tagContext, responseBody);
        } catch (Exception e) {
            throw new TagResponseException(ErrorCode.EMPTY_RESPONSE,
                    "Error instantiating TagResponse class: " + e.getMessage(), tagContext);
        }
    }

    @Nullable
    private static ResponseType getResponseType(@Nullable String contentType) {
        ResponseType responseType = null;

        if (contentType != null) {
            if (contentType.contains("json")) {
                responseType = ResponseType.JSON;
            } else if (contentType.contains("html")) {
                responseType = ResponseType.HTML;
            } else if (contentType.contains("javascript")) {
                responseType = ResponseType.JAVASCRIPT;
            }/* else if (contentType.contains("xml")) {
                responseType = ResponseType.XML;
            }*/
        }

        return responseType;
    }

    @Nullable
    @Override
    public List<AdNetwork> getNetworks() {
        return mNetworks;
    }

    protected void setNetworks(@Nullable List<AdNetwork> networks) {
        mNetworks = networks;
    }

    @NonNull
    protected TagContext getTagContext() {
        return mTagContext;
    }

    @NonNull
    protected AdNetwork createNetworkInstanceFromMap(
      @NonNull final Map<String, String> networkObject,
      @NonNull final String networkType, Map<String, ?> params)
        throws InvalidNetworkParameterException {
        return new AdNetworkBuilder()
            .networkType(networkType)
            .adType(getAdType(networkObject))
            .transitionAnimation(getTransitionAnimation(networkObject))
            .impressionUrl(networkObject.get(sPropertyImpressionUrl))
            .noBidUrl(networkObject.get(sPropertyNoBidUrl))
            .clickUrl(networkObject.get(sPropertyClickUrl))
            .parameters(params)
            .refreshRate(getRefreshRate(networkObject))
            .build();
    }

    private int getRefreshRate(@NonNull Map<String, String> networkObject)
        throws InvalidNetworkParameterException {
        String refreshRate = networkObject.get(sPropertyRefreshRate);
        if (refreshRate == null || refreshRate.trim().length() == 0) {
            refreshRate = String.valueOf(TagController.sDefaultRefreshRateInSecs);
        }

        try {
            return Integer.valueOf(refreshRate);
        } catch (NumberFormatException e) {
            throw new InvalidNetworkParameterException("Invalid RefreshRate value", mTagContext);
        }
    }

    @NonNull
    private AdType getAdType(@NonNull final Map<String, String> networkObject) {
        if (mTagContext.getTagType() == TagType.INTERSTITIAL) {
            return AdType.INTERSTITIAL;
        }

        final String pDeliveryMethod = networkObject.get(sPropertyDeliveryMethod);
        if (pDeliveryMethod != null) {
            final String deliveryMethod = pDeliveryMethod.toUpperCase(Locale.US);
            if ("INTERSTITIAL".equals(deliveryMethod) || "VAST".equals(deliveryMethod)) {
                return AdType.INTERSTITIAL;
            } else {
                return AdType.BANNER;
            }
        }

        return AdType.DEFAULT;
    }

    private TransitionAnimation getTransitionAnimation(@NonNull Map<String, String> networkObject)
        throws InvalidNetworkParameterException {
        String propertyTransitionAnimation = networkObject.get(sPropertyTransitionAnimation);
        if (propertyTransitionAnimation != null) {
            propertyTransitionAnimation = propertyTransitionAnimation.toUpperCase(Locale.US);
            if (TransitionAnimation.doesNotExist(propertyTransitionAnimation)) {
                throw new InvalidNetworkParameterException("Unknown transition animation type: "
                        + propertyTransitionAnimation, mTagContext);
            }

            try {
                return TransitionAnimation.valueOf(propertyTransitionAnimation);
            } catch (IllegalArgumentException e) {
                throw new InvalidNetworkParameterException("Invalid TransitionAnimation value",
                        mTagContext);
            }
        }

        return TransitionAnimation.DEFAULT;
    }
}
