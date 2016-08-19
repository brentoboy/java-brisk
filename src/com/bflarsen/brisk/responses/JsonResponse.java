package com.bflarsen.brisk.responses;

public class JsonResponse extends BaseResponse {

    public Object Payload;

    public JsonResponse(Object payload) {
        super(200);
        this.Payload = payload;
        this.setHeader("Content-Type", "application/json");
        this.setHeader("Cache-Control", "no-cache, private, must-revalidate, s-maxage=0, max-age=0");
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        // TODO: jsonify this
        return this.Payload.toString().getBytes("UTF-8");
    }
}
