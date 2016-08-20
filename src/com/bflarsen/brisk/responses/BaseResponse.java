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

    protected BaseResponse(int statusCode) {
        this.HttpVersion = "HTTP/1.1";
        this.StatusCode = statusCode;
        this.setHeader("Date", UtcFormatter.format(new Date()));
        this.setHeader("Server", "Brisk/0.1.0");
        this.setHeader("Content-Type", "text/html; charset=UTF-8");
        this.setHeader("Cache-Control", "max-age=0");
    }

    public int getStatusCode() {
        return this.StatusCode;
    }

    public String getStatusDescription() {
        if (this.StatusDescription == null || this.StatusDescription.isEmpty()) {
            return lookupStatusDescription(this.StatusCode);
        }
        else {
            return this.StatusDescription;
        }
    }

    public static String lookupStatusDescription(int statusCode) {
        switch(statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 408: return "Request Timeout";
            case 411: return "Length Required";
            case 413: return "Request Entity Too Large";
            case 414: return "Request-URI Too Long";
            case 418: return "I'm a teapot";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            default: return "Unknown Status";
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
    public boolean hasBodySender() {
        return false;  // if you override sendBody, override this too, and return true
    }

    @Override
    public Long getContentLength() {
        return null; // if you override sendBody, you should override this
    }

    @Override
    public void sendBody(OutputStream stream) throws Exception {
        throw new Exception("Send Body Not Implemented for class: " + this.getClass().getName());
    }
}
