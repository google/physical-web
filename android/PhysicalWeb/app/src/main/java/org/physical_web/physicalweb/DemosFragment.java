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

import org.physical_web.demos.Demo;
import org.physical_web.demos.FatBeaconHelloWorld;
import org.physical_web.demos.WifiDirectHelloWorld;

import android.app.ListFragment;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * This class shows a list of demos.
 */
public class DemosFragment extends ListFragment {
  private static final String TAG = DemosFragment.class.getSimpleName();
  private DemosAdapter mAdapter;

  @Override
  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
      Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    View rootView = layoutInflater.inflate(R.layout.fragment_demos, container, false);
    mAdapter = new DemosAdapter();
    setListAdapter(mAdapter);
    initialize();
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.demos);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    Utils.hideAllMenuItems(menu);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Demo demo = mAdapter.getItem(position);
    if (demo.isDemoStarted()) {
      demo.stopDemo();
    } else {
      demo.startDemo();
    }
    setBackgroundColor(v, demo.isDemoStarted());
  }

  private void initialize() {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      mAdapter.addItem(new FatBeaconHelloWorld(getActivity()));
    }
    mAdapter.addItem(new WifiDirectHelloWorld(getActivity()));
  }

  private void setBackgroundColor(View view, boolean isStarted) {
    if (isStarted) {
      view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.physical_web_logo));
    } else {
      view.setBackgroundColor(0xFFFFFFFF);
    }
  }

  private class DemosAdapter extends BaseAdapter {
    List<Demo> demos;

    public DemosAdapter() {
      demos = new ArrayList<>();
    }

    public void addItem(Demo demo) {
      demos.add(demo);
    }

    @Override
    public int getCount() {
      return demos.size();
    }

    @Override
    public Demo getItem(int i) {
      return demos.get(i);
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_demo, viewGroup, false);
      }
      ((TextView) view.findViewById(R.id.demo_title)).setText(demos.get(i).getTitle());
      ((TextView) view.findViewById(R.id.demo_summary)).setText(demos.get(i).getSummary());
      setBackgroundColor(view, demos.get(i).isDemoStarted());
      return view;
    }
  }
}
