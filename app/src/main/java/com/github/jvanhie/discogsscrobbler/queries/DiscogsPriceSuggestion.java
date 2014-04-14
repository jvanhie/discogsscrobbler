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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jono on 15/04/2014.
 */
public class DiscogsPriceSuggestion {

    private List<Quality> mSuggestion;

    @SerializedName("Very Good (VG)")
    public Quality veryGood;
    @SerializedName("Good Plus (G+)")
    public Quality goodPlus;
    @SerializedName("Near Mint (NM or M-)")
    public Quality nearMint;
    @SerializedName("Good (G)")
    public Quality good;
    @SerializedName("Very Good Plus (VG+)")
    public Quality veryGoodPlus;
    @SerializedName("Mint (M)")
    public Quality mint;
    @SerializedName("Fair (F)")
    public Quality fair;
    @SerializedName("Poor (P)")
    public Quality poor;

    public List<Quality> getSuggestion() {
        if(mSuggestion==null) {
            //initialize an ordered list -- boring type work ensues - long live sublime text
            mSuggestion = new ArrayList<Quality>();
            if(poor!=null){
                poor.type="P";
                mSuggestion.add(poor);
            }
            if(fair!=null){
                fair.type="F";
                mSuggestion.add(fair);
            }
            if(good!=null){
                good.type="G";
                mSuggestion.add(good);
            }
            if(goodPlus!=null){
                goodPlus.type="G+";
                mSuggestion.add(goodPlus);
            }
            if(veryGood!=null){
                veryGood.type="VG";
                mSuggestion.add(veryGood);
            }
            if(veryGoodPlus!=null){
                veryGoodPlus.type="VG+";
                mSuggestion.add(veryGoodPlus);
            }
            if(nearMint!=null){
                nearMint.type="NM";
                mSuggestion.add(nearMint);
            }
            if(mint!=null){
                mint.type="M";
                mSuggestion.add(mint);
            }
        }
        return mSuggestion;
    }

    public class Quality {
        public String currency;
        public String type;
        public float value;
    }
}
