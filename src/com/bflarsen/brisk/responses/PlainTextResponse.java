package com.bflarsen.brisk.responses;

public class PlainTextResponse extends BaseResponse {

    String Text;

    public PlainTextResponse(String text) {
        super(200);
        this.Text = text;
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return this.Text.getBytes("UTF-8");
    }
}
