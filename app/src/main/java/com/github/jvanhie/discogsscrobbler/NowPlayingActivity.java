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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.View;


/**
 * An activity representing a single Release detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link com.github.jvanhie.discogsscrobbler.ReleaseListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailFragment}.
 */
public class NowPlayingActivity extends DrawerActivity {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private NowPlayingFragment mNowPlayingFragment;
    private long mReleaseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        /*
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
        */
        //add now playing fragment in a pager
        mPager = (ViewPager) findViewById(R.id.now_playing_pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        //hide de page tab as long as we don't have more to show than 1 fragment
        findViewById(R.id.now_playing_pager_strip).setVisibility(View.GONE);
        //set navigation drawer
        setDrawer(R.id.now_playing_drawer_layout, R.id.now_playing_drawer, getTitle().toString(), getTitle().toString(), true);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final String[] titles = { "now playing"};

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if(mNowPlayingFragment == null) mNowPlayingFragment = new NowPlayingFragment();
                    return mNowPlayingFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }



        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

    }
}
