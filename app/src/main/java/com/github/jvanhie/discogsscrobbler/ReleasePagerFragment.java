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
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Jono on 04/04/2014.
 */
public class ReleasePagerFragment extends Fragment {

    public static String HAS_MENU = "has_menu";
    public static String SHOW_VERSIONS =" show_versions";
    public static String SHOW_TRACKLIST =" show_tracklist";
    public static final String ARG_ITEM_ID = "item_id";

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private ReleaseVersionsFragment mVersionsFragment;
    private ReleaseDetailFragment mDetailFragment;
    private ReleaseTracklistFragment mTrackListFragment;

    private long mReleaseId;
    private boolean mShowVersions;
    private boolean mShowTracklist;

    public ReleasePagerFragment() {

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReleaseId = getArguments().getLong(ARG_ITEM_ID,0);
        mShowVersions = getArguments().getBoolean(SHOW_VERSIONS,true);
        mShowTracklist = getArguments().getBoolean(SHOW_TRACKLIST,true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_release_pager, container, false);

        //add fragment in a pager
        mPager = (ViewPager) rootView.findViewById(R.id.detail_pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        if(mShowVersions) {
            mPager.setCurrentItem(1);
        }
        return rootView;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private String[] titles = { "versions", "info", "tracklist"};

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle arguments = new Bundle();
            arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID,mReleaseId);
            arguments.putBoolean(ReleaseDetailFragment.HAS_MENU,getArguments().getBoolean(HAS_MENU,true));
            //hackish way of not showing the first fragment
            if(!mShowVersions) position++;
            switch (position) {
                case 0:
                    if(mVersionsFragment == null) mVersionsFragment = new ReleaseVersionsFragment();
                    mVersionsFragment.setArguments(arguments);
                    return mVersionsFragment;
                case 1:
                    if(mDetailFragment == null) mDetailFragment = new ReleaseDetailFragment();
                    mDetailFragment.setArguments(arguments);
                    return mDetailFragment;
                case 2:
                    if(mTrackListFragment == null) mTrackListFragment = new ReleaseTracklistFragment();
                    mTrackListFragment.setArguments(arguments);
                    return mTrackListFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            int count = titles.length;
            if(!mShowVersions) count--;
            if(!mShowTracklist) count--;
            return count;
        }



        @Override
        public CharSequence getPageTitle(int position) {
            if(!mShowVersions) return titles[position+1];
            else return titles[position];
        }

    }

    public void addToDiscogs() {
        int pos = mPager.getCurrentItem();
        if(mShowVersions) pos--;
        if(pos == 0 && mDetailFragment!=null) {
            mDetailFragment.addToDiscogs();
        } else if (pos ==1 && mTrackListFragment!=null){
            mTrackListFragment.addToDiscogs();
        }
    }

    public void scrobble() {
        int pos = mPager.getCurrentItem();
        if(mShowVersions) pos--;
        if(pos == 0 && mDetailFragment!=null) {
            mDetailFragment.scrobble();
        } else if (pos ==1 && mTrackListFragment!=null){
            mTrackListFragment.scrobble();
        }
    }
}

