/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.annotations.NetworkParameter;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.exceptions.AdapterException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

public final class AdNetworkController {
    private final AdNetwork mNetwork;

    public AdNetworkController(AdNetwork network) {
        mNetwork = network;
    }

    public void trackImpression() {
        String impressionUrl = mNetwork.getImpressionUrl();
        LOGGER.info("Tracking impression = " + impressionUrl);
        Utils.httpGetUrl(impressionUrl, TagController.sUserAgent);
    }

    public void trackClick() {
        String clickUrl = mNetwork.getClickUrl();
        LOGGER.info("Tracking click = " + clickUrl);
        Utils.httpGetUrl(clickUrl, TagController.sUserAgent);
    }

    public void trackNoBid() {
        String noBidUrl = mNetwork.getNoBidUrl();
        LOGGER.info("Tracking nobid = " + noBidUrl);
        Utils.httpGetUrl(noBidUrl, TagController.sUserAgent);
    }

    @Nullable
    public <T> T mapParameters(@Nullable Class<T> parametersClass, @Nullable Map<String, String> tagQueryParams)
            throws AdapterException {
        Field[] fields = null;
        T parametersObject = null;

        if (parametersClass != null) {
            try {
                Constructor<T> constructor = parametersClass.getConstructor();
                parametersObject = constructor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new AdapterException("No Such Method: " + e.getMessage());
            } catch (Exception e) {
                throw new AdapterException("Error instantiating parameters class: "
                        + e.getMessage());
            }

            fields = parametersClass.getFields();
        }

        if (fields == null) {
            parametersObject = null;
        } else {
            Map<String, ?> networkParams = mNetwork.getParameters();
            for (final Field field : fields) {
                NetworkParameter networkParam = field.getAnnotation(NetworkParameter.class);
                if (networkParam != null) {
                    Object value = getParamValue(networkParams, networkParam, field.getName(),
                            tagQueryParams);
                    setFieldValue(field, parametersObject, value);
                }
            }
        }

        return parametersObject;
    }

    @Nullable
    public <T> T mapParameters(@Nullable Class<T> parametersClass, @Nullable TagController tagController)
            throws AdapterException {
        Map<String, String> tagQueryParams = null;
        if (tagController != null) {
            tagQueryParams = tagController.getTagQueryParams();
        }

        return mapParameters(parametersClass, tagQueryParams);
    }

    public AdNetwork getNetwork() {
        return mNetwork;
    }

    private <T> void setFieldValue(@NonNull Field field, @NonNull T object, @Nullable Object value)
        throws AdapterException {
        try {
            field.set(object, value);
        } catch (Exception e) {
            throw new AdapterException("Error setting parameter value: " + e.getMessage());
        }
    }

    @Nullable
    private Object getParamValue(
      @Nullable final Map<String, ?> networkParams,
      @NonNull final NetworkParameter networkParam,
      @NonNull final String fieldName,
      @Nullable final Map<String, String> tagQueryParams)
        throws AdapterException {
        String name = networkParam.name();
        if (name.length() == 0) {
            name = fieldName;
        }

        name = name.toUpperCase(Locale.US);
        boolean required = networkParam.required();
        boolean appendTagParams = networkParam.appendTagParams();

        Object value = null;
        if (networkParams != null) {
            value = networkParams.get(name);
            boolean emptyValue = (value == null);
            boolean isStringValue = value instanceof String;
            String stringValue = null;
            if (isStringValue) {
                stringValue = (String)value;
            }

            if (required && emptyValue) {
                throw new AdapterException("Required parameter missing (" + name + ")");
            }

            if (appendTagParams && !emptyValue && tagQueryParams != null && isStringValue) {
                value = Utils.getUriWithQueryParams(stringValue, tagQueryParams).toString();
            }
        } else if (required) {
            throw new AdapterException("Required parameter missing (" + name + ")");
        }

        return value;
    }
}
