package com.bflarsen.brisk;

public interface HttpResponder {

    interface Factory {
        HttpResponder create();
    }

    static Factory createFactory(Class<? extends HttpResponder> cls) {
        return () -> {
            try { return cls.newInstance(); }
            catch(Exception ex) { return null; }
        };
    }

    boolean canHandle(HttpContext context);
    HttpResponse handleRequest(HttpContext context);
}
