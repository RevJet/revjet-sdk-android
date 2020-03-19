/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.exceptions;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;

public final class TagUncaughtExceptionHandler implements UncaughtExceptionHandler {
    public static final String EXCEPTION_MESSAGE = "Uncaught exception";
    public static boolean ENABLED = true;
    private final UncaughtExceptionHandler mOldExceptionHandler;

    public TagUncaughtExceptionHandler() {
        mOldExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOGGER.log(Level.SEVERE, EXCEPTION_MESSAGE, ex);

        if (mOldExceptionHandler != null) {
            mOldExceptionHandler.uncaughtException(thread, ex);
        }
    }
}
