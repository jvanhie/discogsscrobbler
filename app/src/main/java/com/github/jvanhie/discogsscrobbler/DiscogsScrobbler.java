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

import com.activeandroid.app.Application;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;


/**
 * Created by Jono on 18/05/2014.
 */
public class DiscogsScrobbler extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //bugfix for dodgy discogs connections
        System.setProperty("http.keepAlive", "false");
        //init crashlytics
        Fabric.with(this, new Crashlytics());
    }

}
