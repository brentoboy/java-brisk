package com.bflarsen.brisk.responses;

import java.io.OutputStream;

public abstract class BaseBufferedResponse extends BaseResponse {

    byte[] buffer;

    public BaseBufferedResponse(int statusCode) {
        super(statusCode);
    }

    public BaseBufferedResponse(int statusCode, String text) {
        this(statusCode);
        if (text != null) {
            buffer = text.getBytes(UTF8);
        }
    }

    public abstract byte[] getBodyBytes() throws Exception;

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
