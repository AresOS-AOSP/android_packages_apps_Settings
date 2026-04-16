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

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

/** Controller for the storage health entry on storage pages. */
public class StorageHealthPreferenceController extends StorageHealthBasePreferenceController {

    @Nullable
    private Preference mPreference;
    @Nullable
    private StorageEntry mStorageEntry;

    public StorageHealthPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Sets storage entry currently shown by the host storage page. */
    public void setSelectedStorageEntry(@Nullable StorageEntry storageEntry) {
        mStorageEntry = storageEntry;
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public CharSequence getSummary() {
        return StorageHealthUtils.getRemainingLifetimeSummary(
                mContext, getRemainingLifetimePercent());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getSummary());
        preference.setVisible(isAvailable()
                && mStorageEntry != null
                && mStorageEntry.isDefaultInternalStorage());
    }
}
