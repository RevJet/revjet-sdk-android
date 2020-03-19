/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.revjet.android.sdk.commons.Base64;
import com.revjet.android.sdk.commons.Utils;

import java.io.ByteArrayInputStream;

public class MRAIDCloseButton extends ImageButton {

    public static final int BUTTON_SIZE = 50;

    private static StateListDrawable sDrawableStates;

    protected MRAIDCloseButton(Context context) {
        super(context);
        initializeButton();
    }

    public static MRAIDCloseButton newInstance(Context context) {
        MRAIDCloseButton closeButton = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            closeButton = new MRAIDCloseButton15(context);
        } else {
            closeButton = new MRAIDCloseButton16(context);
        }

        return closeButton;
    }

    public RelativeLayout.LayoutParams getLayout() {
        DisplayMetrics dm = getResources().getDisplayMetrics();

        float btnWidth = Utils.dipsToPixels(BUTTON_SIZE, dm);
        float btnHeight = Utils.dipsToPixels(BUTTON_SIZE, dm);

        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(Math.round(btnWidth),
                Math.round(btnHeight));
        layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        return layout;
    }

    private void initializeButton() {
        if (null == sDrawableStates) {
            BitmapDrawable drawableNormal = drawableFromString(sEncodedImageCloseButtonNormal);
            BitmapDrawable drawablePressed = drawableFromString(sEncodedImageCloseButtonPressed);
            sDrawableStates = new StateListDrawable();
            sDrawableStates.addState(new int[] {-android.R.attr.state_pressed}, drawableNormal);
            sDrawableStates.addState(new int[] {android.R.attr.state_pressed}, drawablePressed);
        }

        setImageDrawable(sDrawableStates);
        setBackgroundDrawable(null);
    }

    private BitmapDrawable drawableFromString(String encodedString) {
        byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
        BitmapDrawable drawable = new BitmapDrawable(new ByteArrayInputStream(decodedString));
        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int scaledDensity = (int)Utils.dipsToPixels(displayMetrics.xdpi, displayMetrics);
        drawable.setTargetDensity(scaledDensity);
        return drawable;
    }

    private static final String sEncodedImageCloseButtonNormal =
        "iVBORw0KGgoAAAANSUhEUgAAACQAAAAkCAYAAADhAJiYAAAKfGlDQ1BJQ0MgUHJvZmlsZQAAeAHVlndUU8kex+fe9AaBQOgQeu8dpNdQBKmC" +
            "qIQkhBpCqGJDRFzBtSAiAuqKrogouBbaWhBRLCwCCnY3yCKgrIsFUVF5N/BgPee9/e/98+ac+c3n/uY3v5k75ZwvAOROlkCQAlMBSOVn" +
            "CkN83BnLo6IZuMeAAFQBFcgDOoudIXALDg4A/1g+DAJI3HnXWJzrH8P+e4cUh5vBBgAKRrrjOBnsVITPIfyNLRBmAgCLuTcnU4AwqhBh" +
            "GSGyQIQrxMyb55Nijpvn9rmYsBAPJOYeAHgyiyXkAUASIX5GNpuH5CEjCMz4nEQ+wmYIO7MTWByEBQgbpaamibkaYb247/LwvmMWK24x" +
            "J4vFW+T5f0FGIhN7JmYIUlhr5j7+lyY1JQvZr7kijVgyP2VpANLSkTrGYXn6L7AgZe7M5vxcfnjogp8ftzRogeOF3iELLMh0/46Dwxb8" +
            "eQkeSxeYm+G1mCeJ5Sc+s7n8wqyQ8AXOyA71WuC8hLDIBeZwPRf98YnezAV/YiZzca7kNP/FNYAwkACyAB9wABcIQRxIAykAOb1Mbi5i" +
            "AfBIE6wRJvISMhluyK3jGjGYfLaJEcPCzNxc3P1/U8TvbX6x7+hz7wii3/rbl94OgH0x8ibEV/3fcSxNAFpfAED78LdP8y1yFXYBcLGX" +
            "nSXMns+HFjcYQASSQAYoIO9ZE+gBY2ABbIAjcAVewA8EIbscBVYBNrLXqcgu54B1YBMoAiVgF9gLKsEhcAQcB6fAGdAMLoAr4Dq4DXrB" +
            "AHgMRGAEvAKT4AOYgSAIB1EgGqQAqUHakCFkAdlBzpAXFACFQFFQLMSD+FAWtA7aDJVApVAldBiqg36BWqEr0E2oD3oIDUHj0FvoM4yC" +
            "ybAMrALrwKawHewG+8Nh8EqYB6fDeXAhvAOugGvgk3ATfAW+DQ/AIvgVPIUCKBKKjlJHGaPsUB6oIFQ0Kh4lRG1AFaPKUTWoBlQbqgt1" +
            "FyVCTaA+obFoGpqBNkY7on3R4Wg2Oh29Ab0dXYk+jm5Cd6LvoofQk+hvGApGGWOIccAwMcsxPEwOpghTjjmGOY+5hhnAjGA+YLFYOlYX" +
            "a4v1xUZhk7BrsduxB7CN2HZsH3YYO4XD4RRwhjgnXBCOhcvEFeH2407iLuP6cSO4j3gSXg1vgffGR+P5+AJ8Of4E/hK+Hz+KnyFQCdoE" +
            "B0IQgUNYQ9hJOEpoI9whjBBmiFJEXaITMYyYRNxErCA2EK8RnxDfkUgkDZI9aRkpkZRPqiCdJt0gDZE+kaXJBmQPcgw5i7yDXEtuJz8k" +
            "v6NQKDoUV0o0JZOyg1JHuUp5RvkoQZMwkWBKcCQ2SlRJNEn0S7yWJEhqS7pJrpLMkyyXPCt5R3KCSqDqUD2oLOoGahW1lXqfOiVFkzKX" +
            "CpJKldoudULqptSYNE5aR9pLmiNdKH1E+qr0MA1F06R50Ni0zbSjtGu0ERmsjK4MUyZJpkTmlEyPzKSstKyVbIRsrmyV7EVZER1F16Ez" +
            "6Sn0nfQz9EH6ZzkVOTc5rtw2uQa5frlpeSV5V3mufLF8o/yA/GcFhoKXQrLCboVmhaeKaEUDxWWKOYoHFa8pTijJKDkqsZWKlc4oPVKG" +
            "lQ2UQ5TXKh9R7laeUlFV8VERqOxXuaoyoUpXdVVNUi1TvaQ6rkZTc1ZLVCtTu6z2kiHLcGOkMCoYnYxJdWV1X/Us9cPqPeozGroa4RoF" +
            "Go0aTzWJmnaa8Zplmh2ak1pqWoFa67TqtR5pE7TttBO092l3aU/r6OpE6mzVadYZ05XXZerm6dbrPtGj6LnopevV6N3Tx+rb6SfrH9Dv" +
            "NYANrA0SDKoM7hjChjaGiYYHDPuMMEb2RnyjGqP7xmRjN+Ns43rjIRO6SYBJgUmzyWtTLdNo092mXabfzKzNUsyOmj02lzb3My8wbzN/" +
            "a2FgwbaosrhnSbH0ttxo2WL5xsrQimt10OqBNc060HqrdYf1VxtbG6FNg824rZZtrG217X07Gbtgu+12N+wx9u72G+0v2H9ysHHIdDjj" +
            "8JejsWOy4wnHsSW6S7hLji4ZdtJwYjkddhI5M5xjnX9yFrmou7Bcalyeu2q6clyPuY666bsluZ10e+1u5i50P+8+7eHgsd6j3RPl6eNZ" +
            "7NnjJe0V7lXp9cxbw5vnXe896WPts9an3Rfj6++72/c+U4XJZtYxJ/1s/db7dfqT/UP9K/2fBxgECAPaAuFAv8A9gU+Wai/lL20OAkHM" +
            "oD1BT4N1g9ODf12GXRa8rGrZixDzkHUhXaG00NWhJ0I/hLmH7Qx7HK4XnhXeESEZERNRFzEd6RlZGilabrp8/fLbUYpRiVEt0bjoiOhj" +
            "0VMrvFbsXTESYx1TFDO4Undl7sqbqxRXpay6uFpyNWv12VhMbGTsidgvrCBWDWsqjhlXHTfJ9mDvY7/iuHLKOONcJ24pdzTeKb40fozn" +
            "xNvDG09wSShPmEj0SKxMfJPkm3QoaTo5KLk2eTYlMqUxFZ8am9rKl+Yn8zvTVNNy0/oEhoIigSjdIX1v+qTQX3gsA8pYmdGSKYMIm+4s" +
            "vawtWUPZztlV2R9zInLO5krl8nO71xis2bZmNM877+e16LXstR3r1NdtWje03m394Q3QhrgNHRs1NxZuHMn3yT++ibgpedNvBWYFpQXv" +
            "N0dubitUKcwvHN7is6W+SKJIWHR/q+PWQz+gf0j8oWeb5bb9274Vc4pvlZiVlJd82c7efutH8x8rfpzdEb+jZ6fNzoO7sLv4uwZ3u+w+" +
            "XipVmlc6vCdwT1MZo6y47P3e1XtvlluVH9pH3Je1T1QRUNGyX2v/rv1fKhMqB6rcqxqrlau3VU8f4BzoP+h6sOGQyqGSQ59/SvzpwWGf" +
            "w001OjXlR7BHso+8OBpxtOtnu5/rjikeKzn2tZZfKzoecryzzrau7oTyiZ31cH1W/fjJmJO9pzxPtTQYNxxupDeWnAans06//CX2l8Ez" +
            "/mc6ztqdbTinfa76PO18cRPUtKZpsjmhWdQS1dLX6tfa0ebYdv5Xk19rL6hfqLooe3HnJeKlwkuzl/MuT7UL2ieu8K4Md6zueHx1+dV7" +
            "ncs6e675X7tx3fv61S63rss3nG5cuOlws/WW3a3m2za3m7qtu8//Zv3b+R6bnqY7tndaeu172/qW9F3qd+m/ctfz7vV7zHu3B5YO9A2G" +
            "Dz64H3Nf9IDzYOxhysM3j7IfzTzOf4J5UvyU+rT8mfKzmt/1f28U2YguDnkOdT8Pff54mD386o+MP76MFL6gvCgfVRutG7MYuzDuPd77" +
            "csXLkVeCVzMTRX9K/Vn9Wu/1ub9c/+qeXD458kb4Zvbt9ncK72rfW73vmAqeevYh9cPMdPFHhY/HP9l96voc+Xl0JucL7kvFV/2vbd/8" +
            "vz2ZTZ2dFbCErDktgEIsHB8PwNtaAChRiHboBYAoMa+H5yKgeQ2PsFjLz+n5/+R5zTwXbwNArSsA4fkABLQDcBCp2giTkVYsC8NcAWxp" +
            "uVgRj7hkxFtazAFEFiLS5OPs7DsVAHBtAHwVzs7OHJid/XoU0e0PAWhPn9fh4mgsFYBSXVktWe6trar5c+O/M/8CArPqa05dv3oAAAAJ" +
            "cEhZcwAACxMAAAsTAQCanBgAAAekSURBVFgJpZhJiJRXEMff9Kgz4zaijg4q7orgQclBFL0k5KIGFI0bJIyec/CkEA+jhoSIBw+iiBCU" +
            "UXDFfUHBgAaiePOgYQZBXHBfcJvM2l/n/6vuenzdduNIHrx+e9X/VdWrqq+rcrlcqFSqVLRepfXE9+zfv3/ymjVrJiRJUtvZ2Tmopqam" +
            "bsCAAf1Y7+7u7lXp1LF/Nd919uzZJytWrPjHz6rNbN26NWzZskVkKzBmvkI1MIVz1ZcvX2569uzZHy9fvmzVXJ/K8+fPnz58+PDQhQsX" +
            "fhKYeh2qxCvOx07J5oyPDxw48M39+/f/bm9vT4PI9vT09EpC2XKVNW2mWuno6Mi1trW27tmz59eFCxfW7Nixo+7YsWPVSEzSyqjl8obF" +
            "OppIFzaYim7cuNE8c+bMn4cOHVqrOYj3vnjxolq3rjp37ly4e/duEDNUZeelulBXVxdmzJgRFi1aFMaPH58bPXp0VovQrO7q6gr37t1r" +
            "v3Xr1g9S++mrV6/WXrt2rXvz5s3YTd52HFmhjZJ58uTJXhAUSo9AZA8ePJjMnj0bsH2qs2bNSjgjCSei06Oahd6jR4/+Wr58+Up1uWXk" +
            "yTitsrjw7t27Fi1SMNJeGWcyf/78CKKxsTEZMWJEMnLkyLKVNUkm7p8zZ05y+vTpRKoEWBeEb9++/UEX+0rdgPpoqQ4o6jAlGbOR3bt3" +
            "R8ITJkxIBg0alOgVxbly0mKdyt6JEyfGvTt37kxkcxFUW1vbnzpfAxBsyQBpAjAG6Pr1683qUwzM9u3bjdioUaNMEtxcdhIZlAOTnuvf" +
            "v7+da2hoSKDB2rZt2xLZnaswJzv9TfwQDO6lio4h4zVJVR0am5527dplBCZNmpRAGKIQ5OYDBw78LCgZd1JdXW37OMtFXFpISuozexLP" +
            "9kOHDs0TW8NiKhOjap42YFR60DfMx4wZY2CGDBliYzm5CMTn2FdaBw8eHOdWrVplffZzMZcUPOAFwwcPHlxRk8dCB6dX8DOJFrNz5841" +
            "Ihx2yaxfvz7Rk08wcADIFVgtBwbVMn/ixInk9evXyYYNGz6hh6HDC0Dv37/PXblyZRVYDBUemAWVrpaWFjs8ZcoUe0kQ5pavXr3iRlZP" +
            "njxpe7g1wBwUkhk+fLiNjx49GvdLLUlTU5PNc0FXHbzgCeMChhD27ds3WeGgjcmnT59mp0+fbgdhhIgzmYyNz58/Hxloq90eIMOGDUsA" +
            "xovCDTB35MiRor3SgM2zVltbG21w2rRpyTPxhLcw3JXPmoqn/ZoJlezNmzftoDxsNMi0ARf0Hpm5pPA5Y8eOtbNpyYhmcvHixQjGbYuH" +
            "4X4KntpnYUYu4dsg21kIGuLPpk2b7DDEOeSqSKvm1KlTERAMGfs+ObiiNQVVW+OF1dfXx33sdzuDZyH25d68efNdkNF9DyCC5LJly+zQ" +
            "uHHjig5DABUCjH6ppI4fP54cPny4CIxLpl+/fqZWB+2tPxZ4whsMwrIyfPz48UcHtHjx4oqAHBQ2Q//MmTNFAEQjjh2McqKyYDjvgODp" +
            "gPTamjISpyVX2hQkOpogdVlb+qMDIZvNBuk/LFmyJMiGbIvAuD8LZAFEer22IAMOb9++LSVTNFaojGPtz5AWfFFRphikBjvjaQcDQFF8" +
            "ThcN7P3SkiHt9EN65tZ14j7vrV6J5TuPHz8mQofVq1fbEhKVe7C+0oogQw/yK0GhI8j2/HjZ1i/HIlgySpo6fKd07t1PWhl04NZykEFP" +
            "OyiMxD2oCRBeli5daupEXVyuLKi8QEOap2Hpy7PHf/gzLXV6/rQF5hNDJ3Qwz0PgldL36vTSzx4sFR2je2iithMpdXoOBuIkbewrdQnu" +
            "PFnDm9Pi4xpH5/cXHKM9e5z0p6FjWj50cJjQgR+BCLeV+GMllDCPJ8fp4aPcJZQ6Tw/I7McVuPcndBCuRJfQ0UoYKwquehXdyouMEdmh" +
            "i5XgStR2QC4ZPLCDgFkl5/nhw4dk7dq1RpcMIh1c4QmgGFzVL0o/SOYXLFhgh3Fe7sA2btyYELUvXbpkawTJNBgAOSgPE0gRMM3NzRGM" +
            "00ulHwmpDykQWNx/FCVoejUREFLwoOgpBIydqQNJt2kDdslAA1rlEjSSQ523RB9AZVNY0kyYIF4I+c2Yc4BpEKV9N2DmAQENVxW0+ZoR" +
            "7xxpM+kz0gGL9ldO8knInaCrD6Nkri/VJNKQzzpdMtAsxC4DVPiwAExM8m1QQIiH3as+xb48XFIA4Ia8kHRqUgkYe9jrUmEftNJgCp9c" +
            "8I8YfEBrqmNRQbRFLQXJ9uJbMEJnTnLFC/QPxYaRMn5VH7PmCRhn/EMRWtCEsFQFD+cfefuEt3Eh9cHI+R4ScnLgqVOnRmAOsFLLXs4U" +
            "knn7woBYiWQiTy3l/3UQwXTBsGAayv3ZoL9Y7M8G5UOhtbU18AeCLm7nCZTEJv5sID3xPxukPtKDKkm+886dO7/PmzfvFzuQ/xPCeBXG" +
            "8VPaJeRtRP0//o4xD4xE8DM87dRrgk/kgWS8xo5PpFo+r6mAT/9hZV8omv9s4WsGD4zTg4YOwC/SLYyLMDjDKLF0R6K2w5qLYv3cX3qk" +
            "EPLi7QrOncqzH6xbt+5eimYmTzJ/y9R87P4HkQsq2faR5dQAAAAASUVORK5CYII=";

    private static final String sEncodedImageCloseButtonPressed =
        "iVBORw0KGgoAAAANSUhEUgAAACQAAAAkCAYAAADhAJiYAAAKfGlDQ1BJQ0MgUHJvZmlsZQAAeAHVlndUU8kex+fe9AaBQOgQeu8dpNdQBKmC" +
            "qIQkhBpCqGJDRFzBtSAiAuqKrogouBbaWhBRLCwCCnY3yCKgrIsFUVF5N/BgPee9/e/98+ac+c3n/uY3v5k75ZwvAOROlkCQAlMBSOVn" +
            "CkN83BnLo6IZuMeAAFQBFcgDOoudIXALDg4A/1g+DAJI3HnXWJzrH8P+e4cUh5vBBgAKRrrjOBnsVITPIfyNLRBmAgCLuTcnU4AwqhBh" +
            "GSGyQIQrxMyb55Nijpvn9rmYsBAPJOYeAHgyiyXkAUASIX5GNpuH5CEjCMz4nEQ+wmYIO7MTWByEBQgbpaamibkaYb247/LwvmMWK24x" +
            "J4vFW+T5f0FGIhN7JmYIUlhr5j7+lyY1JQvZr7kijVgyP2VpANLSkTrGYXn6L7AgZe7M5vxcfnjogp8ftzRogeOF3iELLMh0/46Dwxb8" +
            "eQkeSxeYm+G1mCeJ5Sc+s7n8wqyQ8AXOyA71WuC8hLDIBeZwPRf98YnezAV/YiZzca7kNP/FNYAwkACyAB9wABcIQRxIAykAOb1Mbi5i" +
            "AfBIE6wRJvISMhluyK3jGjGYfLaJEcPCzNxc3P1/U8TvbX6x7+hz7wii3/rbl94OgH0x8ibEV/3fcSxNAFpfAED78LdP8y1yFXYBcLGX" +
            "nSXMns+HFjcYQASSQAYoIO9ZE+gBY2ABbIAjcAVewA8EIbscBVYBNrLXqcgu54B1YBMoAiVgF9gLKsEhcAQcB6fAGdAMLoAr4Dq4DXrB" +
            "AHgMRGAEvAKT4AOYgSAIB1EgGqQAqUHakCFkAdlBzpAXFACFQFFQLMSD+FAWtA7aDJVApVAldBiqg36BWqEr0E2oD3oIDUHj0FvoM4yC" +
            "ybAMrALrwKawHewG+8Nh8EqYB6fDeXAhvAOugGvgk3ATfAW+DQ/AIvgVPIUCKBKKjlJHGaPsUB6oIFQ0Kh4lRG1AFaPKUTWoBlQbqgt1" +
            "FyVCTaA+obFoGpqBNkY7on3R4Wg2Oh29Ab0dXYk+jm5Cd6LvoofQk+hvGApGGWOIccAwMcsxPEwOpghTjjmGOY+5hhnAjGA+YLFYOlYX" +
            "a4v1xUZhk7BrsduxB7CN2HZsH3YYO4XD4RRwhjgnXBCOhcvEFeH2407iLuP6cSO4j3gSXg1vgffGR+P5+AJ8Of4E/hK+Hz+KnyFQCdoE" +
            "B0IQgUNYQ9hJOEpoI9whjBBmiFJEXaITMYyYRNxErCA2EK8RnxDfkUgkDZI9aRkpkZRPqiCdJt0gDZE+kaXJBmQPcgw5i7yDXEtuJz8k" +
            "v6NQKDoUV0o0JZOyg1JHuUp5RvkoQZMwkWBKcCQ2SlRJNEn0S7yWJEhqS7pJrpLMkyyXPCt5R3KCSqDqUD2oLOoGahW1lXqfOiVFkzKX" +
            "CpJKldoudULqptSYNE5aR9pLmiNdKH1E+qr0MA1F06R50Ni0zbSjtGu0ERmsjK4MUyZJpkTmlEyPzKSstKyVbIRsrmyV7EVZER1F16Ez" +
            "6Sn0nfQz9EH6ZzkVOTc5rtw2uQa5frlpeSV5V3mufLF8o/yA/GcFhoKXQrLCboVmhaeKaEUDxWWKOYoHFa8pTijJKDkqsZWKlc4oPVKG" +
            "lQ2UQ5TXKh9R7laeUlFV8VERqOxXuaoyoUpXdVVNUi1TvaQ6rkZTc1ZLVCtTu6z2kiHLcGOkMCoYnYxJdWV1X/Us9cPqPeozGroa4RoF" +
            "Go0aTzWJmnaa8Zplmh2ak1pqWoFa67TqtR5pE7TttBO092l3aU/r6OpE6mzVadYZ05XXZerm6dbrPtGj6LnopevV6N3Tx+rb6SfrH9Dv" +
            "NYANrA0SDKoM7hjChjaGiYYHDPuMMEb2RnyjGqP7xmRjN+Ns43rjIRO6SYBJgUmzyWtTLdNo092mXabfzKzNUsyOmj02lzb3My8wbzN/" +
            "a2FgwbaosrhnSbH0ttxo2WL5xsrQimt10OqBNc060HqrdYf1VxtbG6FNg824rZZtrG217X07Gbtgu+12N+wx9u72G+0v2H9ysHHIdDjj" +
            "8JejsWOy4wnHsSW6S7hLji4ZdtJwYjkddhI5M5xjnX9yFrmou7Bcalyeu2q6clyPuY666bsluZ10e+1u5i50P+8+7eHgsd6j3RPl6eNZ" +
            "7NnjJe0V7lXp9cxbw5vnXe896WPts9an3Rfj6++72/c+U4XJZtYxJ/1s/db7dfqT/UP9K/2fBxgECAPaAuFAv8A9gU+Wai/lL20OAkHM" +
            "oD1BT4N1g9ODf12GXRa8rGrZixDzkHUhXaG00NWhJ0I/hLmH7Qx7HK4XnhXeESEZERNRFzEd6RlZGilabrp8/fLbUYpRiVEt0bjoiOhj" +
            "0VMrvFbsXTESYx1TFDO4Undl7sqbqxRXpay6uFpyNWv12VhMbGTsidgvrCBWDWsqjhlXHTfJ9mDvY7/iuHLKOONcJ24pdzTeKb40fozn" +
            "xNvDG09wSShPmEj0SKxMfJPkm3QoaTo5KLk2eTYlMqUxFZ8am9rKl+Yn8zvTVNNy0/oEhoIigSjdIX1v+qTQX3gsA8pYmdGSKYMIm+4s" +
            "vawtWUPZztlV2R9zInLO5krl8nO71xis2bZmNM877+e16LXstR3r1NdtWje03m394Q3QhrgNHRs1NxZuHMn3yT++ibgpedNvBWYFpQXv" +
            "N0dubitUKcwvHN7is6W+SKJIWHR/q+PWQz+gf0j8oWeb5bb9274Vc4pvlZiVlJd82c7efutH8x8rfpzdEb+jZ6fNzoO7sLv4uwZ3u+w+" +
            "XipVmlc6vCdwT1MZo6y47P3e1XtvlluVH9pH3Je1T1QRUNGyX2v/rv1fKhMqB6rcqxqrlau3VU8f4BzoP+h6sOGQyqGSQ59/SvzpwWGf" +
            "w001OjXlR7BHso+8OBpxtOtnu5/rjikeKzn2tZZfKzoecryzzrau7oTyiZ31cH1W/fjJmJO9pzxPtTQYNxxupDeWnAans06//CX2l8Ez" +
            "/mc6ztqdbTinfa76PO18cRPUtKZpsjmhWdQS1dLX6tfa0ebYdv5Xk19rL6hfqLooe3HnJeKlwkuzl/MuT7UL2ieu8K4Md6zueHx1+dV7" +
            "ncs6e675X7tx3fv61S63rss3nG5cuOlws/WW3a3m2za3m7qtu8//Zv3b+R6bnqY7tndaeu172/qW9F3qd+m/ctfz7vV7zHu3B5YO9A2G" +
            "Dz64H3Nf9IDzYOxhysM3j7IfzTzOf4J5UvyU+rT8mfKzmt/1f28U2YguDnkOdT8Pff54mD386o+MP76MFL6gvCgfVRutG7MYuzDuPd77" +
            "csXLkVeCVzMTRX9K/Vn9Wu/1ub9c/+qeXD458kb4Zvbt9ncK72rfW73vmAqeevYh9cPMdPFHhY/HP9l96voc+Xl0JucL7kvFV/2vbd/8" +
            "vz2ZTZ2dFbCErDktgEIsHB8PwNtaAChRiHboBYAoMa+H5yKgeQ2PsFjLz+n5/+R5zTwXbwNArSsA4fkABLQDcBCp2giTkVYsC8NcAWxp" +
            "uVgRj7hkxFtazAFEFiLS5OPs7DsVAHBtAHwVzs7OHJid/XoU0e0PAWhPn9fh4mgsFYBSXVktWe6trar5c+O/M/8CArPqa05dv3oAAAAJ" +
            "cEhZcwAACxMAAAsTAQCanBgAAAkNSURBVFgJjZhbaFXZGcfX3vtEExMVL9E0NdZEvEQTH+oVGi+1oDNlsMVrKaVgH2zL0NJKUaiJxtTr" +
            "Q7UwfajzUCkMXqEIRapTUh3RkMZL6ZjEeJkIE6OiTjQm5nrO2f3/1jnrGOJJMgv22Xuv9V3+3//71mUfLwxDM0Lz9u7d6+3Zsyfu5PRe" +
            "FI/Hv6UrS/rZvu9na8w3gekLo2Gn+ruDIGjr7OysP3LkSDd6mzdvDs6cOYONYR16wwGSY98B2blzZ/7o0aN/Imdz5Gy5nmd5nmfS6Qug" +
            "6e7u7pXsvzT+n1gsduLgwYPNAJPNSGVlZUz9aYGlBSRHnpQsK9u3b8/Kycn5ULZ+mZmZWZSRkWF6e3u55C+OUQ9HrgFSVxiJRALJG4Ex" +
            "PT09rQriRwruqpMbGKzr454OEA4s+t27d5fK+McyvkwGARF98+ZN+OrVK//+/fv+ixcvTH9/v4lGo9am5MyoUaPMpEmTzOzZs8Px48fH" +
            "s7OzjdiM9PX1GQXwWVZW1i6N123btq0/HajBgFJgFM0WefmzjE2W07hAxB8+fBjU1dWZJ0+eWAAj/UydmmeWLVtqCgsLYxMmTIhgPDsn" +
            "x3R1df6xvHz379AfDCoFSExY6sltVVXVh4r6qKLNECv9Dx48CGqu1XhftnxpMQgk0aatHyugH8zBHi0/P98sX748FGtx1ZdtcqOY91Qx" +
            "jm/88hyoVrjT6V+6dCkU4s3K+8eqFcBEb926FTl37pzX/rrdiG4LhBQBaKSLNKPT1tZmGhoaPD37eXl5IYik+90rV648WbVq1U18624B" +
            "WYYcbbt27SqRkX+LmVwVYrS2tjaorq42FLJrgEkG47qGvMuRoa5o6KC7evVqpXFZXAUfiMEOAXtv3759NQ4DgGzdqGOUBqslWNbV1dV/" +
            "48aNyIULFwxFqXdr2KVAAVp2hkSigYEyBMRsI9VaDszatWvN4sWLY1mZmZGe3t7bHR0dK44ePfpKap4vILZ2pLBN7JRpNsQ1gwLAMG0B" +
            "Q6SAmTt3rsVAqkjHUI0xZGjouEBUAjawixcvmnv37gViLKZslI4bN84VuBdQN1r0xoraYwKQq6kcExhfqG2UREp0isisWbPGFBQUUA8W" +
            "JGOD0+eA0r9x40azdOlSC66lpcWCITjAvnz50sycOTOuNc4X4ILz58///cCBA+0+UQjIT0XrPNEZY2q3trYaobaKjpmVK1favuLiYrNp" +
            "0yY7NpgpB4YANmzYYObNm2fGjh1rVqxYYUpKSlJMUeiPHz82zc3NARNH74Vjxoz5MVgsIBleQnq098Rramps9OQaw7Smpibz6NGjFIj5" +
            "8+dbULDABRAuokdn/fr1BhlaqMWcAO/cuWPfGe/r7bPPV69eNSy0yUlTQmcgIzz8QYLZz549865du+aJRrs94Mylpb6+3kyZMsVMnTLV" +
            "GuM5NzfXpo+ZBCDYBAxsoMfVdLfJnDhxIgUcVrHLii4CYBF/TK4JWgb+6auwpomdKSrmmJiwBY5HlGguLRjXbm0aGlU/yX5Y0C5ugVCw" +
            "pAkwNNiClZMnT9p3QDvG6XD28YlvzcACYcn3NTApOeixN6VrGMLBW1CNJhBAGnVC8bo0IQNbpPnUqVNWBjCsQekaPgmapnsWVr8JIC43" +
            "Pe3ooJ90oACJMVhZsGCB1aAPMAOZGQoMCkmfNjPSHeML/fgkQ0NG4bA5UBQh6bt9+7aYCmwqGIOZxsZGC4bn4ZgZaNM9y24GDCX4cr1f" +
            "4w4LNECI25QGgdFHI3Uu0JTACA9i24tIqc05IKLhGlEjy9mGAqaoSZnTA4ybYTDo6smBTGcbmwNajy/hxzjhSq4HA8bfPjpF6sGBcQ5J" +
            "E8uCk6HQh1o831pMPCV9hviX7a6IHr5KAgonT57s3b17d7BOyhGRutmEEHoU8OnTp60O7/MFJibW3MJ49uxZmzrApmNKPmHSY0wyXb7m" +
            "/0MdNb7QQhVoI0ycSWQY4zRYQJjLrTOOGbfO4IxIE+vU2yUBUI4p9NFzzdmXz7gwsJ99rnuzX15e/oWEq9lfdAaOgZgVFGWUqBEaa42L" +
            "mn7A2HVGuJ0cOg4UOuiis2ULp+HEO7IEQB1OnDgRn5yNCLhW57EWC1kCdYDQ2SdSVlamOk8s7a5YOUIUFhba4sVpCoycRILEoieDFpgD" +
            "RV3xzIVuaWmpBQWTbBs0+cKnrwwBsp4+C0isfKJo/qcd15sxY0Z8+vTp9iBFNBigTpIbIeeYIVfgwaDQ4zyl/dGuWckCtrbzv5FvioqK" +
            "YspMILaadH0CIDZXf8eOHdHLly+HAvWBBDj7hpo1GkqkjDvnmdevXxudn2yRDrXowS6scGfmtbe3m+vXr6f6cEoq3//++5ytKFQK+qP9" +
            "+/df5BibOsJu3bo1c9q0aZ8pn0s4o+hzJ+BkR22xcZJ3t7XwDBvDNUDhmAYzyKtoU0fYJTrCjtYRVszUy37ZoUOH2iXqkTK+NPzjx4/3" +
            "SGkHYOQwsnDhwvhqHcg5F2GchmGeRwKDLGBglnrhjh62sIntSEZGRL56tfb8GjBgkFpovzqkYOc41ayBKhmpEPK4rvDmzZs+TNFgS31f" +
            "CxDyMAkggNA43C9atCimwNyW8Ft9m/1JQxYC/i0gK61O3e06JFB/kdLPWZV1RVXIvg5PnvtixYl0k2rpb8QIeJr7UJw1axaH+gwYFoMV" +
            "+iDdl9RO+U7kItFrU8ejUP9CbP5eWN6opiI6R/taS2Lr1q0LWTtwRD0NdyGDrHQ4xMVkw9MszpDOVwrmZw6MS1US2Lt/NiAgQLYatVB9" +
            "W7TvV7TvkS6o13rVr5kTaIvxnj9/bkG5miJF1BmL65w5c4y+56MC4eviMA8rVxTkrzSjPk8CSDEzJCAnKGD27xiSW1lZ+QMZ+43GvqOv" +
            "iAjA5IBp5mrP3iXqth7+jvEVBMdTGG3Q2EdPnz7967Fjx+y/HrJJybyT94E15ECm7sl/vVLzu6KiYqNsfE9M/FDO8hStrSVmFE1O7WyC" +
            "KVZfMfcPza5PBf5vhw8f7kBmYAZ4H9yGBZQU9hxbTlnf4sVyOFtAisXEEgHhZM+neKtA1KpO/qsviWb13VD6bWUngcDIO6w4u9z/D0aZ" +
            "6sEzTu2gAAAAAElFTkSuQmCC";
}
