/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;

import com.android.settings.R;

/** Utility methods for internal storage health. */
public final class StorageHealthUtils {

    private static final String TAG = "StorageHealthUtils";

    public static final int REMAINING_LIFETIME_UNAVAILABLE = -1;

    private static final int STATUS_GOOD_MIN_PERCENT = 80;
    private static final int STATUS_NORMAL_MIN_PERCENT = 50;
    private static final int STATUS_LIMITED_MIN_PERCENT = 20;

    private StorageHealthUtils() {}

    /** Returns remaining internal storage lifetime percent, or {@code -1} if unavailable. */
    public static int getRemainingLifetimePercent(Context context) {
        final StorageManager storageManager = context.getSystemService(StorageManager.class);
        if (storageManager == null) {
            return REMAINING_LIFETIME_UNAVAILABLE;
        }

        try {
            return storageManager.getInternalStorageRemainingLifetime();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to read internal storage remaining lifetime", e);
            return REMAINING_LIFETIME_UNAVAILABLE;
        }
    }

    /** Returns whether storage health is supported on this device. */
    public static boolean isStorageHealthSupported(Context context) {
        return getRemainingLifetimePercent(context) != REMAINING_LIFETIME_UNAVAILABLE;
    }

    /** Returns dashboard summary text. */
    public static CharSequence getRemainingLifetimeSummary(Context context,
            int remainingLifetimePercent) {
        if (remainingLifetimePercent == REMAINING_LIFETIME_UNAVAILABLE) {
            return context.getString(R.string.device_info_not_available);
        }
        return context.getString(
                R.string.storage_health_lifetime_summary, remainingLifetimePercent);
    }

    /** Returns detail page lifetime value text. */
    public static CharSequence getRemainingLifetimeValue(Context context,
            int remainingLifetimePercent) {
        if (remainingLifetimePercent == REMAINING_LIFETIME_UNAVAILABLE) {
            return context.getString(R.string.device_info_not_available);
        }
        return context.getString(R.string.storage_health_lifetime_value,
                remainingLifetimePercent);
    }

    /** Returns user-facing health status text. */
    public static CharSequence getHealthStatus(Context context, int remainingLifetimePercent) {
        if (remainingLifetimePercent == REMAINING_LIFETIME_UNAVAILABLE) {
            return context.getString(R.string.storage_health_status_unknown);
        } else if (remainingLifetimePercent >= STATUS_GOOD_MIN_PERCENT) {
            return context.getString(R.string.storage_health_status_good);
        } else if (remainingLifetimePercent >= STATUS_NORMAL_MIN_PERCENT) {
            return context.getString(R.string.storage_health_status_normal);
        } else if (remainingLifetimePercent >= STATUS_LIMITED_MIN_PERCENT) {
            return context.getString(R.string.storage_health_status_limited);
        }
        return context.getString(R.string.storage_health_status_critical);
    }
}
