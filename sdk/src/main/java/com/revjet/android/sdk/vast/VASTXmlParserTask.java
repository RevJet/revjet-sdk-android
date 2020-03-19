/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast;

import android.os.AsyncTask;
import com.revjet.android.sdk.commons.CustomURLConnection;
import com.revjet.android.sdk.commons.IOUtils;
import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class VASTXmlParserTask extends AsyncTask<String, Void, VASTAdRepresentation> {

    public interface VASTXmlParserTaskListener {
        void onParseComplete(final VASTAdRepresentation adRepresentation);
    }

    static final int MAX_NUMBER_OF_REDIRECTS = 20;
    private final WeakReference<VASTXmlParserTaskListener> mListener;
    private int mNumberOfRedirects;

    public VASTXmlParserTask(final VASTXmlParserTaskListener listener) {
        super();
        mListener = new WeakReference<>(listener);
    }

    @Override
    protected VASTAdRepresentation doInBackground(String... strings) {
        List<VASTAdRepresentation> adRepresentations = null;
        try {
            if (strings != null && strings.length > 0) {
                String vastXml = strings[0];

                adRepresentations = new ArrayList<>();
                while (vastXml != null && vastXml.length() > 0 && !isCancelled()) {
                    final VASTXmlParser parser = new VASTXmlParser(vastXml);
                    VASTAdRepresentation adRepresentation = parser.parse();
                    adRepresentations.add(adRepresentation);
                    vastXml = followVastRedirect(adRepresentation.getAdTagUri());
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to parse VAST XML. Exception: " + e);
        }

        VASTAdRepresentation adRepresentation = null;

        if (null != adRepresentations) {
            adRepresentation =  new VASTAdRepresentation();
            for (VASTAdRepresentation representation : adRepresentations) {
                adRepresentation.addImpressions(representation.getImpressions());

                adRepresentation.addStartTrackingEvents(representation.getStartTrackingEvents());
                adRepresentation.addFirstQuartileTrackingEvents(representation.getFirstQuartileTrackingEvents());
                adRepresentation.addMidpointTrackingEvents(representation.getMidpointTrackingEvents());
                adRepresentation.addThirdQuartileTrackingEvents(representation.getThirdQuartileTrackingEvents());
                adRepresentation.addCompleteTrackingEvents(representation.getCompleteTrackingEvents());

                adRepresentation.addClickTrackingEvents(representation.getClickTrackingEvents());

                if (null == adRepresentation.getClickThrough()) {
                    adRepresentation.setClickThrough(representation.getClickThrough());
                }

                adRepresentation.addMediaFileRepresentations(representation.getMediaFileRepresentations());
                adRepresentation.addCompanionAdRepresentations(representation.getCompanionAdRepresentations());
            }
        }

        return adRepresentation;
    }

    @Override
    protected void onPostExecute(final VASTAdRepresentation representation) {
        final VASTXmlParserTaskListener listener = mListener.get();
        if (null != listener) {
            listener.onParseComplete(representation);
        }
    }

    @Override
    protected void onCancelled() {
        final VASTXmlParserTaskListener listener = mListener.get();
        if (null != listener) {
            listener.onParseComplete(null);
        }
    }

    String followVastRedirect(final String redirectUrl) throws Exception {
        if (redirectUrl != null && mNumberOfRedirects < MAX_NUMBER_OF_REDIRECTS) {
            mNumberOfRedirects++;

            final HttpURLConnection urlConnection = CustomURLConnection.openConnection(redirectUrl);
            try {
                final InputStream inputStream = urlConnection.getInputStream();
                return IOUtils.toString(inputStream, "UTF-8");
            } finally {
                urlConnection.disconnect();
            }
        }
        return null;
    }
}
