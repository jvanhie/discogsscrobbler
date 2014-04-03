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
import android.widget.ProgressBar;
import android.widget.SearchView;


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
    private boolean mTwoPane;

    private SearchFragment mSearchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        mSearchFragment = ((SearchFragment) getSupportFragmentManager().findFragmentById(R.id.search_list));

        if (findViewById(R.id.release_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            mSearchFragment.setActivateOnItemClick(true);
        }
        //create navigation drawer
        setDrawer(R.id.search_drawer_layout,R.id.search_drawer,getTitle().toString(),getTitle().toString(),true);

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.discogs_search, menu);
        //configure search box
        SearchView searchView = (SearchView)menu.findItem(R.id.search_field).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                menu.findItem(R.id.search_field).collapseActionView();
                //pass query to search fragment
                mSearchFragment.search(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
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
     * Callback method from {@link com.github.jvanhie.discogsscrobbler.ReleaseListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(long id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID, id);
            ReleaseDetailFragment fragment = new ReleaseDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.release_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.


            Intent detailIntent = new Intent(this, ReleaseDetailActivity.class);
            detailIntent.putExtra(ReleaseDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
