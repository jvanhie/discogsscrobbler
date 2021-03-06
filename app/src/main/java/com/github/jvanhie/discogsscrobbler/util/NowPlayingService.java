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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.github.jvanhie.discogsscrobbler.NowPlayingActivity;
import com.github.jvanhie.discogsscrobbler.R;
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
    public static String TRACK_CHANGE = "track_change" ;
    public static String TRACK_LIST = "track_list";
    public static String ALBUM_ART_URL = "album_art";
    public static String THUMB_URL = "thumb";
    public static String RELEASE_ID = "release_id";
    public static String NEXT_TRACK_MODE = "next_track";
    public static String NEXT_TRACK_ID = "next_track_id";
    public static String NEXT_TRACK_TITLE = "next_track_title";

    /*public variables that can be queried by a bound activity*/
    public boolean isPlaying = false;
    public List<Track> trackList;
    public int currentTrack;
    public String albumArtURL;
    public String thumb;
    public String artist;
    public String album;
    public long releaseId;
    public Track track;

    //variables to maintain paused position
    private long mTrackStart;
    private int mTrackDone = 0;
    
    private ImageLoader mImageLoader;
    private Discogs mDiscogs;
    private Lastfm mLastfm;
    private NotificationCompat.Builder mNotificationBuilder;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NowPlayingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NowPlayingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

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
                .build();
        mImageLoader.init(config);

        mNotificationBuilder = new NotificationCompat.Builder(this).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, NowPlayingActivity.class), 0));

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getBooleanExtra(NEXT_TRACK_MODE,false)) {
            /*request to start scrobbling the next track, check if it is sound*/
            int pos = intent.getIntExtra(NEXT_TRACK_ID,0);
            String title = intent.getStringExtra(NEXT_TRACK_TITLE);
            if(trackList != null && trackList.size()>0) {
                //done listening to the previous track -> scrobble the previous track
                int now = (int)(System.currentTimeMillis()/1000);
                mLastfm.scrobbleTrack(track,now,new Lastfm.LastfmWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        //the user already sees the notification, no need for extras notifications atm.
                    }
                });
                mTrackDone = 0;

                if (pos == -1 && trackList.get(0).title.equals(title)) {
                    //stop requested
                    stop();
                } else if (pos < trackList.size() && trackList.get(pos).title.equals(title)) {
                    //ok, this is a valid request, make it happen
                    play(pos);
                }
            }
            /*release the wakelock if it was called via the now playing alarm*/
            NowPlayingAlarm.completeWakefulIntent(intent);
        } else {
            /*we have received a playlist, start playing*/
            trackList = intent.getParcelableArrayListExtra(TRACK_LIST);
            thumb = intent.getStringExtra(THUMB_URL);
            albumArtURL = intent.getStringExtra(ALBUM_ART_URL);
            releaseId = intent.getLongExtra(RELEASE_ID,0);
            currentTrack = 0;
            /*first try to load the album art, then start playing*/
            mImageLoader.loadImage(this, thumb, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted() {
                }

                @Override
                public void onLoadingCancelled() {
                }

                @Override
                public void onLoadingFailed(FailReason failReason) {
                    mNotificationBuilder.setLargeIcon(null);
                    play(currentTrack);
                }

                @Override
                public void onLoadingComplete(Bitmap loadedImage) {
                    mNotificationBuilder.setLargeIcon(loadedImage);
                    play(currentTrack);
                }
            });
        }

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        stop();
    }



    private void play(final int trackNumber) {
        //do we even have something to play?
        if(trackList==null || trackList.size()==0) return;

        track = trackList.get(trackNumber);

        //don't play headings, on to the next! (if there is a next)
        if(track.type.equals("heading")) {
            if(trackNumber+1 < trackList.size()) {
                play(trackNumber+1);
            } else {
                stop();
            }
            return;
        }

        isPlaying = true;
        artist = track.artist;
        album = track.album;
        currentTrack = trackNumber;
        mNotificationBuilder.setContentTitle(track.title).setContentText(track.artist + " - " + track.album).setWhen(System.currentTimeMillis()).setSmallIcon(android.R.drawable.ic_media_play);
        //fetch the duration (will return immediately when available) and start the alarm for when it's done
        mLastfm.getDuration(track, new Lastfm.LastfmWaiter() {
            @Override
            public void onResult(boolean success) {
                //set nowplaying, scrobbling happens on the alarm callback
                mLastfm.updateNowPlaying(track,new Lastfm.LastfmWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        //the user already sees the notification, no need for extras notifications atm.
                    }
                });

                //notify user
                startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
                //update listeners to track change
                sendBroadcast(new Intent(TRACK_CHANGE));

                Intent intent = new Intent(NowPlayingService.this, NowPlayingAlarm.class);
                if(trackNumber < trackList.size()-1) {
                    intent.putExtra(NEXT_TRACK_ID, (trackNumber + 1));
                    intent.putExtra(NEXT_TRACK_TITLE, trackList.get(trackNumber + 1).title);
                } else {
                    //this is the last track, alarm will be used to stop the service (by issuing pos = -1 and title = first song title)
                    intent.putExtra(NEXT_TRACK_ID, -1);
                    intent.putExtra(NEXT_TRACK_TITLE, trackList.get(0).title);
                }
                mTrackStart = SystemClock.elapsedRealtime();
                int duration = Track.formatDurationToSeconds(track.duration);
                if(duration == 0 ) duration = Lastfm.DEFAULT_TRACK_DURATION;
                if(mTrackDone != 0) duration -= mTrackDone;
                mAlarmIntent = PendingIntent.getBroadcast(NowPlayingService.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, mTrackStart + duration * 1000, mAlarmIntent);

            }
        });

    }

    public void resume() {
        if(!isPlaying) {
            play(currentTrack);
        }
    }

    public void pause() {
        if(isPlaying) {
            //clear pending next track alarm
            if (mAlarmIntent != null) {
                mAlarmManager.cancel(mAlarmIntent);
                mAlarmIntent.cancel();
            }
            isPlaying=false;
            //calculate how much of the track we have already played in ms and add it to the done variable in s
            long diff = SystemClock.elapsedRealtime()-mTrackStart;
            mTrackDone += diff/1000;
            //notify last.fm as well
            mLastfm.stopNowPlaying(track);
            //change notifaction
            mNotificationBuilder.setWhen(System.currentTimeMillis()).setSmallIcon(android.R.drawable.ic_media_pause);
            startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    public void stop() {

            //clear outstanding callbacks
            if(mAlarmIntent!=null) {
                mAlarmManager.cancel(mAlarmIntent);
                mAlarmIntent.cancel();
            }

            //notify last.fm as well
            mLastfm.stopNowPlaying(track);

            if(trackList!=null) trackList.clear();
            isPlaying = false;

            //update listeners to track change
            sendBroadcast(new Intent(TRACK_CHANGE));

            stopForeground(true);

    }

}
