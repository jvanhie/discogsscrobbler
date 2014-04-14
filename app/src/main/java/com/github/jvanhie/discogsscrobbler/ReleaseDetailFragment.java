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

package com.github.jvanhie.discogsscrobbler;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.github.jvanhie.discogsscrobbler.models.Image;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsPriceSuggestion;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.Lastfm;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

/**
 * A fragment representing a single Release detail screen.
 * This fragment is either contained in a {@link ReleaseListActivity}
 * in two-pane mode (on tablets) or a {@link ReleaseDetailActivity}
 * on handsets.
 */
public class ReleaseDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private Release mRelease;

    private Discogs mDiscogs;

    private ImageLoader mImageLoader;

    private View mRootView;

    public boolean hasMenu = false;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReleaseDetailFragment() {
    }

    public ReleaseDetailFragment(boolean hasMenu) {
        this();
        this.hasMenu = hasMenu;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mDiscogs == null) mDiscogs = Discogs.getInstance(getActivity());
        if(mImageLoader == null) {
            //create universal image loader
            mImageLoader = ImageLoader.getInstance();
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showStubImage(R.drawable.default_release)
                    .cacheInMemory()
                    .cacheOnDisc()
                    .displayer(new FadeInBitmapDisplayer(500))
                    .build();
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
                    .enableLogging()
                    .defaultDisplayImageOptions(options)
                    .imageDownloader(new DiscogsImageDownloader(mDiscogs))
                    .build();
            mImageLoader.init(config);
        }
        //get id from arguments
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mRelease = mDiscogs.getRelease(getArguments().getLong(ARG_ITEM_ID,0));
            //get price suggestions
            mDiscogs.getPriceSuggestions(getArguments().getLong(ARG_ITEM_ID,0),new Discogs.DiscogsDataWaiter<DiscogsPriceSuggestion>() {
                @Override
                public void onResult(boolean success, DiscogsPriceSuggestion data) {
                    for (DiscogsPriceSuggestion.Quality quality : data.getSuggestion()) {
                        System.out.println(quality.type + ": " + quality.value + " " + quality.currency);
                    }
                }
            });
        }

        if(hasMenu) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("enable_discogs", true)) {
            if (mRelease == null || mRelease.isTransient) {
                //the release is not in the collection, give the user the opportunity to add it
                inflater.inflate(R.menu.release_detail_search, menu);
            }
        }

        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("enable_lastfm", true)) {
            inflater.inflate(R.menu.release_detail_scrobble, menu);
        }



    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //only handle options when visible and instantiated with menu power
        if(hasMenu && isVisible()) {
            int id = item.getItemId();
            if (id == R.id.detail_add_to_discogs) {
                mDiscogs.addRelease(mRelease.releaseid, new Discogs.DiscogsWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            Toast.makeText(getActivity(), "Added release to Discogs collection", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            if (id == R.id.detail_scrobble_release) {
                Lastfm lastfm = Lastfm.getInstance(getActivity());
                if (lastfm.isLoggedIn()) {
                    //we're in detailview, just scrobble everything
                    final Release release = mRelease;
                    if (release != null) {
                        lastfm.scrobbleTracks(release.tracklist(), new Lastfm.LastfmWaiter() {
                            @Override
                            public void onResult(boolean success) {
                                Toast.makeText(getActivity(), "Scrobbled " + release.tracklist().size() + " tracks", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    //log in first
                    lastfm.logIn();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_release_detail, container, false);
        mRootView = rootView;
        // Show the content
        if (mRelease != null) {
            setRelease();
            if (!mRelease.hasExtendedInfo) {
                //we don't got extended info on this release yet, get it and display it
                mDiscogs.refreshRelease(mRelease, new Discogs.DiscogsWaiter() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            //refresh the info
                            setRelease();
                        }
                    }
                });
            }
        } else {
            mDiscogs.getRelease(getArguments().getLong(ARG_ITEM_ID,0), new Discogs.DiscogsDataWaiter<Release>() {
                @Override
                public void onResult(boolean success, Release data) {
                    if(success) {
                        mRelease = data;
                        setRelease();
                    }
                }
            });
        }

        return rootView;
    }

    private void setRelease() {
        ((TextView) mRootView.findViewById(R.id.detail_title)).setText(mRelease.title);
        ((TextView) mRootView.findViewById(R.id.detail_artist)).setText(mRelease.artist);
        ((TextView) mRootView.findViewById(R.id.detail_label)).setText(mRelease.label);
        ((TextView) mRootView.findViewById(R.id.detail_format)).setText(mRelease.format_extended);
        ((TextView) mRootView.findViewById(R.id.detail_country)).setText(mRelease.country);
        ((TextView) mRootView.findViewById(R.id.detail_released)).setText(mRelease.released_formatted);
        ((TextView) mRootView.findViewById(R.id.detail_genre)).setText(mRelease.genres);
        ((TextView) mRootView.findViewById(R.id.detail_style)).setText(mRelease.styles);
        ((TextView) mRootView.findViewById(R.id.detail_notes)).setText(mRelease.notes);
        //decide to load big or small image
        if(!mRelease.hasExtendedInfo) {
            //we don't got extended info on this release yet, only display thumbnail
            ImageView thumb = (ImageView) mRootView.findViewById(R.id.detail_thumb);
            thumb.setVisibility(ImageView.VISIBLE);
            mImageLoader.displayImage(mRelease.thumb,thumb);
        } else {
            ImageView image = (ImageView) mRootView.findViewById(R.id.detail_image);

            if(mRelease.images().size()>0) {
                Image img = mRelease.images().get(0);
                mImageLoader.displayImage(img.uri, image, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(Bitmap loadedImage) {
                        super.onLoadingComplete(loadedImage);
                        //make the small thumbnail invisible
                        mRootView.findViewById(R.id.detail_thumb).setVisibility(ImageView.INVISIBLE);
                    }
                });
            }
        }
    }

}
