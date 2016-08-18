package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;

public abstract class BaseResponder {
    public abstract HttpResponse respond();
    public abstract HttpResponse respondToException(Exception ex);
}
