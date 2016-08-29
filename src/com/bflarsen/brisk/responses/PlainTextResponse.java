package com.bflarsen.brisk.responses;

public class PlainTextResponse extends BaseBufferedResponse {
    String Text;

    public PlainTextResponse(int statusCode, String text) {
        super(statusCode);
        this.Text = text;
    }

    public PlainTextResponse(String text) {
        this(200, text);
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return this.Text.getBytes(UTF8);
    }
}
