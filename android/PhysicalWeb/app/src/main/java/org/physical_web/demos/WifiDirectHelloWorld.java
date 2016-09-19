package org.physical_web.demos;

import org.physical_web.physicalweb.FileBroadcastService;
import org.physical_web.physicalweb.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

/**
 * Hello world demo for WifiDirect.
 */
public class WifiDirectHelloWorld implements Demo {
  private static final String TAG = WifiDirectHelloWorld.class.getSimpleName();
  private static boolean mIsDemoStarted = false;
  private final Context mContext;

  public WifiDirectHelloWorld(Context context) {
    mContext = context;
  }

  @Override
  public String getSummary() {
    return mContext.getString(R.string.wifi_direct_demo_summary);
  }

  @Override
  public String getTitle() {
    return mContext.getString(R.string.wifi_direct_demo_title);
  }

  @Override
  public boolean isDemoStarted() {
    return mIsDemoStarted;
  }

  @Override
  public void startDemo() {
    Intent intent = new Intent(mContext, FileBroadcastService.class);
    String uriString = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
        mContext.getPackageName() + "/" + R.raw.wifi_direct_default_webpage;
    intent.putExtra(FileBroadcastService.FILE_KEY, uriString);
    intent.putExtra(FileBroadcastService.MIME_TYPE_KEY, "text/html");
    intent.putExtra(FileBroadcastService.TITLE_KEY, "Hello World");
    mContext.startService(intent);
    mIsDemoStarted = true;
  }

  @Override
  public void stopDemo() {
    mContext.stopService(new Intent(mContext, FileBroadcastService.class));
    mIsDemoStarted = false;
  }
}
