package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpResponse;

public class BaseHtmlResponder extends BaseResponder {
    @Override
    public HttpResponse respond() {
        return null;
    }

    @Override
    public HttpResponse respondToException(Exception ex) {
        return null;
    }
}
