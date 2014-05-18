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

package com.github.jvanhie.discogsscrobbler.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearch;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchRelease;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchResult;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.SquareView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jono on 20/03/14.
 */
public class SearchAdapter extends BaseExpandableListAdapter {
    private List<DiscogsSearchResult> mResults = new ArrayList<DiscogsSearchResult>();
    private SparseArray<List<DiscogsSearchRelease>> mReleases;
    private final Context mContext;
    private ImageLoader mImageLoader;

    public SearchAdapter(Context context, List<DiscogsSearchResult> results) {
        mContext = context;
        if(results!=null) mResults = results;
        mReleases = new SparseArray<List<DiscogsSearchRelease>>();
        //create universal image loader
        mImageLoader = ImageLoader.getInstance();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.default_release)
                .cacheInMemory()
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(mContext)
                .defaultDisplayImageOptions(options)
                .imageDownloader(new DiscogsImageDownloader(Discogs.getInstance(mContext)))
                .build();
        mImageLoader.init(config);
    }

    //called when a subquery in the searchfragement was completed
    public void addChildren(int i, List<DiscogsSearchRelease> releases ) {
        mReleases.put(i,releases);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return mResults.size();
    }

    @Override
    public int getChildrenCount(int i) {
        List<DiscogsSearchRelease> releases = mReleases.get(i);
        if(releases == null) return 0;
        else return releases.size();
    }

    @Override
    public Object getGroup(int i) {
        return mResults.get(i);
    }

    @Override
    public Object getChild(int i, int i2) {
        return mReleases.get(i).get(i2);
    }

    @Override
    public long getGroupId(int i) {
        return mResults.get(i).id;
    }

    @Override
    public long getChildId(int i, int i2) {
        return mReleases.get(i).get(i2).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        if (view == null) {
            if(mContext == null) return null;
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.discogs_item, viewGroup, false);
        }
        TextView title = (TextView) view.findViewById(R.id.item_title);
        TextView info1 = (TextView) view.findViewById(R.id.item_info1);
        TextView info2 = (TextView) view.findViewById(R.id.item_info2);
        ImageView img = (ImageView) view.findViewById(R.id.item_image);
        ImageView indicator = (ImageView) view.findViewById(R.id.group_indicator);

        DiscogsSearchResult result = mResults.get(i);

        if(result != null) {
            title.setText(result.title);
            if(result.type.equals("release")) {
                info1.setText(Discogs.formatFormat(result.format));
                info2.setText(result.type + " " + result.year + " " + Discogs.formatGenres(result.genre) + " / " + Discogs.formatStyles(result.style));
            } else if(result.type.equals("artist") || result.type.equals("label")) {
                info1.setText(result.type);
                info2.setText("");
            } else if(result.type.equals("master")) {
                info1.setText(result.type);
                info2.setText(result.year + " " + Discogs.formatGenres(result.genre) + " / " + Discogs.formatStyles(result.style));
            }

            /*check which indicator to show*/
            int imageResourceId = isExpanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float;
            indicator.setImageResource(imageResourceId);
            if(result.type.equals("release")) {
                indicator.setVisibility(View.INVISIBLE);
            } else {
                indicator.setVisibility(View.VISIBLE);
            }

            mImageLoader.displayImage(result.thumb, img);
        }
        return view;
    }

    @Override
    public View getChildView(int i, int i2, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            if(mContext == null) return null;
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.discogs_item, viewGroup, false);
        }
        TextView title = (TextView) view.findViewById(R.id.item_title);
        TextView info1 = (TextView) view.findViewById(R.id.item_info1);
        TextView info2 = (TextView) view.findViewById(R.id.item_info2);
        ImageView img = (ImageView) view.findViewById(R.id.item_image);
        view.setBackgroundColor(Color.LTGRAY);
        DiscogsSearchRelease release = mReleases.get(i).get(i2);

        if(release != null) {

            title.setText(release.title);

            DiscogsSearchResult r = mResults.get(i);
            if(r.type.equals("master")) {
                info1.setText(r.title.substring(0,r.title.indexOf(" - ")));
            }else {
                info1.setText(release.artist);
            }

            if(release.label != null) {
                info2.setText(release.format + " by " + release.label);
            } else {
                info2.setText(release.format);
            }

            mImageLoader.displayImage(release.thumb, img);
        }
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }
}
