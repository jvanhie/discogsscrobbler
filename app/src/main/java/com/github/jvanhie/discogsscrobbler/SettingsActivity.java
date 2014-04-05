/*
 * Copyright (c) 2014 Jono Vanhie-Van Gerwen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jvanhie.discogsscrobbler;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;


import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if(s.equals("enable_discogs")) {
                    if(!sharedPreferences.getBoolean(s,true)) {
                        //user has disabled discogs support, log out!
                        Discogs.getInstance(SettingsActivity.this).logOut();
                    }
                }

                if(s.equals("enable_lastfm")) {
                    if(!sharedPreferences.getBoolean(s,true)) {
                        //user has disabled lastfm support, log out!
                        Lastfm.getInstance(SettingsActivity.this).logOut();
                    }
                }

                if(s.equals("discogs_api_id")) {
                    int choice = Integer.parseInt(sharedPreferences.getString(s,"0"));
                    int current = Discogs.getInstance(SettingsActivity.this).getApiId();
                    if(choice!=current) {
                        if (choice == -1) {
                            startActivity(new Intent(SettingsActivity.this, DiscogsApiActivity.class));
                        } else {
                            Discogs.getInstance(SettingsActivity.this).setApiId(choice);
                        }
                    }
                }
            }
        });



    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        //add lastfm preference
        addPreferencesFromResource(R.xml.pref_lastfm);
        // Add 'discogs' preferences.
        addPreferencesFromResource(R.xml.pref_discogs);


        ListPreference api = (ListPreference) findPreference("discogs_api_id");
        int size = getResources().getStringArray(R.array.discogs_api_keys).length;
        String[] entries = new String[size+1];
        String[] values = new String[size+1];
        for (int i = 0; i <= size; i++) {
            if(i==0) entries[0] = "custom";
            else entries[i] = "" + (i);
            values[i] = "" + (i-1);
        }
        api.setEntries(entries);
        api.setEntryValues(values);


        Preference button = findPreference("discogs_create_api_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                Intent apiIntent = new Intent(SettingsActivity.this, DiscogsApiActivity.class);
                apiIntent.putExtra(DiscogsApiActivity.ARG_API_MODE, "create");
                startActivity(apiIntent);
                return true;
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /** {@inheritDoc} */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DiscogsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_discogs);

            ListPreference api = (ListPreference) findPreference("discogs_api_id");
            int size = getResources().getStringArray(R.array.discogs_api_keys).length;
            String[] entries = new String[size+1];
            String[] values = new String[size+1];
            for (int i = 0; i <= size; i++) {
                if(i==0) entries[0] = "custom";
                else entries[i] = "" + (i);
                values[i] = "" + (i-1);
            }
            api.setEntries(entries);
            api.setEntryValues(values);

        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LastfmPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_lastfm);
        }
    }

    protected boolean isValidFragment (String fragmentName)
    {
        if(LastfmPreferenceFragment.class.getName().equals(fragmentName) || DiscogsPreferenceFragment.class.getName().equals(fragmentName))
            return true;
        return false;

    }
}
