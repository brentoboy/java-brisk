package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.responders.BaseJsonResponder;

public class BadJsonResponder extends BaseJsonResponder{
    @Override
    public Object buildJsonObject() throws Exception {
        throw new Exception("Something bad happened");
    }
}
