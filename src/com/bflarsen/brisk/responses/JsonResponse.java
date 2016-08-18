package com.bflarsen.brisk.responses;

public class JsonResponse extends BaseResponse {

    public Object Payload;

    public JsonResponse(Object payload) {
        this.Payload = payload;
    }

    @Override
    public void sendBody(java.io.Writer stream) {

    }
}
