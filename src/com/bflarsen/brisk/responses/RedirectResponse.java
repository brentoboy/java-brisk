package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.HttpStatusCode;

public class RedirectResponse extends SimpleStatusResponse {

    public RedirectResponse(String location) {
        super(HttpStatusCode.MOVED_PERMANENTLY, "<a href='"+location+"'>"+location+"</a>");
        setHeader("Location", location);
    }

}
