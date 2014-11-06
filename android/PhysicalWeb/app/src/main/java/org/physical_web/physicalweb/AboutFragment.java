package org.physical_web.physicalweb;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.physical_web.physicalweb.BuildConfig;
import org.physical_web.physicalweb.R;

public class AboutFragment extends Fragment {

  @SuppressWarnings("WeakerAccess")
  public AboutFragment() {
  }

  public static AboutFragment newInstance() {
    return new AboutFragment();
  }

  private void initializeApplicationVersionText() {
    String versionString = getString(R.string.about_version_label) + " " + BuildConfig.VERSION_NAME;
    View view = getView();
    if (view != null) {
      TextView versionView = (TextView) view.findViewById(R.id.version);
      versionView.setText(versionString);
    }
  }

  private void initializeWebView() {
    WebView webView = (WebView) getActivity().findViewById(R.id.about_webview);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl(getString(R.string.url_getting_started));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_demo).setVisible(false);
  }

}
