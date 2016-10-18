package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseResponder implements HttpResponder {

    protected HttpContext http_context;

    public abstract HttpResponse buildResponse() throws Exception;

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) throws Exception {
        this.http_context = context;
        // this is here to protect session variables.  if you name them "session..." then its impossible to spoof them via request params
        Map<String, Object> filteredRequestParams =
                context.Request.Params.entrySet().stream()
                .filter(x -> !x.getKey().startsWith("session_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ;
        Map<String, Object> renamedSessionParams = null;
        if (context.Session != null) {
            renamedSessionParams =
                    context.Session.Params.entrySet().stream()
                    .collect(Collectors.toMap(x -> "session_" + x.getKey(), Map.Entry::getValue))
            ;
        }
        context.Server.AutoConverter.fill(this, filteredRequestParams);
        context.Server.AutoConverter.fill(this, renamedSessionParams);
        context.Server.AutoConverter.fill(this, context.Request.Headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (x) -> x.getValue())));
        context.Server.AutoConverter.fill(this, context.WorkerThreadResources);
        return this.buildResponse();
    }
}
