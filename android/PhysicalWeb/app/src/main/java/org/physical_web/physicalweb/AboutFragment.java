/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.physical_web.physicalweb;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

/**
 * The fragment that displays info about the app.
 */
public class AboutFragment extends Fragment {

  @SuppressWarnings("WeakerAccess")
  public AboutFragment() {
  }

  private void initializeApplicationVersionText() {
    String versionString = getString(R.string.about_version_label) + " " + BuildConfig.VERSION_NAME;
    View view = getView();
    if (view != null) {
      TextView versionView = (TextView) view.findViewById(R.id.version);
      versionView.setText(versionString);
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void initializeWebView() {
    WebView webView = (WebView) getActivity().findViewById(R.id.about_webview);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl(getString(R.string.url_getting_started));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    return inflater.inflate(R.layout.fragment_about, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    //noinspection ConstantConditions
    getActivity().getActionBar().setTitle(R.string.title_about);
    initializeWebView();
    initializeApplicationVersionText();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    Utils.hideAllMenuItems(menu);
  }

}
