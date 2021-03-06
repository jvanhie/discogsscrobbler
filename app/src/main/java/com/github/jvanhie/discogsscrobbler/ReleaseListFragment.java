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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.adapters.ReleaseAdapter;
import com.github.jvanhie.discogsscrobbler.models.Release;
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


public class ReleaseListFragment extends Fragment {

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

    private AbsListView mList;
    private Discogs mDiscogs;

    //we'll need this to determine our width in dp;
    private float logicalDensity;
    private double artSize = 1.0;
    private boolean mGrid = false;

    private TextView mEmptyHeading;
    private TextView mEmptyText;

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
        public void onAdapterSet();
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

        @Override
        public void onAdapterSet() {

        }

        @Override
        public void setRefreshVisible(boolean visible) {

        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReleaseListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        logicalDensity = metrics.density;
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

        String layout = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("collection_view","");
        if(layout.equals("list")) {
            //setup list view
            mList = new ListView(getActivity());
            mList.setId(android.R.id.list);
        } else {
            mList = new GridView(getActivity());
            mList.setId(android.R.id.list);
            mGrid = true;
            try {
                String scaling = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("grid_size", "100%");
                scaling=scaling.replace("%","");
                artSize = Integer.parseInt(scaling)/100.0;
            } catch(NumberFormatException e) {
                Toast.makeText(getActivity(),"Cannot parse grid size scaling, check your settings", Toast.LENGTH_SHORT).show();
            }
        }

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onListItemClick(view,i,l);
            }
        });

        mList.setFastScrollEnabled(true);

        //create superframe for adding list and empty view
        FrameLayout superFrame = new FrameLayout(getActivity());
        FrameLayout.LayoutParams layoutparams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        superFrame.setLayoutParams(layoutparams);
        View emptyView = inflater.inflate(R.layout.fragment_empty, container, false);
        mEmptyHeading = ((TextView)emptyView.findViewById(R.id.empty_heading));
        mEmptyHeading.setText("Empty collection");
        mEmptyText =((TextView)emptyView.findViewById(R.id.empty_text));
        mEmptyText.setText("Your collection appears to be empty, if this isn't an error, start by adding some releases via the search function or online");

        /*initialize list with local discogs collection*/
        if(mDiscogs==null) mDiscogs = Discogs.getInstance(getActivity());

        loadList();

        if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("collection_auto_refresh",true)) {
            //do a background call to update the discogs collection if necessary
            checkOnlineCollection();
        }

        superFrame.addView(emptyView);
        mList.setEmptyView(emptyView);
        superFrame.addView(mList);

        return superFrame;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //call listener to determine correct width when available or changed
        getView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int fragmentWidth = getView().getWidth();
                if(fragmentWidth > 0)
                {
                    if(mGrid) {
                        GridView grid = (GridView) mList;
                        int col = grid.getNumColumns();
                        int newCol = (int)Math.round(((fragmentWidth / logicalDensity) / (float) 150)/artSize);
                        if(col != newCol) {
                            int pos = grid.getFirstVisiblePosition();
                            //if an item is selected, focus on this instead
                            if(mActivatedPosition!=ListView.INVALID_POSITION) pos = mActivatedPosition;
                            grid.setNumColumns(newCol);
                            grid.setSelection(pos);
                        }
                    }
                }
            }
        });
    }

    public void filter(String s) {
        if(mList != null && mList.getAdapter() != null) {
            //we need a different empty msg for empty filter results
            mEmptyHeading.setText("No matches");
            mEmptyText.setText("clear or edit your query");
            ((ReleaseAdapter) mList.getAdapter()).getFilter().filter(s);
        }
    }


    private void checkOnlineCollection() {
        if(mDiscogs==null) mDiscogs = Discogs.getInstance(getActivity());
        if (mDiscogs.isLoggedIn()) {
            mCallbacks.setRefreshVisible(true);
            mDiscogs.isCollectionChanged(new Discogs.DiscogsWaiter() {
                @Override
                public void onResult(boolean success) {
                    if (success) {
                        //local and online collection are out of sync, fix it
                        mDiscogs.refreshCollection(new Discogs.DiscogsWaiter() {
                            @Override
                            public void onResult(boolean success) {
                                if (success) {
                                    loadList();
                                }
                                mCallbacks.setRefreshVisible(false);
                            }
                        });
                    } else {
                        mCallbacks.setRefreshVisible(false);
                    }
                }
            });

        } else {
            //user is not logged in, present dialog
            mDiscogs.logIn();
        }
    }

    public void refreshCollection() {
        mCallbacks.setRefreshVisible(true);
        mDiscogs.refreshCollection(new Discogs.DiscogsWaiter() {
            @Override
            public void onResult(boolean success) {
                if (success) {
                    loadList();
                }
                mCallbacks.setRefreshVisible(false);
            }
        });
    }

    public void loadList() {
        List<Release> releases = mDiscogs.getCollection();
        if (mList.getAdapter() == null) {
            mList.setAdapter(new ReleaseAdapter(getActivity(), releases));
            //only set fancy modal selection support on devices supporting it
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setSelection();
            }
        } else {
            ((ReleaseAdapter) mList.getAdapter()).updateReleases(releases);
        }
        mCallbacks.onAdapterSet();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setSelection() {
        mList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setSelector(R.drawable.track_selector);
        mList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.list_cab, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                SparseBooleanArray checkedItems = mList.getCheckedItemPositions();
                final List<Release> releases = new ArrayList<Release>();
                if (checkedItems != null) {
                    for (int i=0; i<checkedItems.size(); i++) {
                        if (checkedItems.valueAt(i)) {
                            releases.add((Release)mList.getAdapter().getItem(checkedItems.keyAt(i)));
                        }
                    }
                }
                switch (item.getItemId()) {
                    case R.id.reload_release:
                        mCallbacks.setRefreshVisible(true);
                        mDiscogs.refreshReleases(releases, new Discogs.DiscogsWaiter() {
                            @Override
                            public void onResult(boolean success) {
                                if(success) {
                                    Toast.makeText(getActivity(), "reloaded releases",Toast.LENGTH_SHORT).show();
                                    loadList();
                                } else {
                                    Toast.makeText(getActivity(), "Failed to reload requested releases!",
                                            Toast.LENGTH_SHORT).show();
                                }
                                mCallbacks.setRefreshVisible(false);
                            }
                        });
                        mode.finish();
                        break;
                    case R.id.delete_release:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        // Add the buttons
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                                mCallbacks.setRefreshVisible(true);
                                mDiscogs.removeReleases(releases, new Discogs.DiscogsWaiter() {
                                    @Override
                                    public void onResult(boolean success) {
                                        if(success) {
                                            Toast.makeText(getActivity(), "Removed selected releases",Toast.LENGTH_SHORT).show();
                                            loadList();
                                        } else {
                                            Toast.makeText(getActivity(), "Failed to remove selected releases!",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        mCallbacks.setRefreshVisible(false);
                                    }
                                });
                                mode.finish();
                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mode.finish();
                            }
                        });
                        builder.setTitle(R.string.delete_releases_title).setMessage(R.string.delete_releases_message);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    case R.id.cancel_selection:
                        mode.finish();
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                mode.setTitle(mList.getCheckedItemCount() + " releases selected");

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

    public void onListItemClick(View view, int position, long id) {
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mActivatedPosition = position;
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

    private void setActivatedPosition(int position) {
        if (position == AbsListView.INVALID_POSITION) {
            mList.setItemChecked(mActivatedPosition, false);
        } else {
            mList.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
