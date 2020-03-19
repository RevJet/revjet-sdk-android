/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revjet.android.sdk.exceptions.InvalidNetworkParameterException;
import com.revjet.android.sdk.exceptions.TagResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;
import java.util.logging.Level;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public final class TagJsonResponse extends AbstractTagResponse {
    @NonNull private final String mResponseBody;

    public TagJsonResponse(@NonNull final TagContext tagContext, @NonNull final String responseBody) {
        super(tagContext);
        mResponseBody = responseBody;
    }

    @Override
    public void parse(@Nullable final TagResponseParseListener listener) throws TagResponseException {
        JSONObject tagObject = null;
        try {
            Object nextValue = new JSONTokener(mResponseBody).nextValue();
            if (nextValue instanceof JSONObject) {
                tagObject = (JSONObject) nextValue;
            }
        } catch (JSONException e) {
            throw new TagResponseException("Error parsing JSON: " + e.getMessage(),
                    getTagContext());
        }

        if (tagObject == null) {
            throw new TagResponseException("Invalid JSON response", getTagContext());
        }

        if (advertisementIsNotAvailable(tagObject)) {
            makeNobidObject(tagObject);
        }

        JSONArray propertyNetworks = tagObject.optJSONArray(sPropertyNetworks);

        List<AdNetwork> networks = parseNetworks(tagObject, propertyNetworks);
        if (networks.size() == 0) {
            throw new TagResponseException(ErrorCode.EMPTY_NETWORKS_ARRAY,
                    "Empty/Invalid networks array", getTagContext());
        }

        setNetworks(networks);

        if (null != listener) {
            listener.onParse(TAG_RESPONSE_RESULT_SUCCESS);
        }
    }

    private boolean advertisementIsNotAvailable(@NonNull final JSONObject tagObject) {
        return !tagObject.optBoolean("advertisementAvailable", true);
    }

    private void makeNobidObject(@Nullable JSONObject tagObject) throws TagResponseException {
        if (tagObject != null) {
            tagObject.remove("advertisementAvailable");

            try {
                JSONObject networkParameters = new JSONObject().put("content", "nobid");
                JSONObject nobidNetwork = new JSONObject()
                    .put(sPropertyNetworkType, "RJ")
                    .put(sPropertyParameters, networkParameters);
                JSONArray networks = new JSONArray().put(nobidNetwork);
                tagObject.put(sPropertyNetworks, networks);
            } catch (JSONException e) {
                throw new TagResponseException("Error parsing JSON: " + e.getMessage(),
                        getTagContext());
            }
        }
    }

    @NonNull
    private List<AdNetwork> parseNetworks(@Nullable JSONObject tagObject, @Nullable JSONArray propertyNetworks)
            throws TagResponseException {
        List<AdNetwork> networks = new ArrayList<>();

        if (propertyNetworks == null) {
            // Networks array is missing. It means that there is only one
            // Network object to process.
            networks.add(createNetworkInstance(tagObject));
            return networks;
        }

        if (propertyNetworks.length() == 0) {
            throw new TagResponseException(ErrorCode.EMPTY_NETWORKS_ARRAY, "Empty networks array",
                    getTagContext());
        }

        for (int i = 0; i < propertyNetworks.length(); i++) {
            try {
                networks.add(createNetworkInstance(propertyNetworks.getJSONObject(i)));
            } catch (InvalidNetworkParameterException e) {
                // Current Network object has invalid parameter(s). Try to
                // parse next Network object from the array.
                LOGGER.log(Level.WARNING, "Invalid network parameter: " + e.getMessage());
            } catch (JSONException e) {
                throw new TagResponseException("Error parsing JSON: " + e.getMessage(),
                        getTagContext());
            }
        }

        return networks;
    }

    @NonNull
    private AdNetwork createNetworkInstance(@Nullable JSONObject networkObject)
            throws InvalidNetworkParameterException {
        if (networkObject == null) {
            throw new InvalidNetworkParameterException(ErrorCode.EMPTY_NETWORKS_ARRAY,
                    "Empty network array", getTagContext());
        }

        Map<String, String> networkMap = JSONObjectToMap(networkObject, false);

        String networkType = getNetworkType(networkObject);
        Map<String, String> params = getNetworkParameters(networkObject);

        return createNetworkInstanceFromMap(networkMap, networkType, params);
    }

    @NonNull
    private Map<String, String> JSONObjectToMap(@NonNull JSONObject jsonObject, boolean toUpperCase) {
        Map<String, String> map = new HashMap<>();

        Iterator<?> it = jsonObject.keys();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (toUpperCase) {
                map.put(key.toUpperCase(Locale.US), jsonObject.optString(key));
            } else {
                map.put(key, jsonObject.optString(key));
            }
        }

        return map;
    }

    @NonNull
    private String getNetworkType(@NonNull JSONObject jsonObject) {
        String deliveryMethod = jsonObject.optString(sPropertyDeliveryMethod).toUpperCase(Locale.US);
        if ("MRAID".equals(deliveryMethod) || "VAST".equals(deliveryMethod)) {
            return deliveryMethod;
        } else {
            return "RJ";
        }
    }

    @NonNull
    private Map<String, String> getNetworkParameters(@NonNull JSONObject jsonObject) {
        JSONObject parameters = jsonObject.optJSONObject(sPropertyParameters);
        if (parameters != null) {
            return JSONObjectToMap(parameters, true);
        } else {
            return JSONObjectToMap(jsonObject, true);
        }
    }
}
