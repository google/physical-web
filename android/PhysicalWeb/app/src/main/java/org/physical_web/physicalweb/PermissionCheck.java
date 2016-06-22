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
 * Stores states of permission checking so that
 * components can access it.
 */
public class PermissionCheck {
  private static PermissionCheck instance = null;
  public boolean mCheckingPermissions;

  protected PermissionCheck() {
  }

  public static PermissionCheck getInstance() {
    if (instance == null) {
      instance = new PermissionCheck();
    }
    return instance;
  }

  public void setCheckingPermissions(boolean value) {
    mCheckingPermissions = value;
  }

  public boolean isCheckingPermissions() {
    return mCheckingPermissions;
  }
}
