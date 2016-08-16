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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * The fragment that displays info about the app.
 */
public class BlockedFragment extends Fragment {

  private BlockedAdapter mAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    View rootView = inflater.inflate(R.layout.fragment_blocked, container, false);
    ListView listView = (ListView) rootView.findViewById(android.R.id.list);
    mAdapter = new BlockedAdapter();
    listView.setAdapter(mAdapter);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    mAdapter.clear();
    for (String s : Utils.getBlockedHosts()) {
        mAdapter.addItem(s);
    }
    mAdapter.notifyDataSetChanged();
    getActivity().getActionBar().setTitle("Blocked URLs");
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    Utils.hideAllMenuItems(menu);
  }

  private class BlockedAdapter extends BaseAdapter {
    private List<String> mListItems;

    BlockedAdapter() {
      mListItems = new ArrayList<>();
    }

    public void addItem(String pwPair) {
      mListItems.add(pwPair);
    }

    @Override
    public int getCount() {
      return mListItems.size();
    }

    @Override
    public String getItem(int i) {
      return mListItems.get(i);
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
      setText(view, R.id.blocked_host, getItem(i));
      ((Button) view.findViewById(R.id.delete_button)).setOnClickListener(
          new View.OnClickListener() {
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
      mListItems.clear();
      notifyDataSetChanged();
    }
  }
}

