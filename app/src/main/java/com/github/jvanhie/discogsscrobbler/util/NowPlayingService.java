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

package com.github.jvanhie.discogsscrobbler.util;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.ReleaseListActivity;
import com.github.jvanhie.discogsscrobbler.models.Track;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import java.util.List;

/**
 * Created by Jono on 18/04/2014.
 */
public class NowPlayingService extends Service {

    private static int NOTIFICATION_ID = 666;
    public static String TRACK_LIST = "tracklist";
    public static String ALBUM_ART_URL = "albumart";

    private boolean mIsPlaying = false;
    private List<Track> mTrackList;
    private int mCurrentTrack;
    private Bitmap mAlbumArt;

    private ImageLoader mImageLoader;
    private Discogs mDiscogs;
    private Lastfm mLastfm;
    private NotificationCompat.Builder mNotificationBuilder;
    private Handler mNowPlayingHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mDiscogs = Discogs.getInstance(this);
        mLastfm = Lastfm.getInstance(this);

        //create universal image loader
        mImageLoader = ImageLoader.getInstance();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.default_release)
                .cacheInMemory()
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .enableLogging()
                .defaultDisplayImageOptions(options)
                .imageDownloader(new DiscogsImageDownloader(mDiscogs))
                .build();
        mImageLoader.init(config);

        mNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ReleaseListActivity.class), 0));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mTrackList = intent.getParcelableArrayListExtra("tracklist");
        String albumArt = intent.getStringExtra("albumart");
        mCurrentTrack = 0;
        /*first try to load the album art, then start playing*/
        mImageLoader.loadImage(this,albumArt,new ImageLoadingListener() {
            @Override
            public void onLoadingStarted() {}
            @Override
            public void onLoadingCancelled() {}

            @Override
            public void onLoadingFailed(FailReason failReason) {
                mNotificationBuilder.setLargeIcon(null);
                play(mCurrentTrack);
            }

            @Override
            public void onLoadingComplete(Bitmap loadedImage) {
                if(mAlbumArt != null) mNotificationBuilder.setLargeIcon(loadedImage);
                play(mCurrentTrack);
            }
        });

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void play(final int trackNumber) {
        mIsPlaying = true;
        Track t = mTrackList.get(trackNumber);
        mCurrentTrack = trackNumber;
        mNotificationBuilder.setContentTitle(t.title).setContentText(t.album + "\n" + t.artist).setWhen(System.currentTimeMillis());
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        //start handler to start the next song
        Runnable nextSong = new Runnable() {
            public void run() {
                //ok, the song should be done by now, next or stop when finished!
                if(trackNumber < mTrackList.size()-1) {
                    System.out.println("track " + (trackNumber+1) + " " +(System.currentTimeMillis()/1000));
                    play(trackNumber+1);
                } else {
                    //finished the tracklist, stop foreground process
                    mIsPlaying = false;
                    stopForeground(true);
                }
            }
        };
        mNowPlayingHandler.postDelayed(nextSong,mDiscogs.formatDurationToSeconds(t.duration) * 1000);

    }

    private void stop() {
        if (mIsPlaying) {
            //remove next song callback
            mNowPlayingHandler.removeCallbacks(null);
            mIsPlaying=false;
            stopForeground(true);
        }

    }
}
