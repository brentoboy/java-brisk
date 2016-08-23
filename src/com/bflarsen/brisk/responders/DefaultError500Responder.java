package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.responses.BaseResponse;
import com.bflarsen.brisk.responses.PlainTextResponse;

public class DefaultError500Responder implements HttpResponder {
    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) {
        BaseResponse response;
        if (context == null
                || context.ResponderException == null
        ) {
            response = new PlainTextResponse("Server Error");
        }
        else {
            response = new PlainTextResponse(String.format("Server Error: %s", context.ResponderException.getMessage()));
        }
        response.StatusCode = 500;
        return response;
    }
}
