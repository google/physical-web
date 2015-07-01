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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Subclass of {@link android.support.v4.widget.SwipeRefreshLayout} that supports containing a
 * ViewGroup whose first child is a ListView. The ViewGroup can contain other views.
 */
public class SwipeRefreshWidget extends android.support.v4.widget.SwipeRefreshLayout {

  public SwipeRefreshWidget(Context context) {
    super(context);
  }

  public SwipeRefreshWidget(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
  }

  @Override
  public boolean canChildScrollUp() {
    // The real child maps cares about is the list, so check if that can scroll.
    ListView target = (ListView) findViewById(android.R.id.list);
    return target.getChildCount() > 0
        && (target.getFirstVisiblePosition() > 0
        || target.getChildAt(0).getTop() < target.getPaddingTop());
  }
}
