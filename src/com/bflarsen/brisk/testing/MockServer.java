package com.bflarsen.brisk.testing;

import com.bflarsen.brisk.HttpServer;

import java.util.ArrayList;
import java.util.List;

public class MockServer extends HttpServer {

    public List<Exception> Exceptions = new ArrayList<>();

    @Override
    public void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing) {
        Exceptions.add(ex);
        System.out.println(String.format("Exception '%s' in %s : %s while '%s') ", ex.getMessage(), className, functionName, whileDoing));
    }

    @Override
    public void LogHandler(String message) {
        System.out.println(message);
    }
}
