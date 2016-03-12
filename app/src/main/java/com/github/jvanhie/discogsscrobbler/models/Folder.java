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
import com.github.jvanhie.discogsscrobbler.queries.DiscogsFolders;

/**
 * Created by Jono on 08/05/2014.
 */
@Table(name = "folders")
public class Folder extends Model {
    @Column(name = "folderid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public long folderid;

    @Column(name = "count")
    public int count;

    @Column(name = "name")
    public String name;

    public Folder() {
        super();
    }

    public Folder(DiscogsFolders.Folder f) {
        folderid = f.id;
        count = f.count;
        name = f.name;
    }

    @Override
    public String toString() {
        return "" + name + " ("+ count+")";
    }
}
