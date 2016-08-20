package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responders.BaseHtmlResponder;
import com.bflarsen.brisk.responders.BaseResponder;
import com.bflarsen.brisk.responses.PlainTextResponse;

public class IndexPageResponder extends BaseResponder {


    @Override
    public HttpResponse respond() {
        return new PlainTextResponse("Hello World!");
    }

    @Override
    public HttpResponse respondToException(Exception ex) {
        return new PlainTextResponse(ex.toString());
    }
}
