/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import android.util.Log;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class RevJetLogger extends Logger {
    public static final Logger LOGGER;
    static {
        LOGGER = getLogger("RevJetSDK");
        LOGGER.setLevel(Level.WARNING);
    }

    protected RevJetLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    public static Logger getLogger(String name) {
        LogManager logManager = LogManager.getLogManager();

        Logger result = logManager.getLogger(name);
        if (result == null) {
            result = new RevJetLogger(name, null);
            result.addHandler(new Handler() {
                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                }

                @Override
                public void publish(LogRecord record) {
                    final int level = record.getLevel().intValue();
                    final String tag = record.getLoggerName();

                    if (level == Level.CONFIG.intValue()) {
                        Log.i(tag, record.getMessage());
                    } else if (level == Level.FINE.intValue()
                            || level == Level.FINER.intValue()) {
                        Log.d(tag, record.getMessage());
                    } else if (level == Level.FINEST.intValue()) {
                        Log.v(tag, record.getMessage());
                    }
                }
            });

            logManager.addLogger(result);
        }

        return result;
    }

    @Override
    public void log(Level logLevel, String msg) {
        super.log(logLevel, formatMessage(msg));
    }

    @Override
    public void log(Level logLevel, String msg, Throwable thrown) {
        super.log(logLevel, formatMessage(msg), thrown);
    }

    private String formatMessage(String msg) {
        StringBuilder result = new StringBuilder();

        Throwable throwable = new Throwable();
        StackTraceElement elements[] = throwable.getStackTrace();
        if (elements != null && elements.length > 3) {
            StackTraceElement caller = elements[3];

            String className = caller.getClassName();
            if (className != null && className.contains(".")) {
                className = className.substring(className.lastIndexOf(".") + 1);
            }

            result.append(className).append(" (").append(caller.getFileName())
                    .append(":").append(caller.getLineNumber()).append("): ");
        }

        return result.append(msg).toString();
    }
}
