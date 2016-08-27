package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpServer;

import java.util.regex.Pattern;

public class ExampleServer extends HttpServer{

    public static void main(String[] args) throws Exception {
        ExampleServer server = new ExampleServer();
        server.Port = 8080;
        server.CreateSessions = true;
        server.addRouteToFiles(Pattern.compile("^.*\\.(ico|txt|css|jpg|gif|jpeg)$"), "C:/code/java-brisk/static");
        server.addRoute("/special.html", SpecialPageResponder::new);
        server.addRoute("/", IndexPageResponder::new);
        server.addRoute("/some.json", SomeJsonResponder::new);
        server.addRoute("/bad.json", BadJsonResponder::new);
        server.run();
        server.wait();
    }

    @Override
    public void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = ex.toString();
        }
        System.out.println(String.format("Exception '%s' in %s : %s while '%s') ", msg, className, functionName, whileDoing));
        ex.printStackTrace();
    }

    @Override
    public void LogHandler(String message) {
        System.out.println(message);
    }

}
