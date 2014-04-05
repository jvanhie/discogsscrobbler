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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import java.util.ArrayList;


/**
 * An activity representing a list of Releases. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ReleaseDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ReleaseListFragment} and the item details
 * (if present) is a {@link ReleaseDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ReleaseListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ReleaseListActivity extends DrawerActivity
        implements ReleaseListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private int mPanes = 1;
    private static final String STATE_PANES = "collection_panes";

    private long mSelected;
    private static final String STATE_RELEASE_SELECTED = "selected_release";

    private ReleaseListFragment mReleaseList;

    private ProgressBar mReleaseProgressBar;

    private boolean mLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_discogs, false);

        setContentView(R.layout.activity_release_list);

        mReleaseProgressBar = (ProgressBar) findViewById(R.id.release_list_progressBar);
        mReleaseList = ((ReleaseListFragment) getSupportFragmentManager().findFragmentById(R.id.release_list));
        //single pane mode
        if(mReleaseProgressBar != null && mLoaded) mReleaseProgressBar.setVisibility(View.INVISIBLE);

        //check if we're in tablet mode -> two or tripane layout
        if (findViewById(R.id.release_pager_container) != null) {
            mPanes = 2;
        } else if (findViewById(R.id.release_tracklist_container) != null) {
            mPanes = 3;
        }

        // Restore the previously serialized activated release
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_RELEASE_SELECTED)) {
            if (savedInstanceState.getInt(STATE_PANES) != mPanes) {
                //if the panelayout has changed, forcibly reload the fragments
                onItemSelected(savedInstanceState.getLong(STATE_RELEASE_SELECTED));
            }
        }

        //create navigation drawer
        setDrawer(R.id.list_drawer_layout, R.id.list_drawer, getTitle().toString(), getTitle().toString(), true);


    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.discogs_list, menu);
        //configure search box
        SearchView searchView = (SearchView)menu.findItem(R.id.list_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                menu.findItem(R.id.list_search).collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mReleaseList.filter(s);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method from {@link ReleaseListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(long id) {
        mSelected = id;
        switch (mPanes) {
            case 1: //just start new activity with the details
                Intent detailIntent = new Intent(this, ReleaseDetailActivity.class);
                detailIntent.putExtra(ReleaseDetailFragment.ARG_ITEM_ID, id);
                startActivity(detailIntent);
                break;
            case 2: //show the pager fragment next to the list
                Bundle arguments2 = new Bundle();
                arguments2.putLong(ReleaseDetailFragment.ARG_ITEM_ID, id);
                ReleasePagerFragment fragment = new ReleasePagerFragment();
                fragment.setArguments(arguments2);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.release_pager_container, fragment)
                        .commit();
                break;
            case 3: //whoa, screen estate! Show detail view _and_ tracklist
                Bundle arguments3 = new Bundle();
                arguments3.putLong(ReleaseDetailFragment.ARG_ITEM_ID, id);
                ReleaseDetailFragment detailFragment = new ReleaseDetailFragment(false);
                detailFragment.setArguments(arguments3);
                ReleaseTracklistFragment tracklistFragment = new ReleaseTracklistFragment(true);
                tracklistFragment.setArguments(arguments3);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.release_detail_container, detailFragment)
                        .replace(R.id.release_tracklist_container, tracklistFragment)
                        .commit();
                break;
        }

    }

    @Override
    public void onAdapterSet() {
        mLoaded = true;
        if(mReleaseProgressBar!=null) mReleaseProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelected > 0) {
            // Serialize and persist the activated item position.
            outState.putLong(STATE_RELEASE_SELECTED, mSelected);
        }
        outState.putInt(STATE_PANES, mPanes);

    }
}
