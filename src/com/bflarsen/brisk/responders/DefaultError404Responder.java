package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.responses.NullResponse;

public class DefaultError404Responder implements HttpResponder {
    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse handleRequest(HttpContext context) {
        return new NullResponse();
    }
}
