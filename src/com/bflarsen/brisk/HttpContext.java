package com.bflarsen.brisk;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class HttpContext {

    public int Id;
    public HttpServer Server;
    public Socket Socket;
    public Statistics Stats;
    public BufferedReader RequestStream;
    public OutputStream ResponseStream;
    public HttpRequest Request;
    public HttpResponder Responder;
    public HttpResponse Response;
    public Exception ResponderException;

    public static class Statistics {
        public long RequestParserStarted;
        public long RequestParserEnded;
        public long RequestRouterStarted;
        public long RequestRouterEnded;
        public long ResponseBuilderStarted;
        public long ResponseBuilderEnded;
        public long ResponseSenderStarted;
        public long ResponseSenderEnded;
        public long SendBodyStarted;
        public long SendBodyEnded;
        public long CleanupStarted;
        public long CompletelyFinished;
        public long Expired;
        public long totalMs;
    }

    public HttpContext(HttpServer server) {
        this.Id = java.util.UUID.randomUUID().hashCode();
        this.Stats = new Statistics();
        this.Request = new HttpRequest();
        this.Server = server;
    }

    public HttpContext(HttpServer server, Socket socket) throws Exception {
        this(server);
        this.Socket = socket;
        this.RequestStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
        if (socket instanceof SSLSocket) {
            this.Request.Protocol = "https";
        }
    }
}
