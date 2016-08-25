package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responders.BaseResponder;
import com.bflarsen.brisk.responses.PlainTextResponse;

public class SpecialPageResponder extends BaseResponder {
    @Override
    public HttpResponse buildResponse() throws Exception {
        return new PlainTextResponse("Isn't this Special");
    }
}

