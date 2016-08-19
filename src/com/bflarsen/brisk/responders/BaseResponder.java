package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;

public abstract class BaseResponder implements HttpResponder {

    public abstract HttpResponse respond();
    public abstract HttpResponse respondToException(Exception ex);

    public HttpResponse handleRequest(HttpContext context) {
        try {
            return this.respond();
        }
        catch (Exception ex) {
            return this.respondToException(ex);
        }
    }
}
