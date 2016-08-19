package com.bflarsen.brisk.responses;

import java.nio.charset.Charset;

public abstract class HtmlResponse extends BaseResponse {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    protected HtmlResponse(int statusCode) {
        super(statusCode);
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
