package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.BaseResponse;
import com.bflarsen.brisk.responses.PlainTextResponse;

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
            response = new PlainTextResponse("Not Found");
        }
        else {
            response = new PlainTextResponse(String.format("Not Found: %s", context.Request.Path));
        }
        response.StatusCode = 404;
        return response;
    }
}
