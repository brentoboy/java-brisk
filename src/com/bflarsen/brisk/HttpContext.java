package com.bflarsen.brisk;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
    public HttpSession Session;
    public Exception ResponderException;
    public List<HttpCookie> SendCookies = new ArrayList<>();

    public static class Statistics {
        public long RequestParserStarted;
        public long RequestParserEnded;
        public long RequestParserAborted;
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
        this.RequestStream = new BufferedReader(new InputStreamReader(new NoCloseInputStream(socket.getInputStream()), "utf-8"));
        if (socket instanceof SSLSocket) {
            this.Request.Protocol = "https";
        }
    }

    public void SendCookie(HttpCookie cookie) {
        this.SendCookies.add(cookie);
    }

    public static class NoCloseInputStream extends java.io.InputStream {
        private java.io.InputStream wrapped;

        public NoCloseInputStream(java.io.InputStream stream) { wrapped = stream; }

        @Override
        public int read() throws IOException { return wrapped.read(); }

        @Override
        public int read(byte b[]) throws IOException { return wrapped.read(b); }

        @Override
        public int read(byte b[], int off, int len) throws IOException { return wrapped.read(b, off, len); }

        @Override
        public long skip(long n) throws IOException { return wrapped.skip(n); }

        @Override
        public int available() throws IOException { return wrapped.available(); }

        @Override
        public synchronized void mark(int readlimit) { wrapped.mark(readlimit); }

        @Override
        public synchronized void reset() throws IOException { wrapped.reset(); }

        @Override
        public boolean markSupported() {
            return wrapped.markSupported();
        }

        @Override
        public void close() { /* don't wrap close, that's the whole point */ }
    }

    public static class NoCloseOutputStream extends java.io.OutputStream {

        private java.io.OutputStream wrapped;

        public NoCloseOutputStream(java.io.OutputStream stream) { wrapped = stream; }

        @Override
        public void write(int b) throws IOException { wrapped.write(b); }

        @Override
        public void write(byte b[]) throws IOException { wrapped.write(b); }

        @Override
        public void write(byte b[], int off, int len) throws IOException { wrapped.write(b, off, len); }

        @Override
        public void flush() throws IOException { wrapped.flush(); }

        @Override
        public void close() throws IOException { wrapped.flush(); flush(); /* don't wrap close, that's the whole point */ }

    }
}
