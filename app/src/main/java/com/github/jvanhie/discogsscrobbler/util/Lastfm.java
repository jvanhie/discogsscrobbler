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

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.DiscogsLoginActivity;
import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.models.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

/**
 * Created by Jono on 03/04/2014.
 */
public class Lastfm extends ContextWrapper {

    private Caller mLastfm;
    private Context mContext;

    private final String API_KEY;
    private final String API_SECRET;
    private String mUserName;
    private String mAccessKey;
    private Session mSession;

    private SharedPreferences mPrefs;

    private static Lastfm instance;

    public static synchronized Lastfm getInstance(Context context) {
        if (instance == null) {
            instance = new Lastfm(context);
        }
        else {
            //last caller gets to set the mContext object
            instance.mContext = context;
        }
        return instance;
    }

    public Lastfm(Context context) {
        super(context);
        mContext = context;
        Resources res = context.getResources();

        API_KEY = res.getString(R.string.lastfm_api_key);
        API_SECRET = res.getString(R.string.lastfm_api_secret);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAccessKey = mPrefs.getString("lastfm_access_key", null);
        mUserName = mPrefs.getString("lastfm_user_name", null);

        //if username and key not null -> recreate the lastfm session
        if(mAccessKey != null) {
            mSession = Session.createSession(API_KEY, API_SECRET, mAccessKey);
        }

        String ua = res.getString(R.string.user_agent);

        mLastfm = Caller.getInstance();
        mLastfm.setUserAgent(ua);
        mLastfm.setCache(null);
        mLastfm.setDebugMode(true);
    }

    public boolean isLoggedIn() {
        return (mSession != null);
    }

    public void updateNowPlaying(final Track track, final LastfmWaiter waiter) {
        if(mSession==null) {
            waiter.onResult(false);
            return;
        }

        AsyncTask<Void,Void,Boolean> t = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                String title = track.title;
                String album = track.album;
                String artist = track.artist;
                int duration = Discogs.formatDurationToSeconds(track.duration);
                ScrobbleData data = new ScrobbleData();
                data.setArtist(artist);
                data.setTrack(title);
                data.setAlbum(album);
                data.setDuration(duration);
                ScrobbleResult result = de.umass.lastfm.Track.updateNowPlaying(data, mSession);
                boolean success = (result.isSuccessful() && !result.isIgnored());
                return success;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                waiter.onResult(result);
            }

        };
        t.execute();
    }

    public void scrobbleTrack(final Track track, final int time, final LastfmWaiter waiter) {
        if(mSession==null) {
            waiter.onResult(false);
            return;
        }

        AsyncTask<Void,Void,Boolean> t = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                String title = track.title;
                String album = track.album;
                String artist = track.artist;
                ScrobbleData data = new ScrobbleData();
                data.setArtist(artist);
                data.setTrack(title);
                data.setAlbum(album);
                data.setTimestamp(time);
                ScrobbleResult result = de.umass.lastfm.Track.scrobble(data, mSession);
                boolean success = (result.isSuccessful() && !result.isIgnored());
                System.out.println("scrobbled: "+ artist +":"+title + " -> "+ success);
                return success;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                waiter.onResult(result);
            }

        };
        t.execute();


    }

    public void scrobbleTracks(List<Track> trackList, final LastfmWaiter waiter) {
        //only keep real tracks
        trackList = filterTracks(trackList);

        final AtomicBoolean allSuccess = new AtomicBoolean(true);
        final AtomicInteger ctr = new AtomicInteger(0);
        final int total = trackList.size();

        int time = (int) (System.currentTimeMillis() / 1000);
        int[] times = new int[trackList.size()];
        for (int i = trackList.size()-1; i >= 0; --i) {
            int length = Discogs.formatDurationToSeconds(trackList.get(i).duration);
            time -= length;
            times[i] = time;
        }
        for (int i = 0 ; i < trackList.size() ; i++) {
            scrobbleTrack(trackList.get(i),times[i], new LastfmWaiter() {
                @Override
                public void onResult(boolean success) {
                    if (!success) allSuccess.set(false);
                    int updates = ctr.incrementAndGet();
                    if (updates == total) {
                        //all updates done, return total result and remove releases from the db
                        waiter.onResult(allSuccess.get());
                    }
                }
            });
        }
    }

    //this method only retains tracks that should be scrobbled
    public List<Track> filterTracks(List<Track> trackList) {
        List<Track> filtered = new ArrayList<Track>();
        for (Track t : trackList) {
            if(!t.type.equals("heading") && t.title != null && !t.title.equals("")) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    public void getSession(final String username, final String password) {
        AsyncTask t = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    mSession = Authenticator.getMobileSession(username, password, API_KEY, API_SECRET);
                    if (mSession != null) {
                        mAccessKey = mSession.getKey();
                        mUserName = mSession.getUsername();
                        saveSession();

                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        t.execute();
    }

    public void logIn() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        // Get the layout inflater
        LayoutInflater inflater = LayoutInflater.from(mContext);
        // Pass null as the parent view because its going in the dialog layout
        final View dialogcontent = inflater.inflate(R.layout.lastfm_login_dialog, null);
        builder.setView(dialogcontent)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String username = ((EditText)dialogcontent.findViewById(R.id.lastfm_username)).getText().toString();
                        String password = ((EditText)dialogcontent.findViewById(R.id.lastfm_password)).getText().toString();
                        getSession(username,password);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //cancelled
                    }
                });
        builder.create().show();
    }

    public void logOut() {
        mAccessKey = null;
        mUserName = null;
        saveSession();
    }

    private void saveSession() {
        SharedPreferences.Editor prefEdit = mPrefs.edit();

        if (mAccessKey != null) {
            prefEdit.putString("lastfm_access_key", mAccessKey);
        } else {
            prefEdit.remove("lastfm_access_key");
        }

        if (mUserName != null) {
            prefEdit.putString("lastfm_user_name", mUserName);
        } else {
            prefEdit.remove("lastfm_user_name");
        }

        prefEdit.commit();
    }

    public interface LastfmWaiter {
        public void onResult(boolean success);
    }

}
