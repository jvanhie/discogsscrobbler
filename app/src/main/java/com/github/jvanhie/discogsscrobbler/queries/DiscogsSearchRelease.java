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

/**
 * Created by Jono on 27/03/2014.
 * A broad class that is populated depending on the query
 * works with artist, label or master release queries
 */
public class DiscogsSearchRelease {
    public long id;
    public String title;
    public String artist;
    //default is release (can be master as well)
    public String type = "release";
    public String thumb;
    public String year;
    public String released;
    public String resource_url;
    public String country;
    public String format;
    public String label;

}
