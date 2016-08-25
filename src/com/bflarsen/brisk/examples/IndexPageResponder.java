package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpResponse;

import com.bflarsen.brisk.responders.BaseResponder;
import com.bflarsen.brisk.responses.PlainTextResponse;

public class IndexPageResponder extends BaseResponder {

    public String name = "no-name";
    public Integer age = 0;

    @Override
    public HttpResponse buildResponse() {
        return new PlainTextResponse(String.format("Name: %s, Age: %d", name, age));
    }
}
