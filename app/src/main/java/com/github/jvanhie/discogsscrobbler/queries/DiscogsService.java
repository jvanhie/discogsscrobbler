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
import retrofit.http.DELETE;
import retrofit.http.EncodedPath;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by Jono on 20/03/14.
 */
public interface DiscogsService {

    @GET("/{path}")
    Response getRawPath(@EncodedPath("path") String path);

    @GET("/users/{username}/collection/folders")
    void getFolders(@Path("username") String username, Callback<DiscogsFolders> callback);

    @GET("/users/{username}/collection/folders/{folderid}/releases?per_page=100&sort=added&sort_order=asc")
    void getCollection(@Path("username") String username, @Path("folderid") long folderid, @Query("page") int page, Callback<DiscogsCollection> callback);

    @GET("/users/{username}/collection/folders/{folderid}/releases?per_page=1&page=1&sort=added&sort_order=desc")
    void getCollectionLastAdded(@Path("username") String username, @Path("folderid") long folderid, Callback<DiscogsCollection> callback);

    @GET("/oauth/identity")
    void getIdentity(Callback<DiscogsIdentity> callback);

    @GET("/releases/{id}")
    void getRelease(@Path("id") long id, Callback<DiscogsRelease> callback);

    @GET("/artists/{id}/releases?per_page=100")
    void getArtistReleases(@Path("id") long id, @Query("page") int page, Callback<DiscogsSearch> callback);

    @GET("/labels/{id}/releases?per_page=100")
    void getLabelReleases(@Path("id") long id, @Query("page") int page, Callback<DiscogsSearch> callback);

    @GET("/masters/{id}/versions?per_page=100")
    void getMasterReleases(@Path("id") long id, @Query("page") int page, Callback<DiscogsSearch> callback);

    @GET("/users/{username}/collection/folders/0/releases/{id}/instances/1")
    void getCollectionRelease(@Path("username") String username, @Path("id") long id, Callback<DiscogsRelease> callback);

    @POST("/users/{username}/collection/folders/1/releases/{id}")
    void addRelease(@Path("username") String username, @Path("id") long id, Callback<Response> callback);

    @DELETE("/users/{username}/collection/folders/0/releases/{id}/instances/1")
    void removeRelease(@Path("username") String username, @Path("id") long id, Callback<Response> callback);

    @GET("/database/search")
    void search(@Query("q") String query, Callback<DiscogsSearch> callback);

    @GET("/database/search")
    void search(@Query("q") String query,@Query("type") String type, Callback<DiscogsSearch> callback);

    @GET("/database/search")
    void searchBarcode(@Query("barcode") String barcode, Callback<DiscogsSearch> callback);

    @GET("/marketplace/price_suggestions/{id}")
    void getPriceSuggestions(@Path("id") long id, Callback<DiscogsPriceSuggestion> callback);

}
