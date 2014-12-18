package org.physical_web.physicalweb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OobActivity extends ActionBarActivity {

  private boolean mUserOptedIn;
  View.OnClickListener mAcceptButtonOnClickListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      mUserOptedIn = true;
      finish();
    }
  };

  private void initializeWebView() {
    WebView webView = (WebView) findViewById(R.id.oob_webview);
    // Force the background color to update (it uses the color specified in the layout xml)
    webView.setBackgroundColor(0x00000000);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
    webView.setWebViewClient(new WebViewClient());
    webView.loadUrl(getString(R.string.url_getting_started));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_oob);
    mUserOptedIn = false;
    findViewById(R.id.oob_accept_button).setOnClickListener(mAcceptButtonOnClickListener);
    initializeWebView();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_oob, menu);
    return true;
  }

  @Override
  public void finish() {
    // Return the result to the calling activity
    Intent intent = new Intent();
    intent.putExtra("userOptedIn", mUserOptedIn);
    setResult(RESULT_OK, intent);
    super.finish();
  }
}