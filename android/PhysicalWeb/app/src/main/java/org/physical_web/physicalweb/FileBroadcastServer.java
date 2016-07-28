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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import fi.iki.elonen.NanoHTTPD;

/**
 * HTTP Server to serve.
 **/
public class FileBroadcastServer extends NanoHTTPD {
  String mType;
  byte[] mFile;

  public FileBroadcastServer(int port, String type, byte[] file) {
    super(port);
    mType = type;
    mFile = file;
  }

  @Override
  public NanoHTTPD.Response serve(IHTTPSession session) {
    InputStream fis = new ByteArrayInputStream(mFile);
    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
        mType, fis, mFile.length);
  }
}
