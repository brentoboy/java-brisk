package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.HttpResponse;

public interface ExceptionResponder extends HttpResponder {
    interface Factory {
        ExceptionResponder create();
    }

    HttpResponse respondToException(Exception ex, HttpContext context);
}
