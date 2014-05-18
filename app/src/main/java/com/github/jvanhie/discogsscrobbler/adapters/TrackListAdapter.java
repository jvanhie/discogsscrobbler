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

package com.github.jvanhie.discogsscrobbler.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.models.Track;

import java.util.List;

/**
 * Created by Jono on 28/04/2014.
 */
public class TrackListAdapter extends BaseAdapter {
    private List<Track> mTracklist;
    private Context mContext;
    private int mNowPlaying = -1;
    private boolean mIsNowPlaying = true;
    private boolean mIsMultiArtist = false;

    public TrackListAdapter(Context context, List<Track> trackList) {
        mContext = context;
        mTracklist = trackList;
        //see if it's a multi artist tracklist
        if(trackList.size()>1) {
            String artist = trackList.get(0).artist;
            for (Track t : trackList) {
                if (!t.artist.equals(artist)) {
                    mIsMultiArtist = true;
                    break;
                }
            }
        }
    }

    public void setNowPlaying(int nowPlaying) {
        mNowPlaying = nowPlaying;
    }

    public void setIsNowPlaying(boolean isNowPlaying) {
        mIsNowPlaying = isNowPlaying;
    }

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
        if(mTracklist.get(i).getId()==null) return mTracklist.get(i).idx;
        else return mTracklist.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            if(mContext == null) return null;
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.discogs_track, viewGroup, false);
        }
        TextView pos = (TextView) view.findViewById(R.id.track_pos);
        TextView duration = (TextView) view.findViewById(R.id.track_duration);
        TextView name = (TextView) view.findViewById(R.id.track_name);
        ImageView playing = (ImageView) view.findViewById(R.id.track_playing);

        Track track = mTracklist.get(i);
        pos.setText(track.position);
        duration.setText(track.duration);
        if(mIsMultiArtist) {
            name.setText(track.artist + " - " + track.title);
        } else {
            name.setText(track.title);
        }


        if(track.type.equals("heading")) {
            name.setTextColor(Color.LTGRAY);
        } else {
            pos.setTextColor(Color.BLACK);
            name.setTextColor(Color.BLACK);
            duration.setTextColor(Color.BLACK);
        }

        //songs that have already been played are gray
        if(i<mNowPlaying) {
            pos.setTextColor(Color.LTGRAY);
            name.setTextColor(Color.LTGRAY);
            duration.setTextColor(Color.LTGRAY);
        }

        if (i==mNowPlaying) {
            //we're the currently played song, indicate it in the tracklist
            if(mIsNowPlaying) {
                playing.setImageResource(R.drawable.now_playing_play);
            } else {
                playing.setImageResource(R.drawable.now_playing_pause);
            }
            playing.setVisibility(View.VISIBLE);

        } else {
            playing.setVisibility(View.GONE);
        }

        return view;
    }

}
