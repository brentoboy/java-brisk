package com.bflarsen.brisk.responses;

public class NullResponse extends HtmlResponse {
    public NullResponse() {
        super(404);
    }

    @Override
    public String generateHtml() throws Exception {
        return null;
    }
}
