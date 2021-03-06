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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.github.jvanhie.discogsscrobbler.models.Folder;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.util.List;


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

    private Discogs mDiscogs;
    private ReleaseListFragment mReleaseList;

    //all possible extra fragments in tablet mode
    private ReleasePagerFragment mReleasePager;
    private ReleaseDetailFragment mReleaseDetail;
    private ReleaseTracklistFragment mReleaseTracklist;

    private ProgressBar mReleaseProgressBar;
    private ProgressBar mRefreshProgressBar;

    private boolean mLoaded = false;
    private boolean mRefresh = false;
    private boolean mFolders = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_discogs, false);

        setContentView(R.layout.activity_release_list);

        mReleaseProgressBar = (ProgressBar) findViewById(R.id.release_list_progressBar);
        mRefreshProgressBar = (ProgressBar) findViewById(R.id.release_list_refresh);
        mReleaseList = ((ReleaseListFragment) getSupportFragmentManager().findFragmentById(R.id.release_list));

        if(mReleaseProgressBar != null && mLoaded) mReleaseProgressBar.setVisibility(View.INVISIBLE);
        if(mRefreshProgressBar != null && mRefresh) mRefreshProgressBar.setVisibility(View.VISIBLE);

        //check if we're in tablet mode -> two or tripane layout (hide details fields first, for a nice collection view)
        if (findViewById(R.id.release_pager_container) != null) {
            mPanes = 2;
            findViewById(R.id.release_pager_container).setVisibility(View.GONE);
        } else if (findViewById(R.id.release_tracklist_container) != null) {
            mPanes = 3;
            findViewById(R.id.release_detail_container).setVisibility(View.GONE);
            findViewById(R.id.release_tracklist_container).setVisibility(View.GONE);
        }

        // Restore the previously serialized activated release
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_RELEASE_SELECTED)) {
            if (savedInstanceState.getInt(STATE_PANES) != mPanes) {
                //if the panelayout has changed, forcibly reload the fragments
                onItemSelected(savedInstanceState.getLong(STATE_RELEASE_SELECTED));
            }
        }

        if(mDiscogs == null) mDiscogs = Discogs.getInstance(this);

        //create navigation drawer
        setDrawer(R.id.list_drawer_layout, R.id.list_drawer, getTitle().toString(), getTitle().toString(), true);


    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.discogs_list, menu);
        //configure search box
        final MenuItem search = menu.findItem(R.id.list_search);
        SearchView searchView = (SearchView) search.getActionView();
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
        searchView.setQueryHint("Filter your releases");
        final MenuItem filter = menu.findItem(R.id.list_filter);
        if(!mFolders) {
            mDiscogs.getFolders(new Discogs.DiscogsDataWaiter<List<Folder>>() {
                @Override
                public void onResult(boolean success, List<Folder> data) {
                    if(success && data != null) {
                        mFolders = true;
                        Spinner s = (Spinner) filter.getActionView(); // find the spinner
                        Context theme = getSupportActionBar().getThemedContext();
                        if(theme == null) return; //another check for a rare bug
                        ArrayAdapter<Folder> mSpinnerAdapter = new ArrayAdapter<Folder>(theme, android.R.layout.simple_spinner_dropdown_item, data);
                        s.setAdapter(mSpinnerAdapter); // set the adapter
                        s.setSelection(0, false);
                        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                mDiscogs.setFolderId(((Folder) adapterView.getItemAtPosition(i)).folderid);
                                //reload list with id
                                mReleaseList.loadList();
                                filter.collapseActionView();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {
                                //filter.collapseActionView();
                            }
                        });
                    }
                }
            });
        }

        //make sure only one actionview is expanded
        MenuItemCompat.setOnActionExpandListener(filter,new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                //collapse search
                search.collapseActionView();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                return true;
            }
        });
        MenuItemCompat.setOnActionExpandListener(search,new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                //collapse search
                filter.collapseActionView();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                return true;
            }
        });

        //s.setSelection(mSearchType,false);

        if (mSelected > 0) {
            inflater.inflate(R.menu.release_detail_scrobble, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.list_refresh) {
            //refresh the list
            mReleaseList.refreshCollection();
        }
        if(id == R.id.detail_scrobble_release) {
            //if tracklist is available, always scrobble from this view
            if(mPanes==3 && mReleaseTracklist!=null) {
                mReleaseTracklist.scrobble();
            } else if (mPanes==2 && mReleasePager!=null) {
                mReleasePager.scrobble();
            }
        }
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
                detailIntent.putExtra(ReleasePagerFragment.SHOW_VERSIONS, false);
                startActivity(detailIntent);
                break;
            case 2: //show the pager fragment next to the list
                invalidateOptionsMenu();
                Bundle arguments2 = new Bundle();
                arguments2.putLong(ReleaseDetailFragment.ARG_ITEM_ID, id);
                arguments2.putBoolean(ReleasePagerFragment.SHOW_VERSIONS, false);
                arguments2.putBoolean(ReleasePagerFragment.HAS_MENU, false);
                mReleasePager = new ReleasePagerFragment();
                mReleasePager.setArguments(arguments2);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.release_pager_container, mReleasePager)
                        .commit();
                findViewById(R.id.release_pager_container).setVisibility(View.VISIBLE);
                break;
            case 3: //whoa, screen estate! Show detail view _and_ tracklist
                invalidateOptionsMenu();
                Bundle arguments3 = new Bundle();
                arguments3.putLong(ReleaseDetailFragment.ARG_ITEM_ID, id);
                mReleaseDetail = new ReleaseDetailFragment();
                mReleaseDetail.setArguments(arguments3);
                Bundle arguments3_t = new Bundle();
                arguments3_t.putLong(ReleaseTracklistFragment.ARG_ITEM_ID, id);
                mReleaseTracklist = new ReleaseTracklistFragment();
                mReleaseTracklist.setArguments(arguments3_t);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.release_detail_container, mReleaseDetail)
                        .replace(R.id.release_tracklist_container, mReleaseTracklist)
                        .commit();
                findViewById(R.id.release_detail_container).setVisibility(View.VISIBLE);
                findViewById(R.id.release_tracklist_container).setVisibility(View.VISIBLE);
                break;
        }

    }

    @Override
    public void onAdapterSet() {
        mLoaded = true;
        if(mReleaseProgressBar!=null) mReleaseProgressBar.setVisibility(View.INVISIBLE);
    }

    public void setRefreshVisible(boolean visible) {
        mRefresh = visible;
        if(mRefreshProgressBar!=null) {
            if(visible) {
                mRefreshProgressBar.setVisibility(View.VISIBLE);
            } else {
                mRefreshProgressBar.setVisibility(View.GONE);
            }
        }
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
