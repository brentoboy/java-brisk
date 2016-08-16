package com.bflarsen.brisk;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public String Method;
    public String Resource;  // the entire requested resource, including the params part
    public String HttpVersion;
    public String Path;  // the part of resource up to the first ?  (the part before the params
    public String Host;  // the host name of the server that was requested
    public final Map<String, Object> Params = new HashMap<>();
    public final Map<String, String> Headers = new HashMap<>();

    public Object getParam(String... keys) {
        if (Params.containsKey(keys[0])) {
            return Params.get(keys[0]);
        }
        else {
            return null;
        }
    }

    public String getHeader(String key) {
        if (Headers.containsKey(key)) {
            return Headers.get(key);
        }
        else {
            return null;
        }
    }
}
