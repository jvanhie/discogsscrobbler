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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;

import java.util.List;

/**
 * Created by Jono on 04/04/2014.
 */
public class ReleasePagerFragment extends Fragment {
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private ReleaseVersionsFragment mVersionsFragment;
    private ReleaseDetailFragment mDetailFragment;
    private ReleaseTracklistFragment mTrackListFragment;
    private long mReleaseId;


    public ReleasePagerFragment() {

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReleaseId = getArguments().getLong(ReleaseDetailFragment.ARG_ITEM_ID,0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_release_pager, container, false);

        //add detail and tracklist fragment in a pager
        mPager = (ViewPager) rootView.findViewById(R.id.detail_pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(1);
        return rootView;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final String[] titles = { "versions", "info", "tracklist"};

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle arguments = new Bundle();
            arguments.putLong(ReleaseDetailFragment.ARG_ITEM_ID,mReleaseId);
            switch (position) {
                case 0:
                    if(mVersionsFragment == null) mVersionsFragment = new ReleaseVersionsFragment();
                    mVersionsFragment.setArguments(arguments);
                    return mVersionsFragment;
                case 1:
                    if(mDetailFragment == null) mDetailFragment = new ReleaseDetailFragment(true);
                    mDetailFragment.setArguments(arguments);
                    return mDetailFragment;
                case 2:
                    if(mTrackListFragment == null) mTrackListFragment = new ReleaseTracklistFragment(true);
                    mTrackListFragment.setArguments(arguments);
                    return mTrackListFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }



        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

    }
}

