/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

import com.revjet.android.sdk.exceptions.InvalidNetworkParameterException;
import com.revjet.android.sdk.exceptions.TagResponseException;
import com.revjet.android.sdk.vast.VASTXmlParserTask;
import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagXmlResponse extends AbstractTagResponse implements VASTXmlParserTask.VASTXmlParserTaskListener {
    private final String mResponseBody;
    private TagResponseParseListener mListener;

    public TagXmlResponse(TagContext tagContext, String responseBody) {
        super(tagContext);
        mResponseBody = responseBody;
    }

    @Override
    public void parse(TagResponseParseListener listener) throws TagResponseException {
        mListener = listener;

        new VASTXmlParserTask(this).execute(mResponseBody);
    }

    @Override
    public void onParseComplete(VASTAdRepresentation adRepresentation) {
        List<AdNetwork> networks = new ArrayList<AdNetwork>();
        Map<String, VASTAdRepresentation> params = new HashMap<String, VASTAdRepresentation>();
        if (null != adRepresentation) {
            params.put("VASTADREPRESENTATION", adRepresentation);
            try {
                networks.add(createNetworkInstanceFromMap(new HashMap<String, String>(), "VAST", params));
                setNetworks(networks);
                notifyListenerOnParse(TAG_RESPONSE_RESULT_SUCCESS);
            } catch (InvalidNetworkParameterException e) {
                notifyListenerOnParse(TAG_RESPONSE_RESULT_FAIL);
            }
        } else {
            notifyListenerOnParse(TAG_RESPONSE_RESULT_FAIL);
        }
    }

    private void notifyListenerOnParse(int result) {
        if (null != mListener) {
            mListener.onParse(result);
        }
    }
}
