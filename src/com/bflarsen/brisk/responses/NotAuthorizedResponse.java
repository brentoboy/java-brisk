package com.bflarsen.brisk.responses;

public class NotAuthorizedResponse extends HtmlResponse {
    public NotAuthorizedResponse() {
        super(401);
    }

    @Override
    public String generateHtml() throws Exception {
        return null;
    }
}
