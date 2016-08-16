package com.bflarsen.brisk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class HttpServer extends Thread {

    public int Port = 80;
    public final Map<Pattern, IHttpResponseBuilder> Routes = new HashMap<>();

    public IExceptionHandler ExceptionHandler = (ex, cls, fn, action) ->
            System.out.println(String.format("Exception '%s' in %s : %s while '%s') ", ex.getMessage(), cls, fn, action));

    public boolean isClosing = false;
    public final LinkedBlockingQueue<Socket> IncomingRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> ParsedRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> AllRequests = new LinkedBlockingQueue<>();

    private final HttpRequestParsingPump RequestParsingPump = new HttpRequestParsingPump(this);

    @Override
    public void run() {
        this.isClosing = false;
        this.RequestParsingPump.run();

        try (ServerSocket serverSocket = new ServerSocket(this.Port)) {
            while (!this.isClosing) {
                try {
                    Socket socket = serverSocket.accept();
                    this.IncomingRequests.add(socket);
                }
                catch (Exception ex) {
                    this.ExceptionHandler.handle(ex, this.getClass().getName(), "run", "Attempting to accept() on port " + this.Port);
                }
            }
        }
        catch(IOException ex){
            this.ExceptionHandler.handle(ex, this.getClass().getName(), "run", "Attempting to listen() on port " + this.Port);
        }
    }

    public void initiateShutdown() {
        this.isClosing = true;
    }
}