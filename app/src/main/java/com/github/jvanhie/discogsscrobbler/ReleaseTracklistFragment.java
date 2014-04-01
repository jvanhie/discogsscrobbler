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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.util.List;

/**
 * A fragment representing a single Release detail screen.
 * This fragment is either contained in a {@link com.github.jvanhie.discogsscrobbler.ReleaseListActivity}
 * in two-pane mode (on tablets) or a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailActivity}
 * on handsets.
 */
public class ReleaseTracklistFragment extends ListFragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private Release mRelease;

    private Discogs mDiscogs;

    private List<Track> mTracklist;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReleaseTracklistFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mDiscogs == null) mDiscogs = Discogs.getInstance(getActivity());
        //get id from arguments if not from constructor
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mRelease=mDiscogs.getRelease(getArguments().getLong(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRelease != null) {
            if (!mRelease.hasExtendedInfo) {
                //we don't got extended info on this release yet, get it and display it
                mDiscogs.refreshRelease(mRelease, new Discogs.DiscogsWaiter() {
                    @Override
                    public void onResult(Boolean success) {
                        if (success) {
                            mTracklist = mRelease.tracklist();
                            setListAdapter(new TrackListAdapter());
                        }
                    }
                });
            } else {
                mTracklist = mRelease.tracklist();
                setListAdapter(new TrackListAdapter());
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().setSelector(R.drawable.track_selector);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if(mTracklist.get(position).type.equals("heading")) {
            boolean selected = getListView().isItemChecked(position);
            //iterate over all the next items and give them the same selection until the next header
            for(int i = position+1; i < getListAdapter().getCount() ; i++) {
                if(!mTracklist.get(i).type.equals("heading")) {
                    getListView().setItemChecked(i,selected);
                } else {
                    break;
                }
            }
        }
    }

    private class TrackListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mTracklist.size();
        }

        @Override
        public Object getItem(int i) {
            return mTracklist.get(i);
        }

        @Override
        public long getItemId(int i) {
            return mTracklist.get(i).getId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.discogs_track, viewGroup, false);
            }
            TextView pos = (TextView) view.findViewById(R.id.track_pos);
            TextView duration = (TextView) view.findViewById(R.id.track_duration);
            TextView name = (TextView) view.findViewById(R.id.track_name);

            Track track = mTracklist.get(i);
            pos.setText(track.position);
            duration.setText(track.duration);
            name.setText(track.title);

            if(track.type.equals("heading")) {
                name.setTextColor(Color.LTGRAY);
            } else {
                name.setTextColor(Color.BLACK);
            }

            return view;
        }
    }
}
