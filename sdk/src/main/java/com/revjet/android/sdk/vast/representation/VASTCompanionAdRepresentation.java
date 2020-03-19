/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.representation;

import java.util.ArrayList;

public class VASTCompanionAdRepresentation extends VASTComponentRepresentation {
    private String imageUrl;
    private String clickThroughUrl;
    private ArrayList<String> clickTrackers = new ArrayList<String>();

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public void setClickThroughUrl(String clickThroughUrl) {
        this.clickThroughUrl = clickThroughUrl;
    }

    public ArrayList<String> getClickTrackers() {
        return clickTrackers;
    }

    public void setClickTrackers(ArrayList<String> clickTrackers) {
        this.clickTrackers = clickTrackers;
    }
}
