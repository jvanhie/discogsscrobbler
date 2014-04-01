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

import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.download.HttpClientImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

/**
 * Created by Jono on 25/03/2014.
 */
public class DiscogsImageDownloader extends ImageDownloader {

    Discogs mDiscogs;

    public DiscogsImageDownloader(Discogs discogs) {
        super();
        mDiscogs = discogs;

    }


    @Override
    protected InputStream getStreamFromNetwork(URI imageUri) throws IOException {
        //slight hack to remove the api root of the url
        String image = imageUri.toString().replace("http://api.discogs.com/","");
        return mDiscogs.getImage(image);
    }

}
