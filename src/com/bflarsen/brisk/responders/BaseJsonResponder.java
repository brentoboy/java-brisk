package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responses.JsonResponse;

public abstract class BaseJsonResponder extends BaseResponder {

    public abstract Object buildJsonObject();

    @Override
    public HttpResponse respond() {
        return new JsonResponse(this.buildJsonObject());
    }
}
