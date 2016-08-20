package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responses.JsonResponse;

public abstract class BaseJsonResponder extends BaseResponder {

    public abstract Object buildJsonObject() throws Exception;

    public static class ErrorBean {
        public Boolean Success = false;
        public String Error;
    }

    @Override
    public HttpResponse respond() {
        try {
            return new JsonResponse(this.buildJsonObject());
        }
        catch (Exception ex) {
            return respondToException(ex);
        }
    }

    @Override
    public HttpResponse respondToException(Exception ex) {
        ErrorBean jsonObject = new ErrorBean();
        jsonObject.Success = false;
        jsonObject.Error = ex.getMessage();
        return new JsonResponse(jsonObject);
    }


}
