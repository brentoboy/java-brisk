package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpResponder;

public class DefaultError404Responder implements HttpResponder {
    @Override
    public HttpResponse handleRequest(HttpContext context) {
        return null;
    }
}
