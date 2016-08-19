package com.bflarsen.brisk.responses;

public class RedirectResponse extends BaseResponse {

    public RedirectResponse() {
        super(301);
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return "Redirected".getBytes("UTF-8");
    }
}
