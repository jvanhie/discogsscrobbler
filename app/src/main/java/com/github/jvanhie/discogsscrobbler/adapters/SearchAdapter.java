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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.queries.DiscogsSearchResult;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.SquareView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jono on 20/03/14.
 */
public class SearchAdapter extends BaseExpandableListAdapter {
    private List<DiscogsSearchResult> mResults = new ArrayList<DiscogsSearchResult>();
    private final Context mContext;
    private ImageLoader mImageLoader;
    private DisplayImageOptions mImageOptions;

    public SearchAdapter(Context context, List<DiscogsSearchResult> results) {
        mContext = context;
        mResults = results;
        //create universal image loader
        mImageLoader = ImageLoader.getInstance();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.default_release)
                .cacheInMemory()
                .cacheOnDisc()
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(mContext)
                .defaultDisplayImageOptions(options)
                .imageDownloader(new DiscogsImageDownloader(Discogs.getInstance(mContext)))
                .build();
        mImageLoader.init(config);
    }

    @Override
    public int getGroupCount() {
        return mResults.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return 0;
    }

    @Override
    public Object getGroup(int i) {
        return null;
    }

    @Override
    public Object getChild(int i, int i2) {
        return null;
    }

    @Override
    public long getGroupId(int i) {
        return 0;
    }

    @Override
    public long getChildId(int i, int i2) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public View getChildView(int i, int i2, boolean b, View view, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return false;
    }
}
