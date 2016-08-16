package com.bflarsen.brisk;

/**
 * Created by brent on 8/15/2016.
 */
public interface IExceptionHandler {
    void handle(Exception ex, String className, String functionName, String whileDoing);
}
