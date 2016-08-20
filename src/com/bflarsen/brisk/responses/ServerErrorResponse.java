package com.bflarsen.brisk.responses;

public class ServerErrorResponse extends HtmlResponse {
    public ServerErrorResponse() {
        super(500);
    }

    @Override
    public String generateHtml() throws Exception {
        return null;
    }
}
