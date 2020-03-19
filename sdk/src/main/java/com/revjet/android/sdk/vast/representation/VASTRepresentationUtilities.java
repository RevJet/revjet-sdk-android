/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.vast.representation;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Display;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;

public class VASTRepresentationUtilities {
    private static final double ASPECT_RATIO_WEIGHT = 40;
    private static final double AREA_WEIGHT = 60;

    public static final String VAST_VERSION = "VAST 2.0 Wrapper";

    public static final List<String> VIDEO_TYPES =
            Arrays.asList("video/mp4", "video/3gpp");
    public static final List<String> COMPANION_AD_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/bmp", "image/gif");

    private static double mScreenAspectRatio;
    private static int mScreenArea;

    @Nullable
    public static VASTMediaFileRepresentation chooseBestMediaFileRepresentation(
      @NonNull final List<VASTMediaFileRepresentation> representations,
      @NonNull final Context context) {
        VASTMediaFileRepresentation bestRepresentation = null;
        double bestFitness = Double.POSITIVE_INFINITY;

        for (VASTMediaFileRepresentation representation : representations) {
            String type = representation.getType();
            String url = representation.getVideoUrl();
            if (VIDEO_TYPES.contains(type) && url != null) {
                Integer width = representation.getWidth();
                Integer height = representation.getHeight();
                double fitness = calculateFitness(width, height, context);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    bestRepresentation = representation;
                }
                else if (null == bestRepresentation) {
                    bestRepresentation = representation;
                }
            }
        }

        return bestRepresentation;
    }

    @Nullable
    public static VASTCompanionAdRepresentation chooseBestCompanionAdRepresentation(
      @NonNull final List<VASTCompanionAdRepresentation> representations,
      @NonNull final Context context) {
        VASTCompanionAdRepresentation bestRepresentation = null;
        double bestFitness = Double.POSITIVE_INFINITY;
        for (VASTCompanionAdRepresentation representation : representations) {
            String type = representation.getType();
            String url = representation.getImageUrl();
            if (COMPANION_AD_TYPES.contains(type) && url != null) {
                Integer width = representation.getWidth();
                Integer height = representation.getHeight();
                double fitness = calculateFitness(width, height, context);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    bestRepresentation = representation;
                }
                else if (null == bestRepresentation) {
                    bestRepresentation = representation;
                }
            }
        }

        return bestRepresentation;
    }

    private static double calculateFitness(final int width, final int height, @NonNull final Context context) {
        if (0 == mScreenArea || 0 == mScreenAspectRatio) {
            final Display display =
                ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int x = display.getWidth();
            int y = display.getHeight();

            int screenWidth = Math.max(x, y);
            int screenHeight = Math.min(x, y);
            mScreenAspectRatio = (double) screenWidth / screenHeight;
            mScreenArea = screenWidth * screenHeight;
        }

        final double mediaAspectRatio = (double) width / height;
        final int mediaArea = width * height;
        final double aspectRatioRatio = mediaAspectRatio / mScreenAspectRatio;
        final double areaRatio = (double) mediaArea / mScreenArea;
        return ASPECT_RATIO_WEIGHT * Math.abs(Math.log(aspectRatioRatio))
                + AREA_WEIGHT * Math.abs(Math.log(areaRatio));
    }
}
