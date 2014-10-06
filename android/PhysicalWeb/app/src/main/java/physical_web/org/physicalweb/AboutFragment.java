package physical_web.org.physicalweb;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
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
  }

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
    getActivity().getActionBar().setTitle(getString(R.string.title_nearby_beacons));
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
  }

}
