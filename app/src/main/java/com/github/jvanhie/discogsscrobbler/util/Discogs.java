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

package com.github.jvanhie.discogsscrobbler.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.github.jvanhie.discogsscrobbler.DiscogsLoginActivity;
import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.ReleaseListActivity;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsCollection.DiscogsBasicRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsCollection;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsIdentity;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearch;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchResult;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsService;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.OAuthProviderListener;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.http.HttpResponse;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class Discogs extends ContextWrapper {
    private final String API_KEY;
    private final String API_SECRET;
    private final String API_ROOT;
    private final String USER_AGENT;

    private SharedPreferences mPrefs;

    private String mAccessToken;
    private String mAccessSecret;
    private String mUserName;

    private DiscogsService mDiscogsService;
    private DiscogsService mDiscogsPublicService;
    private OAuthProvider mOAuthProvider;
    private OAuthConsumer mOAuthConsumer;

    private List<Release> collection;
    private List<DiscogsBasicRelease> onlineCollection = new ArrayList<DiscogsBasicRelease>();

    private Context mContext;

    private static Discogs instance;

    public static synchronized Discogs getInstance(Context context) {
        if (instance == null) {
            instance = new Discogs(context);
        }
        else {
            //last caller gets to set the mContext object
            instance.mContext = context;
        }
        return instance;
    }

    public Discogs(Context context) {
        super(context);
        this.mContext = context;
        Resources res = context.getResources();

        API_KEY = res.getString(R.string.discogs_api_key);
        API_SECRET = res.getString(R.string.discogs_api_secret);
        API_ROOT = res.getString(R.string.discogs_api_root);
        USER_AGENT = res.getString(R.string.user_agent);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAccessToken = mPrefs.getString("access_token", null);
        mAccessSecret = mPrefs.getString("access_secret", null);
        mUserName = mPrefs.getString("user_name", null);

        mOAuthConsumer = new DefaultOAuthConsumer(API_KEY,API_SECRET);

        mOAuthProvider = new DefaultOAuthProvider(
                "http://api.discogs.com/oauth/request_token", "http://api.discogs.com/oauth/access_token",
                "http://www.discogs.com/oauth/authorize");
        mOAuthProvider.setListener(new DiscogsOauthProviderListener());

        //create API call adapter with oauth support
        createDiscogsService(mAccessToken, mAccessSecret);

        //create API without oauth for other queries
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("User-Agent", USER_AGENT);
            }
        };
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(API_ROOT).setRequestInterceptor(requestInterceptor).setLogLevel(RestAdapter.LogLevel.BASIC).build();
        mDiscogsPublicService = restAdapter.create(DiscogsService.class);
    }

    /*STATIC formatter functions*/
    public static String formatArtist(List<DiscogsRelease.Artist> artists) {
        String ret = "";
        if(artists.size()>0) ret = artists.get(0).name;
        return ret;
    }

    public static String formatLabel(List<DiscogsRelease.Label> labels) {
        String ret = "";
        if(labels.size()>0) ret = labels.get(0).name;
        return ret;
    }

    public static String formatFormat(List<DiscogsRelease.Format> formats) {
        String ret = "";
        if(formats.size()>0) ret = formats.get(0).name;
        return ret;
    }

    public static String formatFormatExtended(List<DiscogsRelease.Format> formats) {
        String ret = "";
        if(formats != null) {
            for (int i = 0; i < formats.size(); i++) {
                DiscogsRelease.Format format = formats.get(i);
                try {
                    if (Integer.parseInt(format.qty) > 1) {
                        ret += formats.get(i).qty + "x";
                    }
                } catch (NumberFormatException e) {}
                if(format.name != null) {
                    ret += format.name;
                }
                if(format.descriptions != null) {
                    ret += ", " +formatDescription(format.descriptions);
                }
                if(format.text != null) {
                    ret += ", " + format.text;
                }

                if (i < formats.size() - 1) ret += ", ";
            }
        }
        return ret;
    }

    public static String formatDescription(String[] desc) {
        String ret = "";
        if(desc != null) {
            for (int i = 0; i < desc.length; i++) {
                ret += desc[i];
                if (i < desc.length - 1) ret += ", ";
            }
        }
        return ret;
    }

    public static String formatFormat(String[] formats) {
        String ret = "";
        if(formats != null) {
            for (int i = 0; i < formats.length; i++) {
                ret += formats[i];
                if (i < formats.length - 1) ret += ", ";
            }
        }
        return ret;
    }

    public static String formatStyles(String[] styles) {
        String ret = "";
        if(styles != null) {
            for (int i = 0; i < styles.length; i++) {
                ret += styles[i];
                if (i < styles.length - 1) ret += ", ";
            }
        }
        return ret;
    }

    public static String formatGenres(String[] genres) {
        String ret = "";
        if(genres != null) {
            for (int i = 0; i < genres.length; i++) {
                ret += genres[i];
                if (i < genres.length - 1) ret += ", ";
            }
        }
        return ret;
    }

    /*end of static functions*/

    public String getUser() {
        return mUserName;
    }

    private void createDiscogsService(String token, String secret) {
        RetrofitHttpOAuthConsumer oAuthConsumer = new RetrofitHttpOAuthConsumer(API_KEY, API_SECRET);
        oAuthConsumer.setTokenWithSecret(token, secret);
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("User-Agent", USER_AGENT);
            }
        };
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(API_ROOT).setRequestInterceptor(requestInterceptor).setLogLevel(RestAdapter.LogLevel.BASIC).setClient(new SigningOkClient(oAuthConsumer)).build();
        mDiscogsService = restAdapter.create(DiscogsService.class);
    }

    public InputStream getImage(String image) throws IOException {
        Response r = mDiscogsService.getRawPath(image);
        return (r == null) ? null : r.getBody().in();
    }

    public void isCollectionChanged(final DiscogsWaiter waiter) {

        mDiscogsService.getCollectionLastAdded(mUserName, 0, new Callback<DiscogsCollection>() {
            @Override
            public void success(DiscogsCollection discogsCollection, Response response) {
                int collectionSize = discogsCollection.getPagination().items;
                if (collectionSize != 0) {
                    //last online addition
                    long lastAddition = discogsCollection.getReleases().get(0).id;
                    //last local addition
                    long lastLocal = -1;
                    Release last = new Select().from(Release.class).orderBy("id DESC").executeSingle();
                    if (last != null) lastLocal = last.releaseid;
                    System.out.println("collection ids: " + lastLocal + " vs " + lastAddition);
                    System.out.println("collection size: " + collection.size() + " vs " + collectionSize);
                    //see if the remote collection has changed from the last fetch
                    if (collection.size() != collectionSize || lastLocal != lastAddition) {
                        //yes, it has!
                        waiter.onResult(true);
                    } else {
                        waiter.onResult(false);
                    }
                } else {
                    waiter.onResult(false);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                parseRetrofitError(error);
                waiter.onResult(false);
            }
        });
    }

    private void parseRetrofitError(RetrofitError error) {
        if(!error.isNetworkError()) {
            if(error.getResponse().getStatus()==401) {
                //we made an unauthorized call, token was invalidated, log in again
                Toast.makeText(mContext, mContext.getString(R.string.oauth_error), Toast.LENGTH_SHORT).show();
                logIn();
            }
        }
    }

    public void search(String query, final DiscogsDataWaiter<List<DiscogsSearchResult>> waiter) {
        mDiscogsPublicService.search(query, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                waiter.onResult(true,discogsSearch.results);
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false,null);
            }
        });
    }

    public void getArtistReleases(long id, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        mDiscogsPublicService.getArtistReleases(id, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                ArrayList<DiscogsSearchRelease> result = new ArrayList<DiscogsSearchRelease>();
                for(DiscogsSearchRelease r : discogsSearch.releases) {
                    if (r.type.equals("release")) result.add(r);
                }
                waiter.onResult(true, result);
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false, null);
            }
        });
    }

    public void getLabelReleases(long id, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        mDiscogsPublicService.getLabelReleases(id, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                waiter.onResult(true, discogsSearch.releases);
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false, null);
            }
        });
    }

    public void getMasterReleases(long id, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        mDiscogsPublicService.getMasterReleases(id, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                waiter.onResult(true, discogsSearch.versions);
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false, null);
            }
        });
    }

    /*returns a transient release from discogs, cannot be saved to the db unless the flag is set to false before saving*/
    public void getRelease(long id, final DiscogsDataWaiter<Release> waiter) {
        mDiscogsPublicService.getRelease(id, new Callback<DiscogsRelease>() {
            @Override
            public void success(DiscogsRelease discogsRelease, Response response) {
                waiter.onResult(true,new Release(discogsRelease,true));
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false,null);
                parseRetrofitError(error);
            }
        });
    }

    public void refreshRelease(final Release release, final DiscogsWaiter waiter) {
        //(re)fetch extended info
        mDiscogsPublicService.getRelease(release.releaseid, new Callback<DiscogsRelease>() {
            @Override
            public void success(DiscogsRelease discogsRelease, Response response) {
                release.setValues(discogsRelease);
                release.save();
                waiter.onResult(true);
            }

            @Override
            public void failure(RetrofitError error) {
                waiter.onResult(false);
                parseRetrofitError(error);
            }
        });
    }

    public void refreshReleases(List<Release> releases, final DiscogsWaiter waiter) {
        final AtomicBoolean allSuccess = new AtomicBoolean(true);
        final AtomicInteger ctr = new AtomicInteger(0);
        final int total = releases.size();

        for (Release r : releases) {
            refreshRelease(r, new DiscogsWaiter() {
                @Override
                public void onResult(boolean success) {
                    if(!success) allSuccess.set(false);
                    int updates = ctr.incrementAndGet();
                    if(updates == total) {
                        //all updates done, return total result
                        waiter.onResult(allSuccess.get());
                    }
                }
            });
        }
    }

    public void refreshCollection(final DiscogsWaiter waiter) {
        onlineCollection = new ArrayList<DiscogsBasicRelease>();
        refreshCollection(1, waiter);
    }

    private void refreshCollection(final int page, final DiscogsWaiter waiter) {
        //access discogs releases
        mDiscogsService.getCollection(mUserName, 0, page, new Callback<DiscogsCollection>() {
            @Override
            public void success(DiscogsCollection result, Response response) {
                //store collection for parsing
                onlineCollection.addAll(result.getReleases());
                if (page < result.getPagination().pages) {
                    //load more! More I tell you!
                    refreshCollection(page + 1, waiter);
                } else {
                    //we have reached the last page, sync local collection
                    syncCollection();
                    waiter.onResult(true);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                parseRetrofitError(error);
                waiter.onResult(false);
            }
        });
    }

    //this function is called when the local collection needs to be synced
    private void syncCollection() {
        SortedSet<Long> localIds = new TreeSet<Long>();
        SortedSet<Long> onlineIds = new TreeSet<Long>();
        if(collection==null) loadCollection();
        for (Release r : collection) {
            localIds.add(r.releaseid);
        }
        for (DiscogsBasicRelease r : onlineCollection) {
            onlineIds.add(r.id);
        }

        //create list to remove -> local ids that are not there online
        SortedSet<Long> toRemove = new TreeSet<Long>(localIds);
        toRemove.removeAll(onlineIds);
        removeFromCollection(toRemove);

        //create list to add -> online ids that are not local
        onlineIds.removeAll(localIds);
        List<DiscogsBasicRelease> toAdd = new ArrayList<DiscogsBasicRelease>();
        for(DiscogsBasicRelease r : onlineCollection) {
            if(onlineIds.contains(r.id)) toAdd.add(r);
        }
        addToCollection(toAdd);

        //sync complete, reload collection from db
        loadCollection();
    }

    public void removeFromCollection(Set<Long> toRemove) {
        ActiveAndroid.beginTransaction();
        try {
            for (Release r : collection) {
                if(toRemove.contains(r.releaseid)) {
                    Release.delete(Release.class,r.getId());
                }
            }
            ActiveAndroid.setTransactionSuccessful();
        }
        finally {
            ActiveAndroid.endTransaction();
        }
    }

    public void addToCollection(List<DiscogsBasicRelease> toAdd) {
        ActiveAndroid.beginTransaction();
        try {
            for (DiscogsBasicRelease r : toAdd) {
                Release newRelease = new Release(r);
                newRelease.save();
            }
            ActiveAndroid.setTransactionSuccessful();
        }
        finally {
            ActiveAndroid.endTransaction();
        }
    }

    public void loadCollection() {
        collection = new Select().from(Release.class).orderBy("artist COLLATE NOCASE").execute();
    }

    public List<Release> getCollection() {
        if(collection==null) loadCollection();
        return collection;
    }

    public String getOAuthURL() {
        try {
            String auth = mOAuthProvider.retrieveRequestToken(mOAuthConsumer, "oauth://discogs");
            System.out.println("OAuthURL: " + auth);
            return auth;
        } catch (OAuthException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setAccessToken(String verifier) {
        try {
            mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
            System.out.println("OAuth results: " + verifier + " -> " + mOAuthConsumer.getToken() + "/" + mOAuthConsumer.getTokenSecret());
            setSession(mOAuthConsumer.getToken(), mOAuthConsumer.getTokenSecret());

        } catch (OAuthException e) {
            e.printStackTrace();
        }
    }

    public void setSession(String accessToken, String accessSecret) {

        mAccessToken = accessToken;
        mAccessSecret = accessSecret;
        //rebuild the Discogs API adapter with our new token
        createDiscogsService(mAccessToken,mAccessSecret);
        /*verify if the token and secret is valid by requesting the identity object*/
        mDiscogsService.getIdentity(new Callback<DiscogsIdentity>() {
            @Override
            public void success(DiscogsIdentity discogsIdentity, Response response) {
                if (discogsIdentity.username != null) {
                    //only save the credentials when a username has been correctly parsed
                    mUserName = discogsIdentity.username;
                    saveSession();
                    //credentials have been updated, restart app from the beginning
                    //great succes, start again in the beginning please.
                    Intent intent = new Intent(mContext, ReleaseListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    public void logIn() {
        startActivity(new Intent(mContext, DiscogsLoginActivity.class));
    }

    public void logOut() {
        mAccessToken = null;
        mAccessSecret = null;
        mUserName = null;
        saveSession();
        //remove saved db and cache items in asynctask
        AsyncTask t = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                ActiveAndroid.beginTransaction();
                loadCollection();
                for(Release r : collection) r.delete();
                ActiveAndroid.setTransactionSuccessful();
                ActiveAndroid.endTransaction();
                ImageLoader.getInstance().clearDiscCache();
                ImageLoader.getInstance().clearMemoryCache();
                System.out.println("cleared all local data");
                return true;
            }
        };
        t.execute();

    }

    private void saveSession() {
        SharedPreferences.Editor prefEdit = mPrefs.edit();

        if (mAccessToken != null) {
            prefEdit.putString("access_token", mAccessToken);
        } else {
            prefEdit.remove("access_token");
        }

        if (mAccessSecret != null) {
            prefEdit.putString("access_secret", mAccessSecret);
        } else {
            prefEdit.remove("access_secret");
        }

        if (mUserName != null) {
            prefEdit.putString("user_name", mUserName);
        } else {
            prefEdit.remove("user_name");
        }

        prefEdit.commit();
    }

    public Release getRelease(long releasid) {
        return new Select().from(Release.class).where("releaseid = ?",releasid).executeSingle();
    }

    public interface DiscogsWaiter {
        public void onResult(boolean success);
    }

    public interface DiscogsDataWaiter<T> {
        public void onResult(boolean success, T data);
    }

    private class DiscogsOauthProviderListener implements OAuthProviderListener {

        @Override
        public void prepareRequest(HttpRequest request) throws Exception {
            request.setHeader("User-Agent", USER_AGENT);
        }

        @Override
        public void prepareSubmission(HttpRequest request) throws Exception {

        }

        @Override
        public boolean onResponseReceived(HttpRequest request, HttpResponse response) throws Exception {
            return false;
        }
    }

}

