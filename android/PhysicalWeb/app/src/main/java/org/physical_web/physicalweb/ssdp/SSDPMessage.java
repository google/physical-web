/*
 * Copyright 2014 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AUTHOR: Louay Bassbouss <louay.bassbouss@fokus.fraunhofer.de>
 *
 */
package org.physical_web.physicalweb.ssdp;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lba on 29.01.2015.
 */
public class SsdpMessage {
    public static int TYPE_SEARCH = 0;
    public static int TYPE_NOTIFY = 1;
    public static int TYPE_FOUND = 2;
    private static String FIRST_LINE[] = {Ssdp.TYPE_M_SEARCH+" * HTTP/1.1", Ssdp.TYPE_NOTIFY+" * HTTP/1.1","HTTP/1.1 "+ Ssdp.TYPE_200_OK};
    private int type;
    private Map<String, String> headers;

    public SsdpMessage(int type){
        this.type = type;
    }
    public SsdpMessage(String txt){
        String lines[] = txt.split("\r\n");
        String line = lines[0].trim();
        if(line.startsWith(Ssdp.TYPE_M_SEARCH)){
            this.type = TYPE_SEARCH;
        }
        else if (line.startsWith(Ssdp.TYPE_NOTIFY)){
            this.type = TYPE_NOTIFY;
        }
        else {
            this.type = TYPE_FOUND;
        }
        for (int i = 1; i< lines.length;i++){
            line = lines[i].trim();
            int index = line.indexOf(":");
            if (index>0){
                String key = line.substring(0,index).trim();
                String value = line.substring(index+1).trim();
                getHeaders().put(key,value);
            }
        }
    }

    public Map<String, String> getHeaders() {
        if(headers == null){
            headers = new HashMap<>();
        }
        return headers;
    }

    public int getType() {
        return type;
    }
    
    public String get(String key){
        return getHeaders().get(key);
    }
    
    public String put(String key, String value){
        return getHeaders().put(key,value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(FIRST_LINE[this.type]).append("\r\n");
        for (Map.Entry<String,String> entry: this.headers.entrySet()){
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        builder.append("\r\n");
        return builder.toString();
    }
}