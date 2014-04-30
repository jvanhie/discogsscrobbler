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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;


/**
 * An activity representing a list of Releases. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link com.github.jvanhie.discogsscrobbler.ReleaseListFragment} and the item details
 * (if present) is a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link com.github.jvanhie.discogsscrobbler.ReleaseListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class SearchActivity extends DrawerActivity
        implements SearchFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private int BARCODE_REQUEST_CODE = 666;
    private int mPanes = 1;
    private static final String STATE_PANES = "collection_panes";

    private long mSelected;
    private static final String STATE_RELEASE_SELECTED = "selected_release";

    private SearchFragment mSearchFragment;
    private SearchView mSearchView;
    private ProgressBar mRefreshProgressBar;

    private int mSearchType;
    private String[] mSearchTypes;
    private static final String SEARCH_TYPE = "search_type";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        mSearchFragment = ((SearchFragment) getSupportFragmentManager().findFragmentById(R.id.search_list));
        mRefreshProgressBar = (ProgressBar) findViewById(R.id.search_refresh);

        //check if we're in tablet mode -> two or tripane layout
        if (findViewById(R.id.release_pager_container) != null) {
            mPanes = 2;
        } else if (findViewById(R.id.release_tracklist_container) != null) {
            mPanes = 3;
        }

        mSearchFragment.setActivateOnItemClick(true);

        // Restore the previously serialized activated release
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_RELEASE_SELECTED)) {
            if(savedInstanceState.getInt(STATE_PANES) != mPanes) {
                //if the panelayout has changed, forcibly reload the fragments
                onItemSelected(savedInstanceState.getLong(STATE_RELEASE_SELECTED));
            }
        }

        // set search type
        mSearchType = PreferenceManager.getDefaultSharedPreferences(this).getInt(SEARCH_TYPE,0);
        mSearchTypes = getResources().getStringArray(R.array.search_filter_items);

        //create navigation drawer
        setDrawer(R.id.search_drawer_layout,R.id.search_drawer,getTitle().toString(),getTitle().toString(),true);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.discogs_search, menu);
        //configure search box
        final MenuItem search = menu.findItem(R.id.search_field);
        mSearchView = (SearchView)search.getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                menu.findItem(R.id.search_field).collapseActionView();
                //pass query to search fragment
                if(mSearchType>0) {
                    mSearchFragment.search(s,mSearchTypes[mSearchType]);
                } else {
                    mSearchFragment.search(s);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        mSearchView.setQueryHint("Search Discogs");
        //config filter spinner
        final MenuItem filter = menu.findItem(R.id.search_filter);
        Spinner s = (Spinner) filter.getActionView(); // find the spinner
        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(getActionBar()
                .getThemedContext(), R.array.search_filter_items, android.R.layout.simple_spinner_dropdown_item); //  create the adapter from a StringArray
        s.setAdapter(mSpinnerAdapter); // set the adapter
        s.setSelection(mSearchType,false);

        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSearchType = i;
                PreferenceManager.getDefaultSharedPreferences(SearchActivity.this).edit().putInt(SEARCH_TYPE,mSearchType).apply();
                if(i>0) {
                    mSearchView.setQueryHint("Search Discogs (" + mSearchTypes[i] + ")");
                } else {
                    mSearchView.setQueryHint("Search Discogs");
                }
                filter.collapseActionView();
                search.expandActionView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //filter.collapseActionView();
            }
        });
        //make sure only one actionview is expanded
        filter.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
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
        search.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
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

        //only expand when the drawer is closed
        if(!((DrawerLayout)findViewById(R.id.search_drawer_layout)).isDrawerOpen(findViewById(R.id.search_drawer))) {
            search.expandActionView();
        }

        return true;
    }

    public void setRefreshVisible(boolean visible) {
        if(mRefreshProgressBar!=null) {
            if(visible) {
                mRefreshProgressBar.setVisibility(View.VISIBLE);
            } else {
                mRefreshProgressBar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.search_barcode) {
            //start external barcode scanner intent
            try {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.setPackage("com.google.zxing.client.android");
                intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
                startActivityForResult(intent, BARCODE_REQUEST_CODE);
            } catch (ActivityNotFoundException exc) {
                //TODO: give users the option to download it
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method from {@link com.github.jvanhie.discogsscrobbler.ReleaseListFragment.Callbacks}
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelected > 0) {
            // Serialize and persist the activated item position.
            outState.putLong(STATE_RELEASE_SELECTED, mSelected);
        }
        outState.putInt(STATE_PANES, mPanes);

    }


    @Override //is called when the barcode scan activity returns
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && requestCode == BARCODE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String barcode = data.getStringExtra("SCAN_RESULT");
                mSearchView.setQuery(barcode,false);
                mSearchFragment.searchBarcode(barcode);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    };
}
