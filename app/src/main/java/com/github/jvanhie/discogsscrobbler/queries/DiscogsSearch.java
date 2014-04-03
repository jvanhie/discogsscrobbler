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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jono on 20/03/14.
 */
public class DiscogsSearch {
    public DiscogsPagination pagination;
    public List<DiscogsSearchResult> results;
    public List<DiscogsSearchRelease> releases;
    public List<DiscogsSearchRelease> versions;
}
