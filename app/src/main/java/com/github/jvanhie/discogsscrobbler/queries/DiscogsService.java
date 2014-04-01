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

import java.io.InputStream;
import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.EncodedPath;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by Jono on 20/03/14.
 */
public interface DiscogsService {

    @GET("/{path}")
    Response getRawPath(@EncodedPath("path") String path);

    @GET("/users/{username}/collection/folders/{folderid}/releases?per_page=100&sort=added&sort_order=asc")
    void getCollection(@Path("username") String username, @Path("folderid") long folderid, @Query("page") int page, Callback<DiscogsCollection> callback);

    @GET("/users/{username}/collection/folders/{folderid}/releases?per_page=1&page=1&sort=added&sort_order=desc")
    void getCollectionLastAdded(@Path("username") String username, @Path("folderid") long folderid, Callback<DiscogsCollection> callback);

    @GET("/oauth/identity")
    void getIdentity(Callback<DiscogsIdentity> callback);

    @GET("/releases/{id}")
    void getRelease(@Path("id") long id, Callback<DiscogsRelease> callback);

    @GET("/database/search")
    void search(@Query("q") String query, Callback<DiscogsSearch> callback);

    @GET("/database/search")
    void searchBarcode(@Query("barcode") String barcode, Callback<DiscogsSearch> callback);

}
