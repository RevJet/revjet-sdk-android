/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.representation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VASTAdRepresentation implements Serializable {

    private ArrayList<String> impressions = new ArrayList<String>();

    private String clickThrough;
    private ArrayList<String> clickTrackingEvents = new ArrayList<String>();

    private ArrayList<String> startTrackingEvents = new ArrayList<String>();
    private ArrayList<String> firstQuartileTrackingEvents = new ArrayList<String>();
    private ArrayList<String> midpointTrackingEvents = new ArrayList<String>();
    private ArrayList<String> thirdQuartileTrackingEvents = new ArrayList<String>();
    private ArrayList<String> completeTrackingEvents = new ArrayList<String>();

    private ArrayList<VASTMediaFileRepresentation> mediaFileRepresentations =
            new ArrayList<VASTMediaFileRepresentation>();
    private ArrayList<VASTCompanionAdRepresentation> companionAdRepresentations =
            new ArrayList<VASTCompanionAdRepresentation>();
    
    private VASTMediaFileRepresentation bestMediaFileRepresentation;
    private VASTCompanionAdRepresentation bestCompanionAdRepresentation;
    
    private String mediaFileUrl;
    private String companionAdUrl;
    
    private String adTagUri;

    public void addImpressions(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.impressions.addAll(urls);
        }
    }

    public void addClickTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.clickTrackingEvents.addAll(urls);
        }
    }

    public void addStartTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.startTrackingEvents.addAll(urls);
        }
    }

    public void addFirstQuartileTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.firstQuartileTrackingEvents.addAll(urls);
        }
    }

    public void addMidpointTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.midpointTrackingEvents.addAll(urls);
        }
    }

    public void addThirdQuartileTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.thirdQuartileTrackingEvents.addAll(urls);
        }
    }

    public void addCompleteTrackingEvents(ArrayList<String> urls) {
        if (isListNotNullAndNotEmpty(urls)) {
            this.completeTrackingEvents.addAll(urls);
        }
    }

    public void addMediaFileRepresentations(ArrayList<VASTMediaFileRepresentation> representations) {
        if (isListNotNullAndNotEmpty(representations)) {
            this.mediaFileRepresentations.addAll(representations);
        }
    }

    public ArrayList<VASTMediaFileRepresentation> getMediaFileRepresentations() {
        return mediaFileRepresentations;
    }

    public ArrayList<VASTCompanionAdRepresentation> getCompanionAdRepresentations() {
        return companionAdRepresentations;
    }

    public void addCompanionAdRepresentations(ArrayList<VASTCompanionAdRepresentation> representations) {
        if (isListNotNullAndNotEmpty(representations)) {
            this.companionAdRepresentations.addAll(representations);
        }
    }

    public String getMediaFileUrl() {
        return mediaFileUrl;
    }

    public void setMediaFileUrl(String mediaFileUrl) {
        this.mediaFileUrl = mediaFileUrl;
    }

    public String getCompanionAdUrl() {
        return companionAdUrl;
    }

    public void setCompanionAdUrl(String companionAdUrl) {
        this.companionAdUrl = companionAdUrl;
    }

    public ArrayList<String> getImpressions() {
        return impressions;
    }

    public String getClickThrough() {
        return clickThrough;
    }

    public void setClickThrough(String clickThrough) {
        this.clickThrough = clickThrough;
    }

    public ArrayList<String> getClickTrackingEvents() {
        return clickTrackingEvents;
    }

    public ArrayList<String> getStartTrackingEvents() {
        return startTrackingEvents;
    }

    public ArrayList<String> getFirstQuartileTrackingEvents() {
        return firstQuartileTrackingEvents;
    }

    public ArrayList<String> getMidpointTrackingEvents() {
        return midpointTrackingEvents;
    }

    public ArrayList<String> getThirdQuartileTrackingEvents() {
        return thirdQuartileTrackingEvents;
    }

    public ArrayList<String> getCompleteTrackingEvents() {
        return completeTrackingEvents;
    }

    public VASTMediaFileRepresentation getBestMediaFileRepresentation() {
        return bestMediaFileRepresentation;
    }

    public void setBestMediaFileRepresentation(VASTMediaFileRepresentation bestMediaFileRepresentation) {
        this.bestMediaFileRepresentation = bestMediaFileRepresentation;
    }

    public VASTCompanionAdRepresentation getBestCompanionAdRepresentation() {
        return bestCompanionAdRepresentation;
    }

    public void setBestCompanionAdRepresentation(VASTCompanionAdRepresentation bestCompanionAdRepresentation) {
        this.bestCompanionAdRepresentation = bestCompanionAdRepresentation;
    }

    public String getAdTagUri() {
        return adTagUri;
    }

    public void setAdTagUri(String adTagUri) {
        this.adTagUri = adTagUri;
    }

    private static boolean isListNotNullAndNotEmpty(List<?> list) {
        return (null != list && list.size() > 0);
    }
}
