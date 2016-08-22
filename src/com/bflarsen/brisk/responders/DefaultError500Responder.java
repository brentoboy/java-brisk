package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.responses.PlainTextResponse;

public class DefaultError500Responder implements ExceptionResponder {

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse handleRequest(HttpContext context) {
        return new PlainTextResponse(context.ResponderException.getMessage());
    }

    @Override
    public HttpResponse respondToException(Exception ex, HttpContext context) {
        context.ResponderException = ex;
        return handleRequest(context);
    }
}
