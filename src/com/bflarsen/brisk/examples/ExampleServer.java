package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpServer;

public class ExampleServer extends HttpServer{

    public static void main(String[] args) throws Exception {
        ExampleServer server = new ExampleServer();
        server.Port = 8080;
        server.addRoute("/", IndexPageResponder.class);
        server.run();
        server.wait();
    }

    @Override
    public void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing) {
        System.out.println(String.format("Exception '%s' in %s : %s while '%s') ", ex.getMessage(), className, functionName, whileDoing));
    }

    @Override
    public void LogHandler(String message) {
        System.out.println(message);
    }

}
