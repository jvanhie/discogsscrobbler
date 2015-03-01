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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.adapters.TrackListAdapter;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.NowPlayingService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a single Release detail screen.
 * This fragment is either contained in a {@link com.github.jvanhie.discogsscrobbler.ReleaseListActivity}
 * in two-pane mode (on tablets) or a {@link com.github.jvanhie.discogsscrobbler.ReleaseDetailActivity}
 * on handsets.
 */
public class NowPlayingFragment extends Fragment {

    private Discogs mDiscogs;
    private ImageLoader mImageLoader;

    private NowPlayingService mService;
    private boolean mBound;
    private TrackChangeReceiver mTrackChangeReceiver;

    private FrameLayout mSuperFrame;
    private View mRootView;
    private View mEmptyView;
    private boolean isEmpty=true;
    private TrackListAdapter mTrackListAdapter;

    /*menuitems to enable or disable depending on now playing state*/
    private MenuItem mPlayMenu;
    private MenuItem mPauseMenu;
    private MenuItem mStopMenu;

    private Callbacks mCallbacks = sDummyCallbacks;
    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onReleaseSet(long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onReleaseSet(long id) {
        }

    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NowPlayingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mDiscogs == null) mDiscogs = Discogs.getInstance(getActivity());
        if(mImageLoader == null) {
            //create universal image loader
            mImageLoader = ImageLoader.getInstance();
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showStubImage(R.drawable.default_release)
                    .cacheInMemory()
                    .displayer(new FadeInBitmapDisplayer(500))
                    .build();
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
                    .enableLogging()
                    .defaultDisplayImageOptions(options)
                    .build();
            mImageLoader.init(config);
        }

        /*bind to the now playing service*/
        Intent intent = new Intent(getActivity(), NowPlayingService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        setHasOptionsMenu(true);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mPlayMenu = menu.findItem(R.id.now_playing_play);
        mPauseMenu = menu.findItem(R.id.now_playing_pause);
        mStopMenu = menu.findItem(R.id.now_playing_stop);
        setMenuVisibility();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        //since this fragment will be the only menu producing object in this activity, clear leftovers from strange android screen rotation magic
        //TODO: move menu items to the activity, would be cleaner I suppose
        menu.clear();
        inflater.inflate(R.menu.now_playing, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mBound) {
            switch (item.getItemId()) {
                case R.id.now_playing_play:
                    //start playing again
                    mService.resume();
                    break;
                case R.id.now_playing_pause:
                    //pause playing
                    mService.pause();
                    break;
                case R.id.now_playing_stop:
                    //clear playlist
                    mService.stop();
                    setNowPlaying();
                    break;
            }
        }
        setMenuVisibility();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        mEmptyView = inflater.inflate(R.layout.fragment_empty, container, false);
        ((TextView)mEmptyView.findViewById(R.id.empty_heading)).setText("Nothing is playing");
        ((TextView)mEmptyView.findViewById(R.id.empty_text)).setText("Start playing something by going to your Collection or search something to play on Discogs");

        //create superframe for switching between recently playing and empty view
        mSuperFrame = new FrameLayout(getActivity());
        FrameLayout.LayoutParams layoutparams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        mSuperFrame.setLayoutParams(layoutparams);
        mSuperFrame.addView(mEmptyView);

        if(mBound) {
            setNowPlaying();
        }

        return mSuperFrame;
    }

    private void setEmptyView(boolean empty) {
        //only change views when state changed and superframe is initialized
        if(isEmpty != empty && mSuperFrame != null) {
            if(empty) {
                mSuperFrame.removeView(mRootView);
                mSuperFrame.addView(mEmptyView);
            } else {
                mSuperFrame.removeView(mEmptyView);
                mSuperFrame.addView(mRootView);
            }
            isEmpty=empty;
            //mSuperFrame.invalidate();
        }
    }

    private void setNowPlaying() {
        if(mService == null || mService.trackList == null || mService.trackList.size()==0 || mRootView == null) {
            setEmptyView(true);
            return;
        } else {
            setEmptyView(false);
        }

        /*set general album details*/
        ((TextView) mRootView.findViewById(R.id.now_playing_artist)).setText(mService.artist);
        ((TextView) mRootView.findViewById(R.id.now_playing_album)).setText(mService.album);
        ImageView albumArt = (ImageView) mRootView.findViewById(R.id.now_playing_image);
        mImageLoader.displayImage(mService.albumArtURL,albumArt);

        /*initiate list with tracklist*/
        if(mService.trackList != null && mTrackListAdapter == null) {
            mTrackListAdapter = new TrackListAdapter(getActivity(),mService.trackList);
            ListView list = ((ListView) mRootView.findViewById(R.id.now_playing_tracklist));
            list.setAdapter(mTrackListAdapter);
            //only set fancy modal selection support on devices supporting it
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setSelection(list);
            }
        }

        //set the currently playing track and status
        if(mTrackListAdapter != null) {
            if(mTrackListAdapter.getCount() != mService.trackList.size()) {
                mTrackListAdapter = new TrackListAdapter(getActivity(),mService.trackList);
                ((ListView) mRootView.findViewById(R.id.now_playing_tracklist)).setAdapter(mTrackListAdapter);
            }
            mTrackListAdapter.setNowPlaying(mService.currentTrack);
            mTrackListAdapter.notifyDataSetChanged();
        }

        mCallbacks.onReleaseSet(mService.releaseId);
        setMenuVisibility();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setSelection(final ListView list) {
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setSelector(R.drawable.track_selector);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mBound) {
                    //fully stop previous playlist since we start again from the beginning of the selected track
                    ArrayList<Track> trackList = new ArrayList<Track>(mService.trackList);
                    mService.stop();
                    mService.trackList= trackList;
                    mService.currentTrack = i;
                    mService.resume();
                }
            }
        });
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.now_playing_cab, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                SparseBooleanArray checkedItems = list.getCheckedItemPositions();
                final List<Integer> positions = new ArrayList<Integer>();
                if (checkedItems != null) {
                    for (int i=0; i<checkedItems.size(); i++) {
                        if (checkedItems.valueAt(i)) {
                            positions.add(checkedItems.keyAt(i));
                        }
                    }
                }
                switch(menuItem.getItemId()) {
                    case R.id.play_selection:
                        if(mBound) {
                            //change tracklist and start over again
                            ArrayList<Track> newTrackList = new ArrayList<Track>();
                            for(Integer i : positions) {
                                newTrackList.add(mService.trackList.get(i));
                            }
                            //fully stop previous playlist since we start again from the beginning
                            mService.stop();
                            mService.trackList=newTrackList;
                            mService.currentTrack=0;
                            mService.resume();
                        }
                        actionMode.finish();
                        break;
                    case R.id.remove_selection:
                        int currPlaying = mService.currentTrack;
                        int newPlaying = -1;
                        //change tracklist and start over again
                        ArrayList<Track> newTrackList = new ArrayList<Track>();
                        for(int i = 0 ; i < mService.trackList.size(); i++) {
                            if(!positions.contains(i)) {
                                newTrackList.add(mService.trackList.get(i));
                            }
                            //calculate new currently playing counter
                            if(i == currPlaying) {
                                if(positions.contains(i)) {
                                    currPlaying++;
                                } else {
                                    newPlaying = newTrackList.size()-1;
                                }
                            }
                        }
                        //the currently playing track was removed, start fresh from the next available
                        if(currPlaying != mService.currentTrack) {
                            mService.stop();
                            if(newPlaying!=-1) {
                                mService.trackList=newTrackList;
                                mService.currentTrack=newPlaying;
                                mService.resume();
                            }
                        } else {
                            //pause the playlist and resume with the new tracklist
                            mService.pause();
                            mService.trackList=newTrackList;
                            mService.currentTrack=newPlaying;
                            mService.resume();
                        }
                        actionMode.finish();
                        break;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });
    }

    private void setMenuVisibility() {
        //only execute if the menu items have been initialized
        if (mPlayMenu!=null) {
            //by default, all menu items are hidden
            mPlayMenu.setVisible(false);
            mPauseMenu.setVisible(false);
            mStopMenu.setVisible(false);
            if (mBound && mTrackListAdapter != null) {
                //best place to set the now playing status of the tracklist
                mTrackListAdapter.setIsNowPlaying(mService.isPlaying);
                mTrackListAdapter.notifyDataSetChanged();

                //if there's a playlist -> allow to clear it
                if (mService.trackList != null && mService.trackList.size() > 0) {
                    mStopMenu.setVisible(true);
                }

                //is there something playing? allow to pause, else, allow to play
                if (mService.isPlaying) {
                    mPauseMenu.setVisible(true);
                } else if (mService.trackList != null && mService.trackList.size() > 0) {
                    //paused
                    mPlayMenu.setVisible(true);
                }
            }
        }

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NowPlayingService.LocalBinder binder = (NowPlayingService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            setNowPlaying();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /*for receiving updates from the now playing service*/
    private class TrackChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NowPlayingService.TRACK_CHANGE)) {
                setNowPlaying();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrackChangeReceiver == null) mTrackChangeReceiver = new TrackChangeReceiver();
        IntentFilter intentFilter = new IntentFilter(NowPlayingService.TRACK_CHANGE);
        getActivity().registerReceiver(mTrackChangeReceiver, intentFilter);
        if(mService != null) setNowPlaying();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTrackChangeReceiver != null) getActivity().unregisterReceiver(mTrackChangeReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
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
}
