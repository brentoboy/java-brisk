package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpResponder;

public interface ExceptionResponder extends HttpResponder {
    void setException(Exception ex);
}
