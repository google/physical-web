/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * This fragment is the ui that the user sees when
 * they have entered the app's settings mode.
 * This ui is where the user can view the current configuration
 * of the application (including it's default PWS client)
 * and also allows the user to change the app's behavior.
 */

public class SettingsFragment extends PreferenceFragment
                              implements OnSharedPreferenceChangeListener {

  private static final String TAG = "SettingsFragment";
  private static final int MAX_ENDPOINT_OPTIONS = 3;

  public SettingsFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.settings);
    updatePwsList();
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.title_settings);
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_settings).setVisible(false);
  }

  @Override
  public void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    if (!Utils.isCurrentPwsSelectionValid(getActivity())) {
      Utils.setPwsEndpointPreference(getActivity(),
                                     Utils.getDefaultPwsEndpointPreferenceString(getActivity()));
      Toast.makeText(getActivity(), R.string.error_pws_endpoint_not_configured_properly,
                     Toast.LENGTH_SHORT).show();
    }


  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(getString(R.string.custom_pws_url_key)) ||
        key.equals(getString(R.string.custom_pws_version_key)) ||
        key.equals(getString(R.string.custom_pws_api_key_key))) {
      updatePwsPreference();
      updatePwsList();
    }
  }

  private void updatePwsList() {
    ListPreference listPreference = (ListPreference) findPreference(
        getString(R.string.pws_endpoint_setting_key));
    ArrayList<CharSequence> endpointNames = new ArrayList<>(MAX_ENDPOINT_OPTIONS);
    ArrayList<CharSequence> endpoints = new ArrayList<>(MAX_ENDPOINT_OPTIONS);
    if (Utils.isGoogleApiKeyAvailable(getActivity())) {
      endpointNames.add(getString(R.string.google_pws));
      endpoints.add(Utils.formatEndpointForSharedPrefernces(Utils.GOOGLE_ENDPOINT,
                                                            Utils.GOOGLE_ENDPOINT_VERSION,
                                                            Utils.getGoogleApiKey(getActivity())));
    }
    endpointNames.add(getString(R.string.custom_pws));
    endpoints.add(Utils.getCustomPwsEndpoint(getActivity()));
    endpointNames.add(getString(R.string.prod_pws));
    endpointNames.add(getString(R.string.dev_pws));
    endpoints.add(Utils.formatEndpointForSharedPrefernces(Utils.DEV_ENDPOINT,
                                                          Utils.DEV_ENDPOINT_VERSION, ""));
    endpoints.add(Utils.formatEndpointForSharedPrefernces(Utils.PROD_ENDPOINT,
                                                          Utils.PROD_ENDPOINT_VERSION, ""));
    listPreference.setEntries(endpointNames.toArray(new CharSequence[endpointNames.size()]));
    listPreference.setEntryValues(endpoints.toArray(new CharSequence[endpoints.size()]));
  }

  private void updatePwsPreference() {
    ListPreference listPreference = (ListPreference) findPreference(
        getString(R.string.pws_endpoint_setting_key));
    String entry = (String) listPreference.getEntry();
    if (entry != null && entry.equals(getString(R.string.custom_pws))) {
      // User selected custom PWS therefore need to update it accordingly
      EditTextPreference customPwsUrlPreference = (EditTextPreference) findPreference(
        getString(R.string.custom_pws_url_key));
      ListPreference customPwsVersionPreference = (ListPreference) findPreference(
        getString(R.string.custom_pws_version_key));
      EditTextPreference customPwsApiKeyPreference = (EditTextPreference) findPreference(
        getString(R.string.custom_pws_api_key_key));
      String customPwsUrl = customPwsUrlPreference.getText();
      int customPwsVersion = Integer.parseInt(customPwsVersionPreference.getValue());
      String customPwsApiKey = customPwsApiKeyPreference.getText();
      customPwsUrl = customPwsUrl == null ? "" : customPwsUrl;
      customPwsApiKey = customPwsApiKey == null ? "" : customPwsApiKey;
      listPreference.setValue(Utils.formatEndpointForSharedPrefernces(customPwsUrl,
          customPwsVersion, customPwsApiKey));
    }
  }
}
