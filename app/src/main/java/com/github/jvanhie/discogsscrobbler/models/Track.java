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

package com.github.jvanhie.discogsscrobbler.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

/**
 * Created by Jono on 22/03/2014.
 */
@Table(name = "tracks")
public class Track extends Model implements Parcelable{

    @Column(name = "release", onDelete = Column.ForeignKeyAction.CASCADE)
    public Release release;

    @Column(name = "idx")
    public int idx;

    @Column(name = "position")
    public String position;

    @Column(name = "type")
    public String type;

    @Column(name = "artist")
    public String artist;

    @Column(name = "album")
    public String album;

    @Column(name = "title")
    public String title;

    @Column(name = "duration")
    public String duration;

    public Track() {
        super();
    }

    public Track(Parcel in) {
        readFromParcel(in);
    }

    public Track(DiscogsRelease.Track track, Release release) {
        duration = track.duration;
        position = track.position;
        title = track.title;
        type = track.type_;
        artist = Discogs.formatArtist(track.artists);
        //if the track info does not have an artist set, fetch it from the release
        if(artist.equals("")) artist = release.artist;
        album = release.title;
        this.release = release;
    }

    /*parcelable implementation*/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(idx);
        parcel.writeString(position);
        parcel.writeString(type);
        parcel.writeString(artist);
        parcel.writeString(album);
        parcel.writeString(title);
        parcel.writeString(duration);
    }

    private void readFromParcel(Parcel in) {
        idx = in.readInt();
        position = in.readString();
        type = in.readString();
        artist = in.readString();
        album = in.readString();
        title = in.readString();
        duration = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Creator() {
        @Override
        public Track createFromParcel(Parcel parcel) {
            return new Track(parcel);
        }

        @Override
        public Track[] newArray(int i) {
            return new Track[i];
        }
    };

    /*static formatting functions*/
    public static int formatDurationToSeconds(String duration) {
        int defaultDuration = 0;
        if(duration != null && !duration.equals("") && duration.contains(":")) {
            try {
                String[] tokens = duration.split(":");
                int minutes = Integer.parseInt(tokens[0]);
                int seconds = Integer.parseInt(tokens[1]);
                return (60 * minutes + seconds);
            } catch (NumberFormatException e) {
            }
        }
        return defaultDuration;
    }

    public static String formatDurationToString(int duration) {
        String format = null;
        if(duration > 0) {
            //discogs uses a fixed string representation of duration: mm:ss
            int minutes = (int) Math.floor((double)duration/60.0);
            int seconds = duration%60;
            format = minutes+":"+seconds;
        }
        return  format;
    }
}
