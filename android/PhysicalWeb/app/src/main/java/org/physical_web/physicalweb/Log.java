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

/**
 * Convenience Log class.
 */
public class Log {
  private static final String TAG = "PW-";

  public static void d(String tag, String msg) {
    android.util.Log.d(TAG + tag, msg);
  }

  public static void d(String tag, String msg, Throwable tr) {
    android.util.Log.d(TAG + tag, msg, tr);
  }

  public static void e(String tag, String msg) {
    android.util.Log.e(TAG + tag, msg);
  }

  public static void e(String tag, String msg, Throwable tr) {
    android.util.Log.e(TAG + tag, msg, tr);
  }

  public static String getStackTraceString(Throwable tr) {
    return android.util.Log.getStackTraceString(tr);
  }

  public static void i(String tag, String msg) {
    android.util.Log.i(TAG + tag, msg);
  }

  public static void i(String tag, String msg, Throwable tr) {
    android.util.Log.i(TAG + tag, msg, tr);
  }

  public static boolean isLoggable(String tag, int i) {
    return android.util.Log.isLoggable(TAG + tag, i);
  }

  public static void println(int priority, String tag, String msg) {
    android.util.Log.println(priority, TAG + tag, msg);
  }

  public static void v(String tag, String msg) {
    android.util.Log.v(TAG + tag, msg);
  }

  public static void v(String tag, String msg, Throwable tr) {
    android.util.Log.v(TAG + tag, msg, tr);
  }

  public static void w(String tag, String msg) {
    android.util.Log.w(TAG + tag, msg);
  }

  public static void w(String tag, String msg, Throwable tr) {
    android.util.Log.w(TAG + tag, msg, tr);
  }

  public static void wtf(String tag, String msg) {
    android.util.Log.wtf(TAG + tag, msg);
  }

  public static void wtf(String tag, String msg, Throwable tr) {
    android.util.Log.wtf(TAG + tag, msg, tr);
  }

  public static void wtf(String tag, Throwable tr) {
    android.util.Log.wtf(TAG + tag, tr);
  }
}
