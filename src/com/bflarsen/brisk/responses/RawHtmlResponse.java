package com.bflarsen.brisk.responses;

import java.io.Writer;

public class RawHtmlResponse extends BaseResponse {

    public String RawHtml;

    public RawHtmlResponse(String html) {
        super(200);
        this.setHeader("Content-Type", "text/html");
        this.RawHtml = html;
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return this.RawHtml.getBytes("UTF-8");
    }
}
