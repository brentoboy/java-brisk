package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.*;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class BaseResponse implements HttpResponse {
    public int StatusCode;
    public String StatusDescription;
    public String HttpVersion;
    public Map<String, String> Headers = new LinkedHashMap<>();

    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final SimpleDateFormat UtcFormatter = new SimpleDateFormat();
    static {
        UtcFormatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        UtcFormatter.applyPattern("dd MMM yyyy HH:mm:ss z");
    }

    protected BaseResponse(int statusCode, String contentType) {
        this.HttpVersion = "HTTP/1.1";
        this.StatusCode = statusCode;
        this.setHeader("Date", UtcFormatter.format(new Date()));
        this.setHeader("Server", "Brisk/0.1.0");
        this.setHeader("Content-Type", contentType); // I'd sure like to include "; charset=UTF-8" on the end of this
        this.setHeader("Cache-Control", "max-age=0");
    }

    public int getStatusCode() {
        return this.StatusCode;
    }

    public String getStatusDescription() {
        if (this.StatusDescription == null || this.StatusDescription.isEmpty()) {
            return HttpStatusCode.lookupDescription(this.StatusCode);
        }
        else {
            return this.StatusDescription;
        }
    }

    public void setHeader(String key, String value) {
        this.Headers.put(key, value);
    }

    @Override
    public String getHttpVersion() {
        return this.HttpVersion;
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.Headers;
    }

    @Override
    public abstract Long getContentLength() throws Exception;

    @Override
    public abstract void sendBody(OutputStream stream) throws Exception;
}
