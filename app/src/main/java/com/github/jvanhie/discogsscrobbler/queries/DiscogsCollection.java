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

import com.github.jvanhie.discogsscrobbler.models.Release;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jono on 20/03/14.
 */
public class DiscogsCollection {
    private DiscogsPagination pagination;
    private List<ReleaseSummary> releases;

    public DiscogsPagination getPagination() {
        return pagination;
    }

    public List<DiscogsBasicRelease> getReleases() {
        ArrayList<DiscogsBasicRelease> result = new ArrayList<DiscogsBasicRelease>();
        for (ReleaseSummary r : releases) {
            result.add(r.getRelease());
        }
        return result;
    }

    private class ReleaseSummary {
        private long id;
        private long folder_id;
        private int rating;
        private DiscogsBasicRelease basic_information;

        public DiscogsBasicRelease getRelease() {
            basic_information.rating = rating;
            basic_information.folder_id = folder_id;
            return basic_information;
        }

    }

    public class DiscogsBasicRelease {

        public long id;
        public long folder_id;
        public String title;
        public int year;
        public String resource_url;
        public String thumb;
        public int rating;


        public List<DiscogsRelease.Artist> artists;
        public List<DiscogsRelease.Label> labels;
        public List<DiscogsRelease.Format> formats;


    }

}
