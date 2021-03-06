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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.adapters.TrackListAdapter;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;
import com.github.jvanhie.discogsscrobbler.util.NowPlayingService;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a single Release detail screen.
 * This fragment is either contained in a {@link com.github.jvanhie.discogsscrobbler.ReleaseListActivity}
 * in two-pane mode (on tablets) or a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailActivity}
 * on handsets.
 */
public class ReleaseTracklistFragment extends ListFragment {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String HAS_MENU = "has_menu";

    private Release mRelease;

    private Discogs mDiscogs;

    private List<Track> mTracklist;

    public boolean hasMenu = false;

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
        hasMenu = getArguments().getBoolean(HAS_MENU,false);

        if(hasMenu) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRelease != null) {
            if (!mRelease.hasExtendedInfo) {
                //we don't got extended info on this release yet, get it and display it
                mDiscogs.refreshRelease(mRelease, new Discogs.DiscogsWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            mTracklist = mRelease.tracklist();
                            setListAdapter(new TrackListAdapter(getActivity(),mTracklist));
                        }
                    }
                });
            } else {
                mTracklist = mRelease.tracklist();
                setListAdapter(new TrackListAdapter(getActivity(),mTracklist));
            }
        }  else {
            //we don't have this release in the local db, fetch it from the web
            mDiscogs.getRelease(getArguments().getLong(ARG_ITEM_ID,0), new Discogs.DiscogsDataWaiter<Release>() {
                @Override
                public void onResult(boolean success, Release data) {
                    if(success) {
                        mRelease = data;
                        mTracklist = mRelease.tracklist();
                        setListAdapter(new TrackListAdapter(getActivity(),mTracklist));
                    }
                }
            });
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

            if (mRelease == null || mRelease.isTransient) {
                //the release is not in the collection, give the user the opportunity to add it
                inflater.inflate(R.menu.release_detail_search, menu);
            }
            if (mRelease != null && !mRelease.isTransient) {
                //release is in the collection
                inflater.inflate(R.menu.release_detail_refresh, menu);
            }

            inflater.inflate(R.menu.release_detail_scrobble, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //only handle options when visible and instantiated with menu power
        if(hasMenu && isVisible()) {
            int id = item.getItemId();
            if (id == R.id.detail_add_to_discogs) {
                addToDiscogs();
            }
            if (id == R.id.detail_reload_release) {
                mDiscogs.refreshRelease(mRelease, new Discogs.DiscogsWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        if(success) {
                            Toast.makeText(getActivity(), "reloaded release",Toast.LENGTH_SHORT).show();
                            mTracklist = mRelease.tracklist();
                            setListAdapter(new TrackListAdapter(getActivity(),mTracklist));
                        } else {
                            Toast.makeText(getActivity(), "Failed to reload requested release",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            if (id == R.id.detail_scrobble_release) {
                scrobble();
            }
        }
        return super.onOptionsItemSelected(item);
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

    public List<Track> getSelectedTracks() {
        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
        List<Track> tracks = new ArrayList<Track>();
        if (checkedItems != null) {
            for (int i=0; i<checkedItems.size(); i++) {
                if (checkedItems.valueAt(i)) {
                    tracks.add(mTracklist.get(checkedItems.keyAt(i)));
                }
            }
        }
        //if no tracks are selected, return all
        if(tracks.size()==0) tracks = mTracklist;
        return tracks;
    }

    public void clearSelection() {
        for(int i = 0; i < mTracklist.size(); i ++) {
            getListView().setItemChecked(i, false);
        }
    }

    public void addToDiscogs() {
        if(mRelease==null) return;
        mDiscogs.addRelease(mRelease.releaseid, new Discogs.DiscogsWaiter() {
            @Override
            public void onResult(boolean success) {
                if (success) {
                    Toast.makeText(getActivity(), "Added release to Discogs collection", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Release already in Discogs collection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void scrobble() {
        //first a small sanity check
        if(mRelease==null) return;
        final Lastfm lastfm = Lastfm.getInstance(getActivity());
        if (lastfm.isLoggedIn()) {

            //get selected tracks
            final List<Track> tracks = getSelectedTracks();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Did you just finished listening to " + mRelease.title + " or are you about to listen to it?").setTitle("Scrobble this album?");
            builder.setPositiveButton("About to", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //start now playing service
                    Intent i=new Intent(getActivity(), NowPlayingService.class);
                    i.putParcelableArrayListExtra(NowPlayingService.TRACK_LIST, new ArrayList<Track>(tracks));
                    i.putExtra(NowPlayingService.THUMB_URL,mRelease.thumb);
                    i.putExtra(NowPlayingService.RELEASE_ID,mRelease.releaseid);
                    if(mRelease.images().size()>0)
                        i.putExtra(NowPlayingService.ALBUM_ART_URL,mRelease.images().get(0).uri);
                    getActivity().startService(i);
                    //save to recentlyplayed
                    mDiscogs.setRecentlyPlayed(mRelease);
                    //go to the now playing activity
                    startActivity(new Intent(getActivity(), NowPlayingActivity.class));
                }
            });
            builder.setNeutralButton("Just finished", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //scrobble the tracks now
                    lastfm.scrobbleTracks(tracks, new Lastfm.LastfmWaiter() {
                        @Override
                        public void onResult(boolean success) {
                            if(getActivity()!=null) {
                                Toast.makeText(getActivity(), "Scrobbled " + tracks.size() + " tracks", Toast.LENGTH_SHORT).show();
                                clearSelection();
                            }
                        }
                    });
                    //save to recentlyplayed
                    mDiscogs.setRecentlyPlayed(mRelease);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog don't do anything
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            //log in first
            lastfm.logIn(new Lastfm.LastfmWaiter() {
                @Override
                public void onResult(boolean success) {
                    if(getActivity()!=null) {
                        if(success) {
                            Toast.makeText(getActivity(), "Logged in to last.fm", Toast.LENGTH_SHORT).show();
                            scrobble();
                        } else {
                            Toast.makeText(getActivity(), "Could not log in to last.fm", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }
}
