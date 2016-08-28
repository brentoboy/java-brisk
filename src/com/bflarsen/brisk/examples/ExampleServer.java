package com.bflarsen.brisk.examples;

import com.bflarsen.brisk.HttpServer;
import com.bflarsen.util.Logger;

import java.util.regex.Pattern;

public class ExampleServer extends HttpServer{

    public static void main(String[] args) throws Exception {
        final ExampleServer server = new ExampleServer();

        Logger.Instance = new Logger.Interface() {
            @Override
            public void info(String message, String className, String functionName, String whileDoing) {
                server.logToConsole("INFO", message, className, functionName, whileDoing);
            }
            @Override
            public void error(String message, String className, String functionName, String whileDoing) {
                server.logToConsole("ERROR", message, className, functionName, whileDoing);
            }
            @Override
            public void warning(String message, String className, String functionName, String whileDoing) {
                server.logToConsole("WARN", message, className, functionName, whileDoing);
            }
            @Override
            public void trace(String message, String className, String functionName, String whileDoing) {
                server.logToConsole("TRACE", message, className, functionName, whileDoing);
            }
            @Override
            public void except(Exception ex, String className, String functionName, String whileDoing) {
                server.logToConsole(ex, className, functionName, whileDoing);
            }
        };

        server.Port = 8080;
        server.CreateSessions = true;
        server.addRouteToFiles(Pattern.compile("^.*\\.(ico|txt|css|jpg|gif|jpeg)$"), "C:/code/java-brisk/static");
        server.addRoute("/special.html", SpecialPageResponder::new);
        server.addRoute("/", IndexPageResponder::new);
//        server.addRoute("/some.json", SomeJsonResponder::new);
//        server.addRoute("/bad.json", BadJsonResponder::new);
        server.addRoute(Pattern.compile("^http\\:\\/\\/localhost.*\\/x\\/(?<age>\\d+).html$"), IndexPageResponder::new);
        server.run();
        server.wait();
    }

    public void logToConsole(String lvl, String msg, String cls, String fn, String action) {
        System.out.println(String.format("%s: '%s' in %s::%s while '%s') ", lvl, msg, cls, fn, action));
    }
    public void logToConsole(Exception ex, String cls, String fn, String action) {
        logToConsole("Ex", ex.toString(), cls, fn, action);
        ex.printStackTrace();
    }
}
