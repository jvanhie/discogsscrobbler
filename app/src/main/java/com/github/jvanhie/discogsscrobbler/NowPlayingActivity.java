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
public class NowPlayingActivity extends DrawerActivity implements RecentlyPlayedFragment.Callbacks, NowPlayingFragment.Callbacks{

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private ReleaseDetailFragment mDetailFragment;
    private NowPlayingFragment mNowPlayingFragment;
    private RecentlyPlayedFragment mRecentlyPlayedFragment;
    private long mReleaseId;
    private boolean mReleaseChanged = false;
    private int mPanes = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        //check if we're in tablet mode -> two or tripane layout
        if (findViewById(R.id.detail_container) != null) {
            mPanes = 3;
        } else if (findViewById(R.id.recently_played_container) != null) {
            mPanes = 2;
        }

        System.out.println("panes: " + mPanes);

        //initialize pager
        mPager = (ViewPager) findViewById(R.id.now_playing_pager);
        setPager();

        //set navigation drawer
        setDrawer(R.id.now_playing_drawer_layout, R.id.now_playing_drawer, getTitle().toString(), getTitle().toString(), true);
    }

    private void setPager() {
        if(mPager != null) {
            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            if (mReleaseId != 0) {
                mPager.setCurrentItem(1);
            }
            if(mPanes == 2) {
                if(mRecentlyPlayedFragment == null) mRecentlyPlayedFragment = new RecentlyPlayedFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.recently_played_container, mRecentlyPlayedFragment).commit();
            }
        } else {
            //3 pane goodness, no pagers needed
            //if(mReleaseId!=0 && mDetailFragment == null) {
                System.out.println("release found! " + mReleaseId);
                mDetailFragment = new ReleaseDetailFragment();
                Bundle arguments = new Bundle();
                arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID, mReleaseId);
                mDetailFragment.setArguments(arguments);
                //getSupportFragmentManager().beginTransaction().replace(R.id.detail_container, mDetailFragment).commit();
            //}
            mNowPlayingFragment = new NowPlayingFragment();
            if(mRecentlyPlayedFragment == null) mRecentlyPlayedFragment = new RecentlyPlayedFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.now_playing_container,mNowPlayingFragment)
                    .replace(R.id.detail_container, mDetailFragment)
                    .replace(R.id.recently_played_container, mRecentlyPlayedFragment)
                    .commit();

        }
    }

    @Override
    public void onReleaseSet(long id) {
        if(mReleaseId != id) {
            mReleaseId = id;
            setPager();
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final String[] titles = { "info", "now playing","recently played"};

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //hackish way of skipping the info fragment if we don't have a release id
            if(mReleaseId==0) position++;
            switch (position) {
                case 0:
                        if (mDetailFragment == null) {
                            mDetailFragment = new ReleaseDetailFragment();
                            Bundle arguments = new Bundle();
                            arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID, mReleaseId);
                            mDetailFragment.setArguments(arguments);
                        }
                        return mDetailFragment;
                case 1:
                    //force recreation of now playing tracklist
                    mNowPlayingFragment = new NowPlayingFragment();
                    return mNowPlayingFragment;
                case 2:
                    if(mRecentlyPlayedFragment == null) mRecentlyPlayedFragment = new RecentlyPlayedFragment();
                    return mRecentlyPlayedFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            int count = titles.length;
            if(mReleaseId==0) count--;
            if(mPanes==2) count--;
            return count;
        }



        @Override
        public CharSequence getPageTitle(int position) {
            if(mReleaseId==0) return titles[position+1];
            else return titles[position];
        }

    }

    @Override
    public void onItemSelected(long id) {
        Intent detailIntent = new Intent(this, ReleaseDetailActivity.class);
        detailIntent.putExtra(ReleaseDetailFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
