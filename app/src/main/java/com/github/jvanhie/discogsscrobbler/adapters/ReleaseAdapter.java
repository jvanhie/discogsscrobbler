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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.github.jvanhie.discogsscrobbler.R;
import com.github.jvanhie.discogsscrobbler.models.Release;
import com.github.jvanhie.discogsscrobbler.util.Discogs;
import com.github.jvanhie.discogsscrobbler.util.DiscogsImageDownloader;
import com.github.jvanhie.discogsscrobbler.util.SquareView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Jono on 20/03/14.
 */
public class ReleaseAdapter extends BaseAdapter implements Filterable, SectionIndexer {
    private List<Release> mReleases = new ArrayList<Release>();
    private List<Release> mUnfilteredReleases;
    private final Context mContext;
    private ImageLoader mImageLoader;
    private HashMap<String, Integer> mAlphaIndexer;
    private String[] mSections;
    private Integer[] mSectionValues;


    public ReleaseAdapter(Context context, List<Release> releases) {
        mContext = context;
        mReleases = releases;
        mUnfilteredReleases = new ArrayList<Release>(releases);
        updateAlphaIndex();
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

    private void updateAlphaIndex() {
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> sectionValues = new ArrayList<Integer>();
        Character currSection = Character.UNASSIGNED;

        for (int i = 0; i < mReleases.size(); i++) {
            Character ch = mReleases.get(i).artist.substring(0, 1).toUpperCase().charAt(0);
            if(!Character.isLetter(ch)) ch = '#';
            if (!ch.equals(currSection)) {
                sections.add(ch.toString());
                sectionValues.add(i);
                currSection=ch;
            }
        }

        mSections=new String[sections.size()];
        sections.toArray(mSections);

        mSectionValues=new Integer[sectionValues.size()];
        sectionValues.toArray(mSectionValues);

    }

    public int getCount() {
        return mReleases.size();
    }

    public Object getItem(int position) {
        return mReleases.get(position);
    }

    public long getItemId(int position) {
        return mReleases.get(position).releaseid;
    }

    public View getView(int position, View view, ViewGroup parent) {
        if (parent instanceof GridView) {

            if (view == null) {
                if(mContext == null) return null;
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.discogs_wall_item, parent, false);
            }

            SquareView img = (SquareView)view.findViewById(R.id.square_view);
            Release release = mReleases.get(position);
            mImageLoader.displayImage(release.thumb, img);

            return view;
        } else {
            if (view == null) {
                if(mContext == null) return null;
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.discogs_item, parent, false);
            }
            TextView title = (TextView) view.findViewById(R.id.item_title);
            TextView info1 = (TextView) view.findViewById(R.id.item_info1);
            TextView info2 = (TextView) view.findViewById(R.id.item_info2);
            ImageView img = (ImageView) view.findViewById(R.id.item_image);

            Release release = mReleases.get(position);
            title.setText(release.title);
            info1.setText(release.artist);
            info2.setText(release.format + " by " + release.label);

            mImageLoader.displayImage(release.thumb, img);

            return view;
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    // No filter implemented return entire
                    results.values = mUnfilteredReleases;
                    results.count = mUnfilteredReleases.size();
                }
                else {
                    // perform filtering operation
                    List<Release> filteredReleases = new ArrayList<Release>();

                    for (Release r : mUnfilteredReleases) {
                        if (r.title.toLowerCase().contains(constraint.toString().toLowerCase()) || r.artist.toLowerCase().contains(constraint.toString().toLowerCase()))
                            filteredReleases.add(r);
                    }

                    results.values = filteredReleases;
                    results.count = filteredReleases.size();

                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mReleases = (List<Release>) filterResults.values;
                updateAlphaIndex();
                notifyDataSetChanged();
            }
        };
    }

    public void updateReleases(List<Release> releases) {
        mReleases = releases;
        mUnfilteredReleases = new ArrayList<Release>(mReleases);
        updateAlphaIndex();
        notifyDataSetChanged();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }

    @Override
    public int getPositionForSection(int section) {
        return mSectionValues[section];
    }

    @Override
    public int getSectionForPosition(int pos) {
        for(int i = 0 ; i < mSectionValues.length ; i++) {
            if(pos < mSectionValues[i]) {
                return i-1;
            }
        }
        return 0;
    }
}
