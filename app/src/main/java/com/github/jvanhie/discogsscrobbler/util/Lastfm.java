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
import android.content.res.Resources;

import com.github.jvanhie.discogsscrobbler.R;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;

/**
 * Created by Jono on 03/04/2014.
 */
public class Lastfm extends ContextWrapper {

    private Caller mLastfm;

    private final String API_KEY;
    private final String API_SECRET;
    private String mToken;
    private Session mSession;



    public Lastfm(Context context) {
        super(context);
        Resources res = context.getResources();
        API_KEY = res.getString(R.string.discogs_api_key);
        API_SECRET = res.getString(R.string.discogs_api_secret);
        String ua = res.getString(R.string.user_agent);

        mLastfm = Caller.getInstance();
        mLastfm.setUserAgent(ua);
        mLastfm.setDebugMode(true);
    }

    public void authenticate() {
        mToken = Authenticator.getToken(API_KEY);
        //TODO: call last.fm auth intent with key and token
    }

    public void getSession() {
        mSession = Authenticator.getSession(mToken,API_KEY,API_SECRET);
    }

}
