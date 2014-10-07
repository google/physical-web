package physical_web.org.physicalweb;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class AboutFragment extends Fragment {

  private static String TAG = "AboutFragment";

  public AboutFragment() {
  }

  public static AboutFragment newInstance() {
    AboutFragment aboutFragment = new AboutFragment();
    return aboutFragment;
  }

  private void initialize() {
    getActivity().getActionBar().setTitle(getString(R.string.title_about));
    initializeApplicationVersionText();
    initializeLearnMoreButton();
  }

  /*
  private void initializeApplicationVersionText() {
    String versionString = "";
    String versionName = "";
    try {
      PackageInfo pInfo;
      pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
      versionName = pInfo.versionName;
      versionString = getString(R.string.about_version_label) + " " + versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    TextView textView_applicationVersion = (TextView) getView().findViewById(R.id.textView_applicationVersion);
    textView_applicationVersion.setText(versionString);
  }
  */

  private void initializeApplicationVersionText() {
    String versionString;
    String versionName = null;
    try {
      Context context = getActivity();
      versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    versionString = getString(R.string.about_version_label) + " " + versionName;
    TextView textView_applicationVersion = (TextView) getView().findViewById(R.id.textView_applicationVersion);
    textView_applicationVersion.setText(versionString);
  }

  private void initializeLearnMoreButton() {
    Button button = (Button) getView().findViewById(R.id.button_learnMore);
    button.setOnClickListener(learnMoreButtonOnClickListener);
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_about, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    initialize();
  }

  @Override
  public void onDetach() {
    super.onDestroy();
    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
  }

  /**
   * This is the class that listens
   * for when the user taps the learn-more button
   */
  private View.OnClickListener learnMoreButtonOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      showLearnMorePage();
    }
  };

  private void showLearnMorePage() {
    String url = getString(R.string.url_learn_more);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

}
