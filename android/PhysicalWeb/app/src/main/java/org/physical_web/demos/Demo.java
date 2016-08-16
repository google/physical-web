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
package org.physical_web.demos;

/**
 * Provides an interface for Demos to be added to the demo section.
 */
public interface Demo {

  /**
   * @return The summary of the demo.
   */
  String getSummary();

  /**
   * @return The title of the demo.
   */
  String getTitle();

  /**
   * Checks to see if the demo is running.
   * @return True if the demo has been started and not stopped.
   */
  boolean isDemoStarted();

  /**
   * Starts the demo.
   */
  void startDemo();

  /**
   * Stops the demo.
   */
  void stopDemo();
}
