package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;

public class DefaultError404Responder implements HttpResponder {
    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) {
        BaseResponse response;
        if (context == null
                || context.Request == null
                || context.Request.Path == null
        ) {
            response = new SimpleStatusResponse(HttpStatusCode.NOT_FOUND);
        }
        else {
            response = new SimpleStatusResponse(HttpStatusCode.NOT_FOUND, context.Request.Path);
        }
        return response;
    }
}
