package com.bflarsen.brisk;

public interface HttpResponder {
    interface Factory { HttpResponder create() throws Exception; }

    boolean canHandle(HttpContext context);
    HttpResponse respond(HttpContext context) throws Exception;
}
