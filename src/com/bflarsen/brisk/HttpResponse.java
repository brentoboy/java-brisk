package com.bflarsen.brisk;

import java.io.Writer;

public interface HttpResponse {
    int getStatusCode();
    void setHeader(String key, String value);
    void send(Writer stream) throws Exception;
}
