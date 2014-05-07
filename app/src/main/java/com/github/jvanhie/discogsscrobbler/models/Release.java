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

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsCollection.DiscogsBasicRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Jono on 20/03/14.
 */
@Table(name = "releases")
public class Release extends Model{
    /*values part of the basic release info*/
    @Column(name = "releaseid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public long releaseid;

    @Column(name = "title")
    public String title;

    @Column(name = "year")
    public int year;

    @Column(name = "resource_url")
    public String resource_url;

    @Column(name = "thumb")
    public String thumb;

    @Column(name = "artist")
    public String artist;

    @Column(name = "label")
    public String label = "";

    @Column(name = "format")
    public String format = "";

    /*extended value*/
    @Column(name = "extended")
    public boolean hasExtendedInfo;

    @Column(name = "master_id")
    public int master_id;

    @Column(name = "master_url")
    public String master_url;

    @Column(name = "country")
    public String country;

    @Column(name = "format_extended")
    public String format_extended;

    @Column(name = "released_formatted")
    public String released_formatted;

    @Column(name = "notes")
    public String notes;

    @Column(name = "styles")
    public String styles;

    @Column(name = "genres")
    public String genres;

    /*transient members*/
    public boolean isTransient = false;
    public long timestamp = 0;
    private List<Image> mImages;
    private List<Track> mTracks;


    public List<Image> images() {
        if(isTransient) {
            return mImages;
        } else {
            return getMany(Image.class, "Release");
        }
    }

    public List<Track> tracklist() {
        if(isTransient) {
            return mTracks;
        } else {
            return getMany(Track.class, "Release");
        }
    }

    public Release () {
        super();
    }

    public Release(DiscogsBasicRelease r) {
        this();
        setValues(r);
    }

    public Release(DiscogsSearchRelease r) {
        this();
        setValues(r);
    }

    public Release(DiscogsRelease r) {
        this();
        setValues(r,false);
    }

    public Release(DiscogsRelease r, boolean isTransient) {
        this();
        setValues(r,isTransient);
    }

    public void setValues(DiscogsSearchRelease r) {
        releaseid = r.id;
        title = r.title;
        try {
            year = Integer.parseInt(r.year);
        } catch (NumberFormatException ex) {}
        resource_url=r.resource_url;
        thumb=r.thumb;
        artist= r.artist;
        label= r.label;
        format = r.format;
        isTransient=true;
    }


    public void setValues(DiscogsBasicRelease r) {
        releaseid=r.id;
        title=r.title;
        year=r.year;
        resource_url=r.resource_url;
        thumb=r.thumb;
        artist= Discogs.formatArtist(r.artists);
        label= Discogs.formatLabel(r.labels);
        format = Discogs.formatFormat(r.formats);
    }

    public void setValues(DiscogsRelease r) {
        if(r!=null) setValues(r,false);
    }

    /*perform a full load/update of the release*/
    public void setValues(DiscogsRelease r, boolean isTransient) {
        releaseid=r.id;
        title=r.title;
        year=r.year;
        resource_url=r.resource_url;
        thumb=r.thumb;
        master_id=r.master_id;
        master_url=r.master_url;
        country=r.country;
        released_formatted=r.released_formatted;
        notes=r.notes;
        artist= Discogs.formatArtist(r.artists);
        label= Discogs.formatLabel(r.labels);
        format = Discogs.formatFormat(r.formats);
        format_extended = Discogs.formatFormatExtended(r.formats);
        genres = Discogs.formatGenres(r.genres);
        styles = Discogs.formatStyles(r.styles);
        this.isTransient = isTransient;
        if(isTransient) {
            mImages = new ArrayList<Image>();
            if(r.images != null) {
                for (int i = 0; i < r.images.size(); i++) {
                    Image image = new Image(r.images.get(i), this);
                    image.idx = i;
                    mImages.add(image);
                }
            }
            mTracks = new ArrayList<Track>();
            if(r.tracklist!=null) {
                for (int i = 0; i < r.tracklist.size(); i++) {
                    Track track = new Track(r.tracklist.get(i), this);
                    track.idx = i;
                    mTracks.add(track);
                }
            }
        } else {
            //create Image descriptors and tracklist, store in db immediatly, but first remove any old linked data
            ActiveAndroid.beginTransaction();
            for (Image i : images()) {
                i.delete();
            }
            for (Track t : tracklist()) {
                t.delete();
            }
            if(r.images != null) {
                for (int i = 0; i < r.images.size(); i++) {
                    Image image = new Image(r.images.get(i), this);
                    image.idx = i;
                    image.save();
                }
            }
            if(r.tracklist!=null) {
                for (int i = 0; i < r.tracklist.size(); i++) {
                    Track track = new Track(r.tracklist.get(i), this);
                    track.idx = i;
                    track.save();
                }
            }
            ActiveAndroid.setTransactionSuccessful();
            ActiveAndroid.endTransaction();
        }

        //indicate that extended info is present
        hasExtendedInfo = true;
    }

}
