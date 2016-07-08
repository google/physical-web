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
import android.widget.ListView;

import android.widget.ArrayAdapter;
import java.util.ArrayList;
import android.widget.BaseAdapter;
import android.widget.Button;

/**
 * The fragment that displays info about the app.
 */
public class BlockedFragment extends Fragment {

  ArrayList<String> listItems=new ArrayList<String>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
  BlockedAdapter adapter;

  @SuppressWarnings("WeakerAccess")
  public BlockedFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    View rootView = inflater.inflate(R.layout.fragment_nearby_beacons, container, false);
    ListView listView = (ListView) rootView.findViewById(android.R.id.list);
    adapter=new BlockedAdapter();
    listView.setAdapter(adapter);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    adapter.clear();
    for (String s : Utils.mBlockedUrls) {
        adapter.addItem(s);
    }
    adapter.notifyDataSetChanged();
    //noinspection ConstantConditions
    getActivity().getActionBar().setTitle("Blocked URLs");
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_settings).setVisible(false);
    menu.findItem(R.id.block_settings).setVisible(false);
  }

  private class BlockedAdapter extends BaseAdapter {
    private ArrayList<String> listItems;

    BlockedAdapter() {
      listItems = new ArrayList<>();
    }

    public void addItem(String pwPair) {
      listItems.add(pwPair);
    }

    @Override
    public int getCount() {
      return listItems.size();
    }

    @Override
    public String getItem(int i) {
      return listItems.get(i);
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    private void setText(View view, int textViewId, String text) {
      ((TextView) view.findViewById(textViewId)).setText(text);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Get the list view item for the given position
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_blocked,
                                                         viewGroup, false);
      }
      final String t = getItem(i);
      setText(view, R.id.text1, getItem(i));
      ((Button) view.findViewById(R.id.delete_button)).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Utils.removeBlocked(t);
          Utils.saveBlocked(getActivity());
          clear();
          onResume();
          notifyDataSetChanged();
        }
      });
      return view;
    }

    public void clear() {
      listItems.clear();
      notifyDataSetChanged();
    }

  }

}
