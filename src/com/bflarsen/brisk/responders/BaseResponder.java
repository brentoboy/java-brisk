package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;

public abstract class BaseResponder implements HttpResponder {

    public abstract HttpResponse buildResponse() throws Exception;

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) throws Exception {
        context.Server.AutoConverter.fill(this, context.Request.Params);
        context.Server.AutoConverter.fill(this, context.Session.Params);
        context.Server.AutoConverter.fill(this, context.WorkerThreadResources);
        return this.buildResponse();
    }
}
