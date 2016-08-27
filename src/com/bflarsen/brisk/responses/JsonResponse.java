package com.bflarsen.brisk.responses;

import com.google.gson.Gson;

public class JsonResponse extends BaseBufferedResponse {

    public final static Gson encoder = new Gson();

    public Object Payload;

    public JsonResponse(Object payload) {
        super(200);
        this.Payload = payload;
        this.setHeader("Content-Type", "application/json");
        this.setHeader("Cache-Control", "no-statCache, private, must-revalidate, s-maxage=0, max-age=0");
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return encoder.toJson(this.Payload).getBytes(UTF8);
    }
}
