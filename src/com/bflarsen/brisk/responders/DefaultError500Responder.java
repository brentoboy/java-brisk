package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;

public class DefaultError500Responder implements HttpResponder {
    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) {
        if (context == null || context.ResponderException == null || !context.Server.DisplayErrorDetails) {
            return new SimpleStatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        else {
            return new SimpleStatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, context.ResponderException.getMessage());
        }
    }
}
