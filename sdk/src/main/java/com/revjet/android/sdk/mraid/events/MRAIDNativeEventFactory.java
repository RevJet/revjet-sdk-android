/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import java.lang.reflect.Constructor;

public class MRAIDNativeEventFactory {

    public static final String ANDROID_CALENDAR_CONTENT_TYPE = "vnd.android.cursor.item/event";

    static public MRAIDNativeEvent createEvent(String name) {
        String eventName = name.toLowerCase();
        eventName = Character.toUpperCase(eventName.charAt(0)) + eventName.substring(1);
        String className = "com.revjet.android.sdk.mraid.events.MRAIDNative" + eventName + "Event";
        try {
            Class<?> eventClass = Class.forName(className);
            Constructor<?> constructor = eventClass.getConstructor();
            MRAIDNativeEvent nativeEvent = (MRAIDNativeEvent) constructor.newInstance();
            return nativeEvent;
        } catch (Exception e) {
            return null;
        }
    }
}
