package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpResponder;

public class DefaultError500Responder implements ExceptionResponder {

    Exception ExceptionThrown;

    @Override
    public HttpResponse handleRequest(HttpContext context) {
        return null;
    }

    @Override
    public void setException(Exception ex) {
        this.ExceptionThrown = ex;
    }
}
