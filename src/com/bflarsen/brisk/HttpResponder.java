package com.bflarsen.brisk;

public interface HttpResponder {
    interface Factory { HttpResponder create(); }

    boolean canHandle(HttpContext context);
    HttpResponse respond(HttpContext context) throws Exception;
}
