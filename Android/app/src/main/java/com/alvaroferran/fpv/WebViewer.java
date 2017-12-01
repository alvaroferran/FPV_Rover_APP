package com.alvaroferran.fpv;

import android.webkit.WebViewClient;

/**
 * Created by alvaroferran on 23/11/17.
 */
 import android.webkit.WebView;

public class WebViewer extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }
}