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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.jvanhie.discogsscrobbler.util.Discogs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class DiscogsLoginActivity extends ActionBarActivity {

    private Discogs mDiscogs;
    private WebView mWebView;
    private Context mContext;

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
        new DiscogsAuthTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.discogs_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    private class DiscogsAuthTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            return mDiscogs.getOAuthURL();
        }

        @Override
        protected void onPostExecute(String authURL) {
            if(authURL == null) {
                //the discogs oauth service was down, stop activity
                Toast.makeText(DiscogsLoginActivity.this, getString(R.string.oauth_error), Toast.LENGTH_SHORT).show();
                finish();
            }
            mWebView.setWebViewClient(new WebViewClient() {
                private ProgressBar progressBar = (ProgressBar)findViewById(R.id.webViewProgressBar);

                @Override
                public void onPageFinished(WebView view, String url)
                {
                    mWebView.loadUrl("javascript:(function() { " +
                            "document.getElementById('site_headers_super_wrap').style.display='none'; " +
                            "})()");
                    progressBar.setVisibility(View.INVISIBLE);
                    super.onPageFinished(view, url);

                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);

                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    super.shouldOverrideUrlLoading(view, url);

                    if (url.startsWith("oauth")) {
                        final String url1 = url;
                        Thread t1 = new Thread() {
                            public void run() {
                                //decode URL in case it contains + ? = htmlencoded
                                String decodedUrl = url1;
                                try {
                                    decodedUrl = URLDecoder.decode(decodedUrl, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                Uri uri = Uri.parse(decodedUrl);
                                if(uri.getQueryParameter("denied") != null) {
                                    //user has cancelled the auth, finish activity
                                    finish();
                                }
                                String verifier = uri.getQueryParameter("oauth_verifier");
                                mDiscogs.setAccessToken(verifier);

                            }
                        };
                        t1.start();
                        return true;
                    }

                    return false;
                }
            });
            mWebView.loadUrl(authURL);
        }
    }
}
