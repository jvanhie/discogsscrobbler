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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class DiscogsApiActivity extends ActionBarActivity {

    public static final String ARG_API_MODE = "api_mode";

    private Discogs mDiscogs;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discogs_login);
        mWebView = (WebView) findViewById(R.id.webView);
        mDiscogs = Discogs.getInstance(this);
        //clear cookies, we don't want them -> security!
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        //set settings, js support and not storing form data for security
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSaveFormData(false);
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        mWebView.setWebViewClient(new WebViewClient() {
            private ProgressBar progressBar = (ProgressBar) findViewById(R.id.webViewProgressBar);

            @Override
            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { " +
                        "document.getElementById('site_headers_super_wrap').style.display='none'; " +
                        "})()");
                progressBar.setVisibility(View.INVISIBLE);
                super.onPageFinished(view, url);
                //autofill data
                if (url.equals("http://www.discogs.com/applications/edit")) {
                    String name = "Discogs Scrobbler";
                    String description = "A Discogs collection manager and Last.fm scrobbler. " +
                            "Source code available at https://github.com/jvanhie/discogsscrobbler";
                    mWebView.loadUrl("javascript:(function() { " +
                            "document.getElementById('name').value = '" + name + "'; " +
                            "document.getElementById('description').value = '" + description + "'; })()");
                } else if (url.contains("http://www.discogs.com/applications/edit/")) {
                    //this is a detail screen, scrape data
                    mWebView.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementById('page_content').innerHTML);");

                }


            }
        });
        if(getIntent().hasExtra(ARG_API_MODE) && getIntent().getStringExtra(ARG_API_MODE).equals("create")) {
            mWebView.loadUrl("http://www.discogs.com/applications/edit");
        } else {
            mWebView.loadUrl("http://www.discogs.com/settings/developers");
        }
    }

    class MyJavaScriptInterface
    {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void processHTML(String html)
        {
            //nasty page scraping goodness
            int keyIdx = html.indexOf("Consumer Key");
            String key = html.substring(html.indexOf("<code>",keyIdx)+6,html.indexOf("</code>",keyIdx));
            int secretIdx = html.indexOf("Consumer Secret");
            String secret = html.substring(html.indexOf("<code>",secretIdx)+6,html.indexOf("</code>",secretIdx));
            mDiscogs.setCustomApi(key,secret);
            finish();
        }
    }

}
