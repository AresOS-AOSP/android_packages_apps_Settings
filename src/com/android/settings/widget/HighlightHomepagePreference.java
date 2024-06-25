/*
 * Copyright (C) 2023-2024 The risingOS Android Project
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

package com.android.settings.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.airbnb.lottie.LottieAnimationView;
import com.android.settings.R;

import java.util.HashMap;
import java.util.Map;

/** A customized layout for highlight homepage preference. */
public class HighlightHomepagePreference extends HomepagePreference implements
        HomepagePreferenceLayoutHelper.HomepagePreferenceLayout {
        
    private static final String PERSONALIZE_KEY = "top_level_personalisation";

    private LottieAnimationView animationView;

    private final HomepagePreferenceLayoutHelper mHelper;

    private static final Map<String, Integer> preferenceAnimations = new HashMap<>();
    static {
        preferenceAnimations.put(PERSONALIZE_KEY, R.raw.rattan);
    }

    public HighlightHomepagePreference(Context context, AttributeSet attrs, int defStyleAttr,
                                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHelper = new HomepagePreferenceLayoutHelper(this);
        init();
    }

    public HighlightHomepagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new HomepagePreferenceLayoutHelper(this);
        init();
    }

    public HighlightHomepagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHelper = new HomepagePreferenceLayoutHelper(this);
        init();
    }

    public HighlightHomepagePreference(Context context) {
        super(context);
        mHelper = new HomepagePreferenceLayoutHelper(this);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.top_level_preference_highlight_card);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHelper.onBindViewHolder(holder);
        animationView = (LottieAnimationView) holder.findViewById(R.id.highlight_lottie_animation_holder);
        String preferenceKey = getKey();
        if (preferenceAnimations.containsKey(preferenceKey) && animationView != null) {
            int animationResId = preferenceAnimations.get(preferenceKey);
            animationView.setAnimation(animationResId);
            if (PERSONALIZE_KEY.equals(preferenceKey)) {
                int maxFrame = getContext().getResources().getInteger(R.integer.personalize_max_frame);
                int minFrame = getContext().getResources().getInteger(R.integer.personalize_min_frame);
                animationView.setMinAndMaxFrame(minFrame, maxFrame);
            }
            animationView.playAnimation();
        }
    }

    @Override
    public HomepagePreferenceLayoutHelper getHelper() {
        return mHelper;
    }
}
