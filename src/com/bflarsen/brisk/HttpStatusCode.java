package com.bflarsen.brisk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpStatusCode {
    public static final int SWITCHING_PROTOCOLS = 101;
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NO_CONTENT = 204;
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    public static final int NOT_MODIFIED = 304;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int PAYMENT_REQUIRED = 402;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int LENGTH_REQUIRED = 411;
    public static final int REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int REQUEST_URI_TOO_LONG = 414;
    public static final int IM_A_TEAPOT = 418;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;
    public static final int BAD_GATEWAY = 502;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION_NOT_SUPPORTED = 505;

    public static final Map<Integer, String> KnownCodes = new ConcurrentHashMap<>();
    static {
        KnownCodes.put(SWITCHING_PROTOCOLS, "Switching Protocols");
        KnownCodes.put(OK, "OK");
        KnownCodes.put(CREATED, "Created");
        KnownCodes.put(ACCEPTED, "Accepted");
        KnownCodes.put(NO_CONTENT, "No Content");
        KnownCodes.put(MOVED_PERMANENTLY, "Moved Permanently");
        KnownCodes.put(FOUND, "Found");
        KnownCodes.put(NOT_MODIFIED, "Not Modified");
        KnownCodes.put(BAD_REQUEST, "Bad Request");
        KnownCodes.put(UNAUTHORIZED, "Unauthorized");
        KnownCodes.put(PAYMENT_REQUIRED, "Payment Required");
        KnownCodes.put(FORBIDDEN, "Forbidden");
        KnownCodes.put(NOT_FOUND, "Not Found");
        KnownCodes.put(METHOD_NOT_ALLOWED, "Method Not Allowed");
        KnownCodes.put(NOT_ACCEPTABLE, "Not Acceptable");
        KnownCodes.put(REQUEST_TIMEOUT, "Request Timeout");
        KnownCodes.put(LENGTH_REQUIRED, "Length Required");
        KnownCodes.put(REQUEST_ENTITY_TOO_LARGE, "Request Entity Too Large");
        KnownCodes.put(REQUEST_URI_TOO_LONG, "Request-URI Too Long");
        KnownCodes.put(IM_A_TEAPOT, "I'm a teapot");
        KnownCodes.put(INTERNAL_SERVER_ERROR, "Internal Server Error");
        KnownCodes.put(NOT_IMPLEMENTED, "Not Implemented");
        KnownCodes.put(BAD_GATEWAY, "Bad Gateway");
        KnownCodes.put(SERVICE_UNAVAILABLE, "Service Unavailable");
        KnownCodes.put(GATEWAY_TIMEOUT, "Gateway Timeout");
        KnownCodes.put(HTTP_VERSION_NOT_SUPPORTED, "HTTP Version Not Supported");
    }

    public static String lookupDescription(int statusCode) {
        if (KnownCodes.containsKey(statusCode))
            return KnownCodes.get(statusCode);
        else
            return "Unknown Status: " + statusCode;
    }
}
