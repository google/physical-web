package physical_web.org.physicalweb;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

  private void initializeLearnMoreButton() {
    Button button = (Button) getActivity().findViewById(R.id.button_learn_more);
    // This listens for the taps the learn-more button
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showLearnMorePage();
      }
    });
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.fragment_about, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    //noinspection ConstantConditions
    getActivity().getActionBar().setTitle(R.string.title_about);
    initializeApplicationVersionText();
    initializeLearnMoreButton();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_demo).setVisible(false);
  }

  private void showLearnMorePage() {
    String url = getString(R.string.url_learn_more);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

}
