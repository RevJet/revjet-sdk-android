/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

import android.os.Handler;
import android.os.Looper;

public final class Timer {
    public interface Event {
        public void onTimerEvent(Timer timer, boolean forceRefresh);
    }

    private static final Handler sTimerHandler = new Handler(Looper.getMainLooper());
    private final Event mEventListener;

    private long mDelay = 0;
    private long mLastTimeMillis = -1;

    private final class TimerEvent implements Runnable {
        private boolean mForceRefresh = false;

        @Override
        public void run() {
            if (mEventListener != null) {
                mEventListener.onTimerEvent(Timer.this, mForceRefresh);
            }
        }

        public void setForceRefresh(boolean forceRefresh) {
            mForceRefresh = forceRefresh;
        }
    }

    private final TimerEvent mTimerEvent = new TimerEvent();

    public Timer(Event eventListener) {
        mEventListener = eventListener;
    }

    public Timer(Event eventListener, long delay) {
        this(eventListener);
        mDelay = delay;
    }

    public void start(boolean forceRefresh) {
        if (mDelay <= 0) {
            LOGGER.warning("Timer delay can't be less or equals zero");
        } else {
            stop();

            mLastTimeMillis = System.currentTimeMillis();
            mTimerEvent.setForceRefresh(forceRefresh);
            sTimerHandler.postDelayed(mTimerEvent, mDelay * 1000);
        }
    }

    public void stop() {
        mLastTimeMillis = (long) -1;
        sTimerHandler.removeCallbacks(mTimerEvent);
    }

    public void pause() {
        if (mLastTimeMillis != -1) {
            long elapsedTime = System.currentTimeMillis() - mLastTimeMillis;
            int newRefreshRate = (int) (mDelay - (elapsedTime / 1000));
            if (newRefreshRate >= 0) {
                mDelay = newRefreshRate;
            }

            stop();
        }
    }

    public void setDelay(long delay) {
        mDelay = delay;
    }

    public long getDelay() {
        return mDelay;
    }
}
