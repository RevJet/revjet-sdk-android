/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast;

import com.revjet.android.sdk.vast.representation.VASTAdRepresentation;
import com.revjet.android.sdk.vast.representation.VASTCompanionAdRepresentation;
import com.revjet.android.sdk.vast.representation.VASTMediaFileRepresentation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VASTXmlParser {

    // Element names
    private static final String IMPRESSION = "Impression";
    private static final String CLICK_THROUGH = "ClickThrough";
    private static final String CLICK_TRACKING = "ClickTracking";
    private static final String TRACKING = "Tracking";
    private static final String MEDIA_FILE = "MediaFile";
    private static final String COMPANION = "Companion";
    private static final String STATIC_RESOURCE = "StaticResource";
    private static final String COMPANION_CLICK_THROUGH = "CompanionClickThrough";
    private static final String TRACKING_EVENTS = "TrackingEvents";
    private static final String VAST_AD_TAG_URI = "VASTAdTagURI";

    // Attribute names
    private static final String EVENT = "event";
    private static final String TYPE  = "type";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String CREATIVE_TYPE = "creativeType";

    // Attribute values
    private static final String CREATIVE_VIEW = "creativeView";

    // Tracking event names
    private static final String START = "start";
    private static final String FIRST_QUARTILE = "firstQuartile";
    private static final String MIDPOINT = "midpoint";
    private static final String THIRD_QUARTILE = "thirdQuartile";
    private static final String COMPLETE = "complete";

    private String xmlString;
    private Document vastDocument;

    public VASTXmlParser(String xmlString) {
        this.xmlString = xmlString;

        if (null != this.xmlString) {
            this.xmlString = this.xmlString.replaceFirst("<\\?.*\\?>", "");
        }
    }

    public VASTAdRepresentation parse() throws ParserConfigurationException, IOException, SAXException {
        if (null == this.xmlString) {
            throw new IOException();
        }

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setCoalescing(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.vastDocument = documentBuilder.parse(new InputSource(new StringReader(this.xmlString)));

        VASTAdRepresentation adRepresentation = this.createAdRepresentaiton();
        adRepresentation.addMediaFileRepresentations(this.createMediaFileRepresentations());
        adRepresentation.addCompanionAdRepresentations(this.createCompanionAdRepresentations());

        return adRepresentation;
    }

    private VASTAdRepresentation createAdRepresentaiton() {
        VASTAdRepresentation adRepresentation = new VASTAdRepresentation();
        ArrayList<String> impressions = XMLUtilities.getStringDataAsList(this.vastDocument, IMPRESSION);
        adRepresentation.addImpressions(impressions);

        ArrayList<String> clickThrough = XMLUtilities.getStringDataAsList(this.vastDocument, CLICK_THROUGH);
        if (clickThrough.size() > 0) {
            adRepresentation.setClickThrough(clickThrough.get(0));
        }

        ArrayList<String> clickTrackingEvents = XMLUtilities.getStringDataAsList(this.vastDocument, CLICK_TRACKING);
        adRepresentation.addClickTrackingEvents(clickTrackingEvents);

        ArrayList<String> startTrackingEvents = this.getVideoTrackerByAttribute(START);
        adRepresentation.addStartTrackingEvents(startTrackingEvents);

        ArrayList<String> firstQuartileTrackingEvents = this.getVideoTrackerByAttribute(FIRST_QUARTILE);
        adRepresentation.addFirstQuartileTrackingEvents(firstQuartileTrackingEvents);

        ArrayList<String> midpointTrackingEvents = this.getVideoTrackerByAttribute(MIDPOINT);
        adRepresentation.addMidpointTrackingEvents(midpointTrackingEvents);

        ArrayList<String> thirdQuartileTrackingEvents = this.getVideoTrackerByAttribute(THIRD_QUARTILE);
        adRepresentation.addThirdQuartileTrackingEvents(thirdQuartileTrackingEvents);

        ArrayList<String> completeTrackingEvents = this.getVideoTrackerByAttribute(COMPLETE);
        adRepresentation.addCompleteTrackingEvents(completeTrackingEvents);

        List<String> uriWrapper = XMLUtilities.getStringDataAsList(vastDocument, VAST_AD_TAG_URI);
        if (null != uriWrapper && uriWrapper.size() > 0) {
            adRepresentation.setAdTagUri(uriWrapper.get(0));
        }
        
        return adRepresentation;
    }

    private ArrayList<VASTMediaFileRepresentation> createMediaFileRepresentations() {
        final NodeList mediaFileNodes = this.vastDocument.getElementsByTagName(MEDIA_FILE);
        int numberOfNodes = mediaFileNodes.getLength();
        final ArrayList<VASTMediaFileRepresentation> mediaFileRepresentations =
                new ArrayList<VASTMediaFileRepresentation>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; ++i) {
            mediaFileRepresentations.add(this.createMediaFileRepresentation(mediaFileNodes.item(i)));
        }
        return mediaFileRepresentations;
    }

    private VASTMediaFileRepresentation createMediaFileRepresentation(Node mediaFileNode) {
        Integer width = XMLUtilities.getAttributeValueAsInt(mediaFileNode, WIDTH);
        Integer height = XMLUtilities.getAttributeValueAsInt(mediaFileNode, HEIGHT);
        String type = XMLUtilities.getAttributeValue(mediaFileNode, TYPE);
        String url = XMLUtilities.getNodeValue(mediaFileNode);

        VASTMediaFileRepresentation mediaFileRepresentation = new VASTMediaFileRepresentation();
        mediaFileRepresentation.setWidth(width);
        mediaFileRepresentation.setHeight(height);
        mediaFileRepresentation.setType(type);
        mediaFileRepresentation.setVideoUrl(url);
        return mediaFileRepresentation;
    }

    private ArrayList<VASTCompanionAdRepresentation> createCompanionAdRepresentations() {
        final NodeList companionAdNodes = this.vastDocument.getElementsByTagName(COMPANION);
        int numberOfNodes = companionAdNodes.getLength();
        final ArrayList<VASTCompanionAdRepresentation> companionAdRepresentations =
                new ArrayList<VASTCompanionAdRepresentation>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; ++i) {
            companionAdRepresentations.add(this.createCompanionAdRepresentation(companionAdNodes.item(i)));
        }
        return companionAdRepresentations;
    }

    private VASTCompanionAdRepresentation createCompanionAdRepresentation(Node companionAdNode) {
        Integer width = XMLUtilities.getAttributeValueAsInt(companionAdNode, WIDTH);
        Integer height = XMLUtilities.getAttributeValueAsInt(companionAdNode, HEIGHT);
        Node staticResourceNode = XMLUtilities.getFirstMatchingChildNode(companionAdNode, STATIC_RESOURCE);
        String type = XMLUtilities.getAttributeValue(staticResourceNode, CREATIVE_TYPE);
        String imageUrl = XMLUtilities.getNodeValue(staticResourceNode);
        Node companionClickThroughNode = XMLUtilities.getFirstMatchingChildNode(
                companionAdNode, COMPANION_CLICK_THROUGH);
        String clickThroughCompanion = XMLUtilities.getNodeValue(companionClickThroughNode);
        ArrayList<String> clickTrackers = new ArrayList<String>();
        Node trackingEventsNode = XMLUtilities.getFirstMatchingChildNode(companionAdNode, TRACKING_EVENTS);
        if (null != trackingEventsNode) {
            ArrayList<Node> trackerNodes = XMLUtilities.getMatchingChildNodes(
                    trackingEventsNode, TRACKING, EVENT, Arrays.asList(CREATIVE_VIEW));
            for (Node trackerNode : trackerNodes) {
                if (trackerNode.getFirstChild() != null) {
                    clickTrackers.add(trackerNode.getFirstChild().getNodeValue().trim());
                }
            }
        }

        VASTCompanionAdRepresentation companionAdRepresentation = new VASTCompanionAdRepresentation();
        companionAdRepresentation.setWidth(width);
        companionAdRepresentation.setHeight(height);
        companionAdRepresentation.setType(type);
        companionAdRepresentation.setImageUrl(imageUrl);
        companionAdRepresentation.setClickThroughUrl(clickThroughCompanion);
        companionAdRepresentation.setClickTrackers(clickTrackers);
        return companionAdRepresentation;
    }

    private ArrayList<String> getVideoTrackerByAttribute(final String attributeValue) {
        return XMLUtilities.getStringDataAsList(this.vastDocument, TRACKING, EVENT, attributeValue);
    }
}
