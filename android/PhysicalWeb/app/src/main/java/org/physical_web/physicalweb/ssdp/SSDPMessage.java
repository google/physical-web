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