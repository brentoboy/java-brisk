package com.bflarsen.brisk.responses;

import java.io.OutputStream;

public class BaseBufferedResponse extends BaseResponse {

    public byte[] buffer;

    public BaseBufferedResponse(int statusCode, String contentType, String textContent) {
        super(statusCode, contentType);
        if (textContent != null) {
            buffer = textContent.getBytes(UTF8);
        }
    }

    public byte[] getBodyBytes() throws Exception {
        // todo: log, complain, do something ...  you if you dont pass in text, you've got to override getBodyBytes!
        return null;
    }

    @Override
    public Long getContentLength() throws Exception {
        if (buffer == null) {
            buffer = this.getBodyBytes();
        }
        if (buffer == null)
            return null;
        return (long)buffer.length;
    }

    @Override
    public void sendBody(OutputStream stream) throws Exception {
        if (buffer == null)
            buffer = this.getBodyBytes();
        if (buffer == null)
            return;
        stream.write(buffer);
    }
}
