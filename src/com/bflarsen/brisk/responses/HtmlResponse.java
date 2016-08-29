package com.bflarsen.brisk.responses;

import java.nio.charset.Charset;

public abstract class HtmlResponse extends BaseBufferedResponse {

    protected HtmlResponse(int statusCode) {
        super(statusCode);
        this.setHeader("Content-Type", "text/html");
    }

    public abstract String generateHtml() throws Exception;

    @Override
    public byte[] getBodyBytes() throws Exception {
        String html = generateHtml();
        if (html == null)
            html = "";
        return html.getBytes(UTF8);
    }
}
