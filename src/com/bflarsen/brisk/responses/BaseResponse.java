package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.*;

import java.io.Writer;
import java.util.*;

public abstract class BaseResponse implements HttpResponse {
    public int StatusCode = 200;
    public Map<String, String> Headers = new LinkedHashMap<>();

    public int getStatusCode() {
        return this.StatusCode;
    }

    public void send(Writer stream) throws Exception {
        this.sendHeaders(stream);
        this.sendBody(stream);
    }

    public void setHeader(String key, String value) {
        this.Headers.put(key, value);
    }

    public void sendHeaders(Writer stream) throws Exception {
        for (Map.Entry<String, String> entry : this.Headers.entrySet()) {
            stream.write(entry.getKey());
            stream.write(": ");
            stream.write(entry.getValue());
            stream.write("\n");
        }
    }

    abstract public void sendBody(Writer stream);
}
