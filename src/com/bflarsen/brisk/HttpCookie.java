package com.bflarsen.brisk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HttpCookie {
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
    { dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT")); }

    public String Name; // directs browsers to send a cookie header for page requests, passing the server "Name: Value"
    public String Value;
    public String Path = "/"; // directs browsers to only send this cookie if they are requesting a page "under" this path... like "/admin"
    public String Domain; // directs browsers to send this cookie to "*.Domain", set it to ".my.com" to send to any subdomain of  "my.com"
    public Long Expires; // if set, it will expire at this time
    public boolean Secure; // attribute is meant to keep cookie communication limited to encrypted transmission, directing browsers to use cookies only via secure/encrypted connections
    public boolean HttpOnly; // directs browsers not to expose cookies through channels other than HTTP (and HTTPS) requests. This means that the cookie cannot be accessed via client-side scripting languages (notably JavaScript)

    public HttpCookie() {}

    public HttpCookie(String name, String value) { this.Name = name; this.Value = value; }

    public String getResponseLine() {
        StringBuilder builder = new StringBuilder();
        builder.append("Set-Cookie: ").append(Name).append("=").append(Value);
        if (Path != null && !Path.isEmpty()) {
            builder.append("; Path=").append(Path);
        }
        if (Domain != null && !Domain.isEmpty()) {
            builder.append("; Domain=").append(Domain);
        }
        if (Expires != null) {
            Date dt = new Date();
            dt.setTime(Expires);
            builder.append("; Expires=").append(dateFormatter.format(dt));
        }
        if (Secure) {
            builder.append("; Secure");
        }
        if (HttpOnly) {
            builder.append("; HttpOnly");
        }
        return builder.toString();
    }
}
