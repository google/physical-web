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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
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

  private static final String TAG = SettingsFragment.class.getSimpleName();
  private static final int MAX_ENDPOINT_OPTIONS = 3;
  private PreferenceGroup mCustomEndpointCategory;

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
    mCustomEndpointCategory =
        (PreferenceGroup) findPreference(getString(R.string.custom_pws_endpoint_key));
    updatePwsPreference();
    updateAllSettingSummaries();
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
    Utils.hideAllMenuItems(menu);
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
    Utils.startScan(getActivity());
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateSettingSummary(key);
    if (key.equals(getString(R.string.custom_pws_url_key)) ||
        key.equals(getString(R.string.custom_pws_version_key)) ||
        key.equals(getString(R.string.custom_pws_api_key_key))) {
      updatePwsPreference();
      updatePwsList();
    } else if (key.equals(getString(R.string.pws_endpoint_setting_key))) {
      updatePwsPreference();
      Utils.deleteCache(getActivity());
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
    endpoints.add(Utils.formatEndpointForSharedPrefernces(Utils.PROD_ENDPOINT,
                                                          Utils.PROD_ENDPOINT_VERSION, ""));
    endpoints.add(Utils.formatEndpointForSharedPrefernces(Utils.DEV_ENDPOINT,
                                                          Utils.DEV_ENDPOINT_VERSION, ""));
    listPreference.setEntries(endpointNames.toArray(new CharSequence[endpointNames.size()]));
    listPreference.setEntryValues(endpoints.toArray(new CharSequence[endpoints.size()]));
  }

  private void updatePwsPreference() {
    ListPreference listPreference = (ListPreference) findPreference(
        getString(R.string.pws_endpoint_setting_key));
    String entry = (String) listPreference.getEntry();
    if (entry == null) {
      return;
    }

    if (entry.equals(getString(R.string.custom_pws))) {
      // User selected custom PWS therefore need to update it accordingly
      EditTextPreference customPwsUrlPreference =
          (EditTextPreference) mCustomEndpointCategory.findPreference(
              getString(R.string.custom_pws_url_key));
      ListPreference customPwsVersionPreference =
          (ListPreference) mCustomEndpointCategory.findPreference(
              getString(R.string.custom_pws_version_key));
      EditTextPreference customPwsApiKeyPreference =
          (EditTextPreference) mCustomEndpointCategory.findPreference(
              getString(R.string.custom_pws_api_key_key));
      String customPwsUrl = customPwsUrlPreference.getText();
      int customPwsVersion = Integer.parseInt(customPwsVersionPreference.getValue());
      String customPwsApiKey = customPwsApiKeyPreference.getText();
      customPwsUrl = customPwsUrl == null ? "" : customPwsUrl;
      customPwsApiKey = customPwsApiKey == null ? "" : customPwsApiKey;
      listPreference.setValue(Utils.formatEndpointForSharedPrefernces(customPwsUrl,
          customPwsVersion, customPwsApiKey));
      getPreferenceScreen().addPreference(mCustomEndpointCategory);
    } else {
      getPreferenceScreen().removePreference(mCustomEndpointCategory);
    }
  }

  private void updateSettingSummary(String key) {
    Preference pref = findPreference(key);
    if (pref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) pref;
      listPref.setSummary(listPref.getEntry());
    } else if (pref instanceof EditTextPreference) {
      EditTextPreference editTextPref = (EditTextPreference) pref;
      editTextPref.setSummary(editTextPref.getText());
    }
  }

  private void updateAllSettingSummaries() {
    for (String key : PreferenceManager.getDefaultSharedPreferences(getActivity())
        .getAll().keySet()) {
      updateSettingSummary(key);
    }
  }
}
