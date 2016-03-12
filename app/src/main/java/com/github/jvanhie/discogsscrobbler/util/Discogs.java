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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.github.jvanhie.discogsscrobbler.DiscogsLoginActivity;
import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.ReleaseListActivity;
import com.github.jvanhie.discogsscrobbler.models.Folder;
import com.github.jvanhie.discogsscrobbler.models.RecentlyPlayed;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsCollection;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsCollection.DiscogsBasicRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsFolders;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsIdentity;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsPriceSuggestion;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearch;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchLoader;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchResult;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsService;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import retrofit.RetrofitError.Kind;
import retrofit.client.Response;

public class Discogs extends ContextWrapper {
    private final String API_ROOT;
    private final String USER_AGENT;

    private SharedPreferences mPrefs;

    //presents on of the available appids, but can also support secondary appids or a user created one
    private String mApiKey;
    private String mApiSecret;
    private String mApiId;

    private String mAccessToken;
    private String mAccessSecret;
    private String mUserName;

    private DiscogsService mDiscogsService;
    private DiscogsService mDiscogsPublicService;
    private OAuthProvider mOAuthProvider;
    private OAuthConsumer mOAuthConsumer;

    private boolean mFoldersChanged = false;
    private long mFolderId = 0;
    private List<Folder> folders;

    private AtomicInteger mRetries=new AtomicInteger(0);
    private static final int MAX_RETRIES = 10;

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

        API_ROOT = res.getString(R.string.discogs_api_root);
        USER_AGENT = res.getString(R.string.user_agent);

        //discogs auth values
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mApiKey = mPrefs.getString("discogs_api_key",null);
        mApiSecret = mPrefs.getString("discogs_api_secret",null);
        mApiId = mPrefs.getString("discogs_api_id", null);
        mAccessToken = mPrefs.getString("discogs_access_token", null);
        mAccessSecret = mPrefs.getString("discogs_access_secret", null);
        mUserName = mPrefs.getString("discogs_user_name", null);

        if(mApiKey == null || mApiSecret == null) {
            //we don't have an appid yet, get one randomly from the key/secret pairs
            String[] apiKeys = res.getStringArray(R.array.discogs_api_keys);
            String[] apiSecrets = res.getStringArray(R.array.discogs_api_secrets);
            int id = new Random().nextInt(apiKeys.length);
            mApiKey = apiKeys[id];
            mApiSecret = apiSecrets[id];
            //store the id as string for the android settingsactivity
            mApiId = "" + id;
        }

        //create the oauth session (for authenticating the user to the set appid)
        createOAuthService();

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

    private void createOAuthService() {
        mOAuthConsumer = new DefaultOAuthConsumer(mApiKey,mApiSecret);

        mOAuthProvider = new DefaultOAuthProvider(
                "https://api.discogs.com/oauth/request_token", "https://api.discogs.com/oauth/access_token",
                "https://www.discogs.com/oauth/authorize");
        mOAuthProvider.setListener(new DiscogsOauthProviderListener());
    }

    private void createDiscogsService(String token, String secret) {
        RetrofitHttpOAuthConsumer oAuthConsumer = new RetrofitHttpOAuthConsumer(mApiKey,mApiSecret);
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

    /*STATIC formatter functions*/
    public static String formatArtist(List<DiscogsRelease.Artist> artists) {
        String ret = "";
        if(artists != null) {
            for (DiscogsRelease.Artist artist : artists) {
                ret += removeNumberFromArtist(artist.name);
                if(!artist.join.equals("") && artists.size()>1) {
                    ret += " " + artist.join + " ";
                }
            }
        }
        return ret;
    }

    public static String formatLabel(List<DiscogsRelease.Label> labels) {
        String ret = "";
        if(labels != null && labels.size()>0) ret = labels.get(0).name;
        ret = removeNumberFromLabel(ret);
        return ret;
    }

    public static String formatFormat(List<DiscogsRelease.Format> formats) {
        String ret = "";
        if(formats != null && formats.size()>0) ret = formats.get(0).name;
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

                if (i < formats.size() - 1) ret += "\n";
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



    //Code reused from Jollybox's VinylScrobbler, thanks!
    public static String removeNumberFromArtist (String artist) {
        Pattern numberExp = Pattern.compile("^(.*?)( \\([0-9]+\\))?$");
        Pattern theExp = Pattern.compile("^(.*?)(, The)$");

        Matcher m = numberExp.matcher(artist);
        if (m.matches()) {
            artist = m.group(1);
        }

        m = theExp.matcher(artist);
        if (m.matches()) {
            return "The " + m.group(1);
        } else {
            return artist;
        }
    }

    public static String removeNumberFromLabel (String label) {
        Pattern numberExp = Pattern.compile("^(.*?)( \\([0-9]+\\))?$");

        Matcher m = numberExp.matcher(label);
        if (m.matches()) {
            label = m.group(1);
        }

        return label;
    }


    /*end of static functions*/

    public boolean isLoggedIn() {
        return (mUserName != null);
    }

    public String getUser() {
        return mUserName;
    }



    public InputStream getImage(String image) throws IOException {
        Response r = null;
        try {
            r =mDiscogsService.getRawPath(image);
        } catch (Exception e) {
            if (mRetries.getAndIncrement()<MAX_RETRIES) {
                return getImage(image);
            }
        }
        if(r == null) {
            return null;
        } else {
            mRetries.set(0);
            return r.getBody().in();
        }
    }

    private void parseRetrofitError(RetrofitError error) {
        if(error.getKind() != RetrofitError.Kind.NETWORK) {
            if(error.getResponse().getStatus()==401) {
                //we made an unauthorized call, token was invalidated, log in again
                logIn();
            }
        }
    }

    public void isCollectionChanged(final DiscogsWaiter waiter) {
        //see if releases are changed
        mDiscogsService.getCollectionLastAdded(mUserName, 0, new Callback<DiscogsCollection>() {
            @Override
            public void success(DiscogsCollection discogsCollection, Response response) {
                mRetries.set(0);
                int collectionSize = discogsCollection.getPagination().items;
                if (collectionSize != 0) {
                    //last online addition
                    long lastAddition = discogsCollection.getReleases().get(0).id;
                    //last local addition
                    long lastLocal = -1;
                    Release last = new Select().from(Release.class).orderBy("id DESC").executeSingle();
                    if (last != null) lastLocal = last.releaseid;
                    //see if the remote collection has changed from the last fetch
                    if (collection.size() != collectionSize || lastLocal != lastAddition || mFoldersChanged) {
                        //yes, it has!
                        System.out.println("collection changed! " + collection.size() + "-" + collectionSize + "|" + lastLocal + "-" + lastAddition + "|" + mFoldersChanged);
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
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isCollectionChanged(waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false);
                    }
                } else {
                    parseRetrofitError(error);
                    waiter.onResult(false);
                }
            }
        });
    }

    public void search(final String query, final DiscogsDataWaiter<List<DiscogsSearchResult>> waiter) {
        mDiscogsService.search(query, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                mRetries.set(0);
                waiter.onResult(true,discogsSearch.results);
            }

            @Override
            public void failure(RetrofitError error) {
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                search(query, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    parseRetrofitError(error);
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void search(final String query, final String type, final DiscogsDataWaiter<List<DiscogsSearchResult>> waiter) {
        mDiscogsService.search(query, type, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                mRetries.set(0);
                waiter.onResult(true, discogsSearch.results);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                search(query, type, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    parseRetrofitError(error);
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void searchBarcode(final String query, final DiscogsDataWaiter<List<DiscogsSearchResult>> waiter) {
        mDiscogsService.searchBarcode(query, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                mRetries.set(0);
                waiter.onResult(true, discogsSearch.results);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                searchBarcode(query, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    parseRetrofitError(error);
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void getArtistReleases(final long id, final int page, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        DiscogsService service = mDiscogsPublicService;
        if(mAccessToken != null && mAccessSecret != null) service = mDiscogsService;
        service.getArtistReleases(id, page, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                ArrayList<DiscogsSearchRelease> result = new ArrayList<DiscogsSearchRelease>();
                for (DiscogsSearchRelease r : discogsSearch.releases) {
                    if (r.type.equals("release")) result.add(r);
                }
                if (discogsSearch.pagination.page < discogsSearch.pagination.pages) {
                    //add dummy release - loader to get more
                    DiscogsSearchLoader loader = new DiscogsSearchLoader();
                    loader.title = "Load more releases";
                    loader.artist = "Click here to load more releases";
                    loader.format = "Page " + (page + 1) + " of " + discogsSearch.pagination.pages;
                    loader.type = "loader";
                    loader.id = 0;
                    //loader specific options
                    loader.parenttype = "artist";
                    loader.page = page + 1;
                    loader.parentid = id;
                    result.add(loader);
                }
                mRetries.set(0);
                waiter.onResult(true, result);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getArtistReleases(id, page, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void getLabelReleases(final long id, final int page, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        DiscogsService service = mDiscogsPublicService;
        if(mAccessToken != null && mAccessSecret != null) service = mDiscogsService;
        service.getLabelReleases(id, page, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                if (discogsSearch.pagination.page < discogsSearch.pagination.pages) {
                    //add dummy release - loader to get more
                    DiscogsSearchLoader loader = new DiscogsSearchLoader();
                    loader.title = "Load more releases";
                    loader.artist = "Click here to load more releases";
                    loader.format = "Page " + (page + 1) + " of " + discogsSearch.pagination.pages;
                    loader.type = "loader";
                    loader.id = 0;
                    //loader specific options
                    loader.parenttype = "label";
                    loader.page = page + 1;
                    loader.parentid = id;
                    discogsSearch.releases.add(loader);
                }
                mRetries.set(0);
                waiter.onResult(true, discogsSearch.releases);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getLabelReleases(id, page, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void getMasterReleases(final long id, final int page, final DiscogsDataWaiter<List<DiscogsSearchRelease>> waiter) {
        DiscogsService service = mDiscogsPublicService;
        if(mAccessToken != null && mAccessSecret != null) service = mDiscogsService;
        service.getMasterReleases(id, page, new Callback<DiscogsSearch>() {
            @Override
            public void success(DiscogsSearch discogsSearch, Response response) {
                if (discogsSearch.pagination.page < discogsSearch.pagination.pages) {
                    //add dummy release - loader to get more
                    DiscogsSearchLoader loader = new DiscogsSearchLoader();
                    loader.title = "Load more releases";
                    loader.artist = "Click here to load more releases";
                    loader.format = "Page " + (page + 1) + " of " + discogsSearch.pagination.pages;
                    loader.type = "loader";
                    loader.id = 0;
                    //loader specific options
                    loader.parenttype = "master";
                    loader.page = page + 1;
                    loader.parentid = id;
                    discogsSearch.versions.add(loader);
                }
                mRetries.set(0);
                waiter.onResult(true, discogsSearch.versions);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getMasterReleases(id, page, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    waiter.onResult(false, null);
                }
            }
        });
    }

    /*returns a transient release from discogs, cannot be saved to the db unless the flag is set to false before saving*/
    public void getRelease(final long id, final DiscogsDataWaiter<Release> waiter) {
        DiscogsService service = mDiscogsPublicService;
        if(mAccessToken != null && mAccessSecret != null) service = mDiscogsService;
        service.getRelease(id, new Callback<DiscogsRelease>() {
            @Override
            public void success(DiscogsRelease discogsRelease, Response response) {
                mRetries.set(0);
                waiter.onResult(true, new Release(discogsRelease, true));
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getRelease(id, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void addRelease(final long id, final DiscogsWaiter waiter) {
        //first check if the item is already in our local collection, not worth looking online if it is
        if(getRelease(id) != null) {
            waiter.onResult(false);
        } else {
            mDiscogsService.addRelease(mUserName, id, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    mRetries.set(0);
                    waiter.onResult(true);
                }

                @Override
                public void failure(RetrofitError error) {
                    if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                        //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                        if (mRetries.getAndIncrement() < MAX_RETRIES) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    addRelease(id, waiter);
                                }
                            }, 1000);
                        } else {
                            //we reached the global max retries, give up
                            waiter.onResult(false);
                        }
                    } else {
                        waiter.onResult(false);
                    }
                }
            });
        }
    }

    public void getPriceSuggestions(final long id, final DiscogsDataWaiter<DiscogsPriceSuggestion> waiter) {
        mDiscogsService.getPriceSuggestions(id, new Callback<DiscogsPriceSuggestion>() {
            @Override
            public void success(DiscogsPriceSuggestion s, Response response) {
                mRetries.set(0);
                waiter.onResult(true, s);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if (mRetries.getAndIncrement() < MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getPriceSuggestions(id, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, null);
                    }
                } else {
                    waiter.onResult(false, null);
                }
            }
        });
    }

    public void removeRelease(final long id, final long instance, final DiscogsWaiter waiter) {
        mDiscogsService.removeRelease(mUserName, id, instance, new Callback<Response>() {
            @Override
            public void success(Response r, Response response) {
                mRetries.set(0);
                waiter.onResult(true);
            }

            @Override
            public void failure(RetrofitError error) {
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                removeRelease(id, instance, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false);
                    }
                } else {
                    waiter.onResult(false);
                }
            }
        });
    }

    public void refreshRelease(final Release release, final DiscogsWaiter waiter) {
        //(re)fetch extended info via authenticated call so we always get a fresh result
        mDiscogsService.getRelease(release.releaseid, new Callback<DiscogsRelease>() {
            @Override
            public void success(DiscogsRelease discogsRelease, Response response) {
                mRetries.set(0);
                release.setValues(discogsRelease);
                release.save();
                waiter.onResult(true);
            }

            @Override
            public void failure(RetrofitError error) {
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refreshRelease(release, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false);
                    }
                } else {
                    waiter.onResult(false);
                    parseRetrofitError(error);
                }
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

    public void preloadCollection() {
        final ProgressDialog refreshDialog = new ProgressDialog(mContext);
        refreshDialog.setTitle("Refreshing your Discogs collection");
        refreshDialog.setMessage("Fetching your current collection state");
        refreshDialog.setProgressStyle(refreshDialog.STYLE_SPINNER);
        refreshDialog.show();

        refreshCollection(new DiscogsWaiter() {
            @Override
            public void onResult(boolean success) {
                refreshDialog.dismiss();
                if(success) {
                    final int total = collection.size();
                    final ProgressDialog progressDialog = new ProgressDialog(mContext);

                    final Thread downloader = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (Release r : collection) {
                                try {
                                    refreshRelease(r, new DiscogsWaiter() {
                                        @Override
                                        public void onResult(boolean success) {
                                            progressDialog.incrementProgressBy(1);
                                            if(progressDialog.getProgress() == total) {
                                                //all updates done, return total result
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });
                                    //Discogs requests a local rate limit of 240 request per minute
                                    Thread.sleep(250);
                                } catch(InterruptedException e) {
                                    //stop the task on interrupt (thrown when user dismisses the dialog)
                                    break;
                                }
                            }
                        }
                    });

                    progressDialog.setTitle("Preloading your Discogs collection");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage("Downloading release data");
                    progressDialog.setMax(total);
                    progressDialog.setProgress(0);
                    progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            downloader.interrupt();
                        }
                    });
                    progressDialog.show();

                    downloader.start();
                }
            }
        });

    }

    public void removeReleases(List<Release> releases, final DiscogsWaiter waiter) {
        final AtomicBoolean allSuccess = new AtomicBoolean(true);
        final AtomicInteger ctr = new AtomicInteger(0);
        final int total = releases.size();
        final Set<Long> toRemove = new TreeSet<Long>();

        for (Release r : releases) {
            toRemove.add(r.releaseid);
            removeRelease(r.releaseid,r.instance_id, new DiscogsWaiter() {
                @Override
                public void onResult(boolean success) {
                    if (!success) allSuccess.set(false);
                    int updates = ctr.incrementAndGet();
                    if (updates == total) {
                        //all updates done, return total result and remove releases from the db
                        removeFromCollection(toRemove);
                        loadCollection();
                        waiter.onResult(allSuccess.get());
                    }
                }
            });
        }
    }

    /*recentlyplayed functions - behind the scenes they use a more lightweight object (RecentlyPlayed) for db persistence*/
    public void setRecentlyPlayed(Release r) {
        RecentlyPlayed recent = new RecentlyPlayed(r);
        recent.save();
        /*hook into the recently played function to add missing releases to the collection*/
        if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("release_auto_add",false)) {
            addRelease(r.releaseid, new DiscogsWaiter() {
                @Override
                public void onResult(boolean success) {
                    if(success) {
                        Toast.makeText(mContext, "Added release to your Discogs collection", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    //TODO: maybe this could get a bit heavy for the main thread, make async?
    public List<Release> getRecentlyPlayed() {
        ArrayList<Release> releases = new ArrayList<Release>();
        //get the user preference on recently played size
        int size = 50;
        try {
            String items = PreferenceManager.getDefaultSharedPreferences(mContext).getString("recently_played_size", "50");
            size = Integer.parseInt(items);
        } catch(NumberFormatException ex) {}
        List<RecentlyPlayed> recent = new Select().from(RecentlyPlayed.class).limit(size).orderBy("timestamp DESC").execute();
        //create release list and store the oldest timestamp
        long minTimestamp = 0;
        for(RecentlyPlayed r : recent) {
            releases.add(r.getRelease());
            minTimestamp=r.timestamp;
        }
        //do some cleanup while we're at it: remove all recently played items with timestamp < oldest timestamp of valid results
        new Delete().from(RecentlyPlayed.class).where("timestamp < ?", minTimestamp).execute();
        return releases;
    }

    public void getFolders(final DiscogsDataWaiter<List<Folder>> waiter) {
        //also check if folders are changed
        mDiscogsService.getFolders(mUserName, new Callback<DiscogsFolders>() {
            @Override
            public void success(DiscogsFolders discogsFolders, Response response) {
                mRetries.set(0);
                if (folders == null) loadFolders();
                if (discogsFolders.folders.size() != folders.size()) {
                    //number of folders has changed, clear current folders in db, mark folder state as dirty
                    new Delete().from(Folder.class).execute();
                    mFoldersChanged = true;
                } else {
                    //compare item count (trigger for folder id refresh)
                    for (int i = 0; i < folders.size(); i++) {
                        for(int j = 0; j < folders.size(); j++) {
                            if(folders.get(i).folderid == discogsFolders.folders.get(j).id) {
                                if (folders.get(i).count != discogsFolders.folders.get(j).count) {
                                    mFoldersChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                folders.clear();
                for (DiscogsFolders.Folder f : discogsFolders.folders) {
                    Folder newFolder = new Folder(f);
                    folders.add(newFolder);
                    newFolder.save();
                }
                waiter.onResult(true, folders);
            }

            @Override
            public void failure(RetrofitError error) {
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getFolders(waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false, folders);
                    }
                } else {
                    if (folders == null) loadFolders();
                    waiter.onResult(false, folders);
                }
            }
        });
    }


    public void loadFolders() {
        folders = new Select().from(Folder.class).orderBy("folderid").execute();
    }

    public void setFolderId(long folderId) {
        this.mFolderId = folderId;
    }

    public long getFolderId() {
        return mFolderId;
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
                mRetries.set(0);
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
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refreshCollection(page, waiter);
                            }
                        }, 1000);
                    } else {
                        //we reached the global max retries, give up
                        waiter.onResult(false);
                    }
                } else {
                    parseRetrofitError(error);
                    waiter.onResult(false);
                }
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

        //if there has been a change in folders, sync local folder ids
        if(mFoldersChanged) {
            syncFolders();
            mFoldersChanged=false;
        }

        //sync complete, reload collection from db
        loadCollection();
    }

    public void syncFolders() {
        HashMap<Long,Long> remoteFolderMap = new HashMap<Long, Long>();
        for (DiscogsBasicRelease r : onlineCollection) {
            if(r!=null) {
                remoteFolderMap.put(r.id, r.folder_id);
            }
        }
        ActiveAndroid.beginTransaction();
        for(Release r : collection) {
            if(r!=null) {
                long folderId = remoteFolderMap.get(r.releaseid);
                if (r.folder_id != folderId) {
                    r.folder_id = folderId;
                    r.save();
                }
            }
        }
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
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
        /*check if we need to filter releases by folder id*/
        if(mFolderId!=0) {
            ArrayList<Release> filtered = new ArrayList<Release>();
            for(Release r : collection) {
                if(r.folder_id==mFolderId) filtered.add(r);
            }
            return filtered;
        } else return collection;
    }

    public String getOAuthURL() {
        try {
            String auth = mOAuthProvider.retrieveRequestToken(mOAuthConsumer, "oauth://discogs");
            return auth;
        } catch (OAuthException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setAccessToken(String verifier) {
        try {
            if(verifier!=null) {
                mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
                setSession(mOAuthConsumer.getToken(), mOAuthConsumer.getTokenSecret());
            }

        } catch (OAuthException e) {
            e.printStackTrace();
        }
    }

    public void setSession(final String accessToken, final String accessSecret) {

        mAccessToken = accessToken;
        mAccessSecret = accessSecret;
        //rebuild the Discogs API adapter with our new token
        createDiscogsService(mAccessToken,mAccessSecret);
        /*verify if the token and secret is valid by requesting the identity object*/
        mDiscogsService.getIdentity(new Callback<DiscogsIdentity>() {
            @Override
            public void success(DiscogsIdentity discogsIdentity, Response response) {
                mRetries.set(0);
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
                if(error.getKind() == Kind.NETWORK || error.getKind() == Kind.UNEXPECTED) {
                    //we should retry network errors with a 1 second delay (discogs rate limit rules), unless we reach our global max retries
                    if(mRetries.getAndIncrement()<MAX_RETRIES) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setSession(accessToken, accessSecret);
                            }
                        }, 1000);
                    }
                }
            }
        });
    }

    public void setCustomApi(String key, String secret) {
        logOut();
        mApiKey = key;
        mApiSecret = secret;
        mApiId = "-1";
        createOAuthService();
        saveSession();
    }

    public void setApiId(int id) {
        logOut();
        String[] apiKeys = getResources().getStringArray(R.array.discogs_api_keys);
        String[] apiSecrets = getResources().getStringArray(R.array.discogs_api_secrets);
        mApiKey = apiKeys[id];
        mApiSecret = apiSecrets[id];
        //store the id as string for the android settingsactivity
        mApiId = "" + id;
        createOAuthService();
        saveSession();
    }

    public int getApiId() {
        return Integer.parseInt(mApiId);
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
                return true;
            }
        };
        t.execute();

    }


    private void saveSession() {
        SharedPreferences.Editor prefEdit = mPrefs.edit();

        if (mApiKey != null) {
            prefEdit.putString("discogs_api_key", mApiKey);
        } else {
            prefEdit.remove("discogs_api_key");
        }

        if (mApiSecret != null) {
            prefEdit.putString("discogs_api_secret", mApiSecret);
        } else {
            prefEdit.remove("discogs_api_secret");
        }

        if (mApiId != null) {
            prefEdit.putString("discogs_api_id", mApiId);
        } else {
            prefEdit.remove("discogs_api_id");
        }

        if (mAccessToken != null) {
            prefEdit.putString("discogs_access_token", mAccessToken);
        } else {
            prefEdit.remove("discogs_access_token");
        }

        if (mAccessSecret != null) {
            prefEdit.putString("discogs_access_secret", mAccessSecret);
        } else {
            prefEdit.remove("discogs_access_secret");
        }

        if (mUserName != null) {
            prefEdit.putString("discogs_user_name", mUserName);
        } else {
            prefEdit.remove("discogs_user_name");
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

