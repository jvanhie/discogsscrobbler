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

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.util.Discogs;

/**
 * Created by Jono on 22/03/2014.
 */
@Table(name = "tracks")
public class Track extends Model{

    @Column(name = "release", onDelete = Column.ForeignKeyAction.CASCADE)
    public Release release;

    @Column(name = "idx")
    public int idx;

    @Column(name = "duration")
    public String duration;

    @Column(name = "position")
    public String position;

    @Column(name = "title")
    public String title;

    @Column(name = "type")
    public String type;

    @Column(name = "artist")
    public String artist;

    public Track() {
        super();
    }

    public Track(DiscogsRelease.Track track, Release release) {
        duration = track.duration;
        position = track.position;
        title = track.title;
        type = track.type_;
        artist = Discogs.formatArtist(track.artists);
        this.release = release;
    }

}
