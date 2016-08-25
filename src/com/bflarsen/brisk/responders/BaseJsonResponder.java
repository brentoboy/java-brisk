package com.bflarsen.brisk.responders;


import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responses.BaseResponse;
import com.bflarsen.brisk.responses.JsonResponse;

public abstract class BaseJsonResponder extends BaseResponder {

    public abstract Object buildJsonObject() throws Exception;

    public static class ErrorBean {
        public Boolean Success = false;
        public String Error;
    }

    @Override
    public HttpResponse buildResponse() throws Exception {
        try {
            return new JsonResponse(this.buildJsonObject());
        }
        catch (Exception ex) {
            ErrorBean errorBean = new ErrorBean();
            errorBean.Success = false;
            errorBean.Error = ex.getMessage();
            BaseResponse response = new JsonResponse(errorBean);
            response.StatusCode = 500;
            return response;
        }
    }
}
