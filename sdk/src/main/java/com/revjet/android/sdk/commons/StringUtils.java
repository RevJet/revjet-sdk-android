/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;

public final class StringUtils {
    @Nullable
    public static String md5(@Nullable String str) {
        return hashEncode(str, "MD5");
    }

    @Nullable
    public static String sha1(@Nullable String str) {
        return hashEncode(str, "SHA-1");
    }

    @Nullable
    public static String base64(@Nullable String str) {
        String encodedStr = null;
        if (str != null) {
            encodedStr = new String(encode64(str.getBytes())).trim();
        }

        return encodedStr;
    }

    public static boolean containsIgnoreCase(@Nullable String str, @Nullable String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String hashEncode(@Nullable String str, @NonNull String algorithm) {
        String resultString = null;

        if (str != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                digest.reset();
                digest.update(str.getBytes());

                resultString = data2Hex(digest.digest());
            } catch (Exception ignored) {
            }
        }

        return resultString;
    }

    private static final byte[] sEncodingTable = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    @NonNull
    private static byte[] encode64(@NonNull byte[] in) {
        int length = in.length;
        byte[] out = new byte[((length + 2) / 3) * 4];

        for (int i = 0; i < length; i += 3) {
            int value = 0;
            for (int j = i; j < (i + 3); j++) {
                value <<= 8;

                if (j < length) {
                    value |= (0xFF & in[j]);
                }
            }

            int index = (i / 3) * 4;
            out[index] = sEncodingTable[(value >> 18) & 0x3F];
            out[index + 1] = sEncodingTable[(value >> 12) & 0x3F];
            out[index + 2] = (i + 1) < length ? sEncodingTable[(value >> 6) & 0x3F]
                    : (byte) '=';
            out[index + 3] = (i + 2) < length ? sEncodingTable[value & 0x3F]
                    : (byte) '=';
        }

        return out;
    }

    @Nullable
    private static String data2Hex(@Nullable byte[] data) {
        String resultString = null;

        if (data != null) {
            int len = data.length;
            StringBuilder sb = new StringBuilder(len << 1);
            for (int i = 0; i < len; i++) {
                sb.append(Character.forDigit((data[i] & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(data[i] & 0x0f, 16));
            }

            resultString = sb.toString();
        }

        return resultString;
    }
}
