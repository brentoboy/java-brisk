package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;

public abstract class BaseResponder implements HttpResponder {

    public abstract HttpResponse respond();
    private HttpContext context;

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    public HttpResponse respondToException(Exception ex) {
        ExceptionResponder errorResponder = context.Server.Error500ResponderFactory.create();
        return errorResponder.respondToException(ex, context);
    }

    public HttpResponse handleRequest(HttpContext context) {
        this.context = context;
        try {
            context.Server.AutoConverter.fill(this, context.Request.Params);
            return this.respond();
        }
        catch (Exception ex) {
            return this.respondToException(ex);
        }
    }
}
