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

/**
 * Created by Jono on 01/05/2014. A separate class for recently played items, only storing what is relevant for a recently played list
 * Needs to be converted to a Release for displaying with getRelease()
 */
@Table(name = "recentlyplayed")
public class RecentlyPlayed extends Model{

    @Column(name = "releaseid")
    public long releaseid;

    @Column(name = "title")
    public String title;

    @Column(name = "year")
    public int year;

    @Column(name = "thumb")
    public String thumb;

    @Column(name = "artist")
    public String artist;

    @Column(name = "label")
    public String label = "";

    @Column(name = "format")
    public String format = "";

    @Column(name = "timestamp", index = true)
    public long timestamp;

    public RecentlyPlayed() {
        super();
    }

    public RecentlyPlayed(Release r) {
        this();
        releaseid=r.releaseid;
        title=r.title;
        year=r.year;
        thumb=r.thumb;
        artist= r.artist;
        label= r.label;
        format = r.format;
        timestamp = System.currentTimeMillis();
    }

    public Release getRelease() {
        Release r = new Release();
        r.isTransient = true;
        r.releaseid=releaseid;
        r.title=title;
        r.year=year;
        r.thumb=thumb;
        r.artist=artist;
        r.label=label;
        r.format=format;
        r.timestamp = timestamp;
        return r;
    }


}
