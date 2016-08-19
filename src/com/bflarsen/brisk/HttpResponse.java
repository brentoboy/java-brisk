package com.bflarsen.brisk;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public interface HttpResponse {
    void setHeader(String key, String value);

    String getHttpVersion();
    int getStatusCode();
    String getStatusDescription();
    Map<String, String> getHeaders();
    byte[] getBodyBytes() throws Exception;

    boolean hasBodySender();
    Long getContentLength();
    void sendBody(OutputStream stream) throws Exception;
}
