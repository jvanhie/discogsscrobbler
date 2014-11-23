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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.adapters.ReleaseAdapter;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.util.ArrayList;
import java.util.List;

/**
 * A list fragment representing a list of Releases. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ReleaseDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */


public class ReleaseVersionsFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private Release mRelease;
    private List<Release> mReleases;

    private AbsListView mList;
    private Discogs mDiscogs;

    private int mPage = 1;
    private int mLoadpage = 1;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }

    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReleaseVersionsFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //setup list view
        mList = new ListView(getActivity());
        mList.setId(android.R.id.list);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onListItemClick(view,i,l);
            }
        });

        //create superframe for adding list and empty view
        FrameLayout superFrame = new FrameLayout(getActivity());
        FrameLayout.LayoutParams layoutparams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        superFrame.setLayoutParams(layoutparams);
        View emptyView = inflater.inflate(R.layout.fragment_empty, container, false);
        ((TextView)emptyView.findViewById(R.id.empty_heading)).setText("No versions found");
        ((TextView)emptyView.findViewById(R.id.empty_text)).setText("No alternative versions of this release were found, this could also mean Discogs did not add a master id to this release");

        /*initialize list with local discogs collection*/
        if(mDiscogs==null) mDiscogs = Discogs.getInstance(getActivity());
        if(mRelease==null) {
            mDiscogs.getRelease(getArguments().getLong(ARG_ITEM_ID, 0), new Discogs.DiscogsDataWaiter<Release>() {
                @Override
                public void onResult(boolean success, Release data) {
                    if (success) {
                        mRelease = data;
                        loadList();
                    }
                }
            });
        } else {
            loadList();
        }

        superFrame.addView(emptyView);
        mList.setEmptyView(emptyView);
        superFrame.addView(mList);

        return superFrame;
    }

    public void loadList() {
        if(mReleases == null || mPage!=mLoadpage) {
            mDiscogs.getMasterReleases(mRelease.master_id,mLoadpage, new Discogs.DiscogsDataWaiter<List<DiscogsSearchRelease>>() {
                @Override
                public void onResult(boolean success, List<DiscogsSearchRelease> data) {
                    if (success && data != null) {
                        mPage=mLoadpage;
                        if(mReleases== null) {
                            mReleases = new ArrayList<Release>();
                        } else {
                            //it's an additional load, remove the previous loader
                            mReleases.remove(mReleases.size()-1);
                        }
                        for (DiscogsSearchRelease r : data) {
                            //create release object that releaseadapter will understand (discogs does not provide artist name with it's master versions call, get it from the current release)
                            Release release = new Release(r);
                            release.artist = mRelease.artist;
                            //don't add the current release
                            if (release.releaseid != mRelease.releaseid) {
                                mReleases.add(release);
                            }

                        }
                        if (mReleases.size() != 0) {
                            if (mList.getAdapter() == null) {
                                mList.setAdapter(new ReleaseAdapter(getActivity(), mReleases));
                            } else {
                                ((ReleaseAdapter) mList.getAdapter()).updateReleases(mReleases);
                            }
                        }
                    }
                }
            });
        }else {
            if (mReleases.size() != 0) {
                if (mList.getAdapter() == null) {
                    mList.setAdapter(new ReleaseAdapter(getActivity(), mReleases));
                } else {
                    ((ReleaseAdapter) mList.getAdapter()).updateReleases(mReleases);
                }
            }
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    public void onListItemClick(View view, int position, long id) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        if(id==0) {
            //it's a loader, get the next page
            mLoadpage=mPage+1;
            loadList();
        } else {
            mCallbacks.onItemSelected(id);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    private void setActivatedPosition(int position) {
        if (position == AbsListView.INVALID_POSITION) {
            mList.setItemChecked(mActivatedPosition, false);
        } else {
            mList.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
