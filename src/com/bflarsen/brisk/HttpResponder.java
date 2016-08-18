package com.bflarsen.brisk;

public interface HttpResponder {
    HttpResponse handleRequest(HttpContext context);
}
