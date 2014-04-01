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

package com.github.jvanhie.discogsscrobbler.queries;

import java.util.List;

/**
 * Created by Jono on 27/03/2014.
 */
public class DiscogsRelease {
    public long id;
    public String title;
    public int year;
    public String resource_url;
    public int master_id;
    public String master_url;
    public String country;
    public String released_formatted;
    public String notes;
    public String thumb;

    public String[] styles;
    public String[] genres;
    public List<Artist> artists;
    public List<Label> labels;
    public List<Format> formats;
    public List<Image> images;
    public List<Track> tracklist;

    public class Artist {
        public String name;
    }

    public class Label {
        public String name;
    }

    public class Format {
        public String qty;
        public String[] descriptions;
        public String text;
        public String name;
    }

    public class Image {
        public String type;
        public int width;
        public int height;
        public String uri;
        public String uri150;
    }

    public class Track {
        public String duration;
        public String position;
        public String title;
        public String type_;
    }

}
