package com.bflarsen.brisk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class HttpContext {

    public int Id;
    public Socket Socket;
    public Statistics Stats;
    public BufferedReader RequestStream;
    public HttpRequest Request;
    public IExceptionHandler ExceptionHandler;

    public static class Statistics {
        public long RequestParserStarted;
        public long RequestParserEnded;
    }

    public HttpContext() {
        this.Id = java.util.UUID.randomUUID().hashCode();
        this.Stats = new Statistics();
        this.Request = new HttpRequest();
    }

    public HttpContext(Socket socket) throws Exception {
        this();
        this.Socket = socket;
        this.RequestStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
    }
}
