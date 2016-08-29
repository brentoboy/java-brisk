package com.bflarsen.brisk.responses;

public class RedirectResponse extends BaseBufferedResponse {

    public RedirectResponse() {
        super(301, "Redirected");
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        throw new Exception("This shouldn't be called, because I supplied content to the constructor.");
    }
}
