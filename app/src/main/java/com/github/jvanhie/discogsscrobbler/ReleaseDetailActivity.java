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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;

import java.util.List;


/**
 * An activity representing a single Release detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ReleaseListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ReleaseDetailFragment}.
 */
public class ReleaseDetailActivity extends DrawerActivity implements ReleaseVersionsFragment.Callbacks{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_detail);

        if (savedInstanceState == null) {
            // Create the release pager fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID,
                    getIntent().getLongExtra(ReleaseDetailFragment.ARG_ITEM_ID,0));
            ReleasePagerFragment fragment = new ReleasePagerFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.release_pager_container, fragment)
                    .commit();
        }
        //set navigation drawer
        setDrawer(R.id.detail_drawer_layout,R.id.detail_drawer,getTitle().toString(),getTitle().toString(),false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            //up button pressed, in the detailview we want this implemented as back (goes to certain selected item in release or search list)
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(long id) {
        Intent detailIntent = new Intent(this, ReleaseDetailActivity.class);
        detailIntent.putExtra(ReleaseDetailFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
