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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.adapters.ReleaseAdapter;
import com.github.jvanhie.discogsscrobbler.adapters.SearchAdapter;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchResult;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.util.ArrayList;
import java.util.List;

/**
 * A list fragment representing a list of Releases. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */


public class SearchFragment extends Fragment {

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

    private ExpandableListView mList;
    private SearchAdapter mAdapter;
    private Discogs mDiscogs;

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
        public void setRefreshVisible(boolean visible) ;
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }

        public void setRefreshVisible(boolean visible) {

        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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

        mList = new ExpandableListView(getActivity());
        mList.setAdapter(mAdapter);

        mList.setOnGroupClickListener(new GroupClickHandler());

        mList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
                onItemSelected(l);
                return false;
            }
        });

        if (mDiscogs == null) mDiscogs = Discogs.getInstance(getActivity());

        mList.setGroupIndicator(null);

        return mList;
    }

    public void search(String s) {
        mCallbacks.setRefreshVisible(true);
        mDiscogs.search(s,new Discogs.DiscogsDataWaiter<List<DiscogsSearchResult>>() {
            @Override
            public void onResult(boolean success, List<DiscogsSearchResult> data) {
                if(success) {
                    //show results
                    mAdapter = new SearchAdapter(getActivity(),data);
                    mList.setAdapter(mAdapter);
                }
                mCallbacks.setRefreshVisible(false);
            }
        });
    }

    public void search(String s, String type) {
        mCallbacks.setRefreshVisible(true);
        mDiscogs.search(s,type,new Discogs.DiscogsDataWaiter<List<DiscogsSearchResult>>() {
            @Override
            public void onResult(boolean success, List<DiscogsSearchResult> data) {
                if(success) {
                    //show results
                    mAdapter = new SearchAdapter(getActivity(),data);
                    mList.setAdapter(mAdapter);
                }
                mCallbacks.setRefreshVisible(false);
            }
        });
    }

    public void searchBarcode(String barcode) {
        mCallbacks.setRefreshVisible(true);
        mDiscogs.searchBarcode(barcode,new Discogs.DiscogsDataWaiter<List<DiscogsSearchResult>>() {
            @Override
            public void onResult(boolean success, List<DiscogsSearchResult> data) {
                if(success) {
                    //show results
                    mAdapter = new SearchAdapter(getActivity(),data);
                    mList.setAdapter(mAdapter);
                }
                mCallbacks.setRefreshVisible(false);
            }
        });
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

    public void onItemSelected(long id) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        mList.setChoiceMode(activateOnItemClick
                ? AbsListView.CHOICE_MODE_SINGLE
                : AbsListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == AbsListView.INVALID_POSITION) {
            mList.setItemChecked(mActivatedPosition, false);
        } else {
            mList.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private class GroupClickHandler implements ExpandableListView.OnGroupClickListener {
        @Override
        public boolean onGroupClick(ExpandableListView expandableListView, View view, final int i, long l) {
            DiscogsSearchResult result = (DiscogsSearchResult)mAdapter.getGroup(i);
            if(result.type.equals("release")) {
                onItemSelected(result.id);
            } else {
                //this is a group item, fetch it's content and add it dynamically to the list if collapsed and first click, else let the listview do it's magic
                if(mList.isGroupExpanded(i) || mAdapter.getChildrenCount(i) != 0) return false;
                mCallbacks.setRefreshVisible(true);
                if(result.type.equals("artist")) {
                    mDiscogs.getArtistReleases(result.id, new Discogs.DiscogsDataWaiter<List<DiscogsSearchRelease>>() {
                        @Override
                        public void onResult(boolean success, List<DiscogsSearchRelease> data) {
                            if(success) {
                                mAdapter.addChildren(i,data);
                                mList.expandGroup(i);
                            }
                            mCallbacks.setRefreshVisible(false);
                        }
                    });
                } else if(result.type.equals("label")) {
                    mDiscogs.getLabelReleases(result.id, new Discogs.DiscogsDataWaiter<List<DiscogsSearchRelease>>() {
                        @Override
                        public void onResult(boolean success, List<DiscogsSearchRelease> data) {
                            if (success) {
                                mAdapter.addChildren(i, data);
                                mList.expandGroup(i);
                            }
                            mCallbacks.setRefreshVisible(false);
                        }
                    });
                } else if(result.type.equals("master")) {
                    mDiscogs.getMasterReleases(result.id, new Discogs.DiscogsDataWaiter<List<DiscogsSearchRelease>>() {
                        @Override
                        public void onResult(boolean success, List<DiscogsSearchRelease> data) {
                            if (success) {
                                mAdapter.addChildren(i, data);
                                mList.expandGroup(i);
                            }
                            mCallbacks.setRefreshVisible(false);
                        }
                    });
                }

            }
            //I handled it
            return true;
        }
    }
}
