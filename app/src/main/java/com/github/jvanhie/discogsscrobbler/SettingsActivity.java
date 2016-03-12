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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;

import java.util.List;

public class SettingsActivity extends DrawerActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
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
        setContentView(R.layout.activity_settings);

        //create navigation drawer
        setDrawer(R.id.settings_drawer_layout, R.id.settings_drawer, getTitle().toString(), getTitle().toString(), true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,String s)
    {
        System.out.println(s);
        if (s.equals("enable_discogs")) {
            if (!sharedPreferences.getBoolean(s, true)) {
                //user has logged out of Discogs
                Discogs.getInstance(SettingsActivity.this).logOut();
            } else {
                Discogs.getInstance(SettingsActivity.this).logIn();
            }
        }

        if (s.equals("enable_lastfm")) {
            if (!sharedPreferences.getBoolean(s, true)) {
                //user has logged out of last.fm
                Lastfm.getInstance(SettingsActivity.this).logOut();
            } else {
                Lastfm.getInstance(SettingsActivity.this).logIn(new Lastfm.LastfmWaiter() {
                    @Override
                    public void onResult(boolean success) {
                    }
                });
            }
        }

        if (s.equals("discogs_api_id")) {
            int choice = Integer.parseInt(sharedPreferences.getString(s, "0"));
            int current = Discogs.getInstance(SettingsActivity.this).getApiId();
            if (choice != current) {
                if (choice == -1) {
                    startActivity(new Intent(SettingsActivity.this, DiscogsApiActivity.class));
                } else {
                    Discogs.getInstance(SettingsActivity.this).setApiId(choice);
                }
            }
        }
    }

    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class DiscogsPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.pref_discogs);

            SwitchPreferenceCompat sw = (SwitchPreferenceCompat) findPreference("enable_discogs");
            Discogs discogs = Discogs.getInstance(getContext());
            boolean loggedin = discogs.isLoggedIn();
            sw.setChecked(loggedin);
            if(loggedin) {
                sw.setSummaryOn("Signed in as " + discogs.getUser());
            }

            Preference preloadButton = findPreference("discogs_preload_collection");
            preloadButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Discogs.getInstance(getActivity()).preloadCollection();
                    return true;
                }
            });

        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LastfmPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.pref_lastfm);
            SwitchPreferenceCompat sw = (SwitchPreferenceCompat) findPreference("enable_lastfm");
            Lastfm lastfm = Lastfm.getInstance(getContext());
            boolean loggedin = lastfm.isLoggedIn();
            sw.setChecked(loggedin);
            if(loggedin) {
                sw.setSummaryOn("Signed in as " + lastfm.getUserName());
            }
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AdvancedPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.pref_advanced);

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
                    Intent apiIntent = new Intent(getActivity(), DiscogsApiActivity.class);
                    apiIntent.putExtra(DiscogsApiActivity.ARG_API_MODE, "create");
                    startActivity(apiIntent);
                    return true;
                }
            });


            Preference sellerButton = findPreference("discogs_seller_settings_button");
            sellerButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.discogs_seller_dialog_message)).setTitle(getString(R.string.discogs_seller_dialog_title));
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.discogs.com/settings/seller")));
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog don't do anything
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }
            });
        }
    }
}
