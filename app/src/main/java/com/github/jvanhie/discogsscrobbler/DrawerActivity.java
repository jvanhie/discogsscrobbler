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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.github.jvanhie.discogsscrobbler.adapters.NavDrawerListAdapter;

import java.util.ArrayList;

/**
 * Created by Jono on 01/04/2014.
 */
public class DrawerActivity extends FragmentActivity {

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    public void setDrawer(int layout, int list, String title, String drawerTitle, boolean showDrawerIcon) {
        mTitle = title;
        mDrawerTitle = drawerTitle;
        mDrawerLayout = (DrawerLayout) findViewById(layout);
        //populate the navigation drawer
        mDrawerList = (ListView) findViewById(list);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // load slide menu items
        String[] navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);
        // nav drawer icons from resources
        TypedArray navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);
        NavDrawerListAdapter adapter = new NavDrawerListAdapter(this);
        for(int i = 0; i < navMenuTitles.length ; i++) {
            adapter.addItem(navMenuTitles[i],navMenuIcons.getResourceId(i,-1));
        }
        // Set the adapter for the list view
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0: //collection
                        startActivity(new Intent(DrawerActivity.this, ReleaseListActivity.class));
                        break;
                    case 1: //search
                        startActivity(new Intent(DrawerActivity.this, SearchActivity.class));
                        break;
                    case 2: //now playing
                        if(!PreferenceManager.getDefaultSharedPreferences(DrawerActivity.this).getBoolean("enable_lastfm",true)) {
                            //the item only works with lastfm enabled

                        }
                        break;
                    case 3: //settings
                        startActivity(new Intent(DrawerActivity.this, SettingsActivity.class));
                        break;
                    case 4: //help

                        break;
                }
            }
        });


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ){
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(showDrawerIcon);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        for(int i = 0 ; i < menu.size(); i++) {
            menu.getItem(i).setVisible(!drawerOpen);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}
