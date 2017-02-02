package com.bflarsen.brisk;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpSession {
    public final String UniqueID;
    public long Expires; // Millis since Epoch
    public final Map<String, Object> Params;

    public HttpSession(String uuid) {
        UniqueID = uuid;
        Params = new LinkedHashMap<>();
    }
}
