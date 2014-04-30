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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.github.jvanhie.discogsscrobbler.adapters.TrackListAdapter;
import com.github.jvanhie.discogsscrobbler.models.Image;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsPriceSuggestion;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;
import com.github.jvanhie.discogsscrobbler.util.NowPlayingService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;

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

    private View mRootView;
    private TrackListAdapter mTrackListAdapter;

    /*menuitems to enable or disable depending on now playing state*/
    private MenuItem mPlayMenu;
    private MenuItem mPauseMenu;
    private MenuItem mStopMenu;

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
                    .cacheOnDisc()
                    .displayer(new FadeInBitmapDisplayer(500))
                    .build();
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
                    .enableLogging()
                    .defaultDisplayImageOptions(options)
                    .imageDownloader(new DiscogsImageDownloader(mDiscogs))
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

        if(mBound) {
            setNowPlaying();
        }

        return mRootView;
    }

    private void setNowPlaying() {
        /*set general album details*/
        ((TextView) mRootView.findViewById(R.id.now_playing_artist)).setText(mService.artist);
        ((TextView) mRootView.findViewById(R.id.now_playing_album)).setText(mService.album);
        ImageView albumArt = (ImageView) mRootView.findViewById(R.id.now_playing_image);
        mImageLoader.displayImage(mService.albumArtURL,albumArt);

        /*initiate list with tracklist*/
        if(mService.trackList != null && mTrackListAdapter == null) {
            mTrackListAdapter = new TrackListAdapter(getActivity(),mService.trackList);
            ((ListView) mRootView.findViewById(R.id.now_playing_tracklist)).setAdapter(mTrackListAdapter);
        }

        //set the currently playing track
        if(mTrackListAdapter != null) {
            mTrackListAdapter.setNowPlaying(mService.currentTrack);
            mTrackListAdapter.notifyDataSetChanged();
        }

        setMenuVisibility();
    }

    private void setMenuVisibility() {
        //only execute if the menu items have been initialized
        if (mPlayMenu!=null) {
            //by default, all menu items are hidden
            mPlayMenu.setVisible(false);
            mPauseMenu.setVisible(false);
            mStopMenu.setVisible(false);
            if (mBound) {

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
}
