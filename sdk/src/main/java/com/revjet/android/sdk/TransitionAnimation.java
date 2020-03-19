/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk;

public enum TransitionAnimation {
    NONE, RANDOM, FLIPFROMLEFT, FLIPFROMRIGHT, CURLUP, CURLDOWN;

    public static final TransitionAnimation DEFAULT = NONE;

    public static boolean doesNotExist(String transitionAnimation) {
        for (final TransitionAnimation enumTransitionAnimation : TransitionAnimation.values()) {
            if (enumTransitionAnimation.name().equals(transitionAnimation)) {
                return false;
            }
        }

        return true;
    }
}
