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
public class NowPlayingActivity extends DrawerActivity implements RecentlyPlayedFragment.Callbacks{

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private NowPlayingFragment mNowPlayingFragment;
    private RecentlyPlayedFragment mRecentlyPlayedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        //add now playing and recently played fragment in a pager
        mPager = (ViewPager) findViewById(R.id.now_playing_pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        //set navigation drawer
        setDrawer(R.id.now_playing_drawer_layout, R.id.now_playing_drawer, getTitle().toString(), getTitle().toString(), true);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final String[] titles = { "now playing","recently played"};

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if(mNowPlayingFragment == null) mNowPlayingFragment = new NowPlayingFragment();
                    return mNowPlayingFragment;
                case 1:
                    if(mRecentlyPlayedFragment == null) mRecentlyPlayedFragment = new RecentlyPlayedFragment();
                    return mRecentlyPlayedFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }



        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

    }

    @Override
    public void onItemSelected(long id) {
        Intent detailIntent = new Intent(this, ReleaseDetailActivity.class);
        detailIntent.putExtra(ReleaseDetailFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
