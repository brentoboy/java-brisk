package com.bflarsen.brisk.responses;

public class NotAuthorizedResponse extends PlainTextResponse {
    public NotAuthorizedResponse() {
        super(401, "Not Authorized");
    }
}
