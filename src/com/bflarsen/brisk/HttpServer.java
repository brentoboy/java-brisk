package com.bflarsen.brisk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import com.bflarsen.brisk.responders.*;

public abstract class HttpServer extends Thread {

    public int Port = 80;
    public final Map<Pattern, Class<? extends HttpResponder>> Routes = new HashMap<>();
    public Class<? extends HttpResponder> Error404Responder = DefaultError404Responder.class;
    public Class<? extends HttpResponder> Error500Responder = DefaultError500Responder.class;

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
            this.LogHandler("Listening on Port " + this.Port);

            while (!this.isClosing) {
                try {
                    Socket socket = serverSocket.accept();
                    this.IncomingRequests.add(socket);
                }
                catch (Exception ex) {
                    this.ExceptionHandler(ex, this.getClass().getName(), "run", "Attempting to accept() on port " + this.Port);
                }
            }
        }
        catch(IOException ex){
            this.ExceptionHandler(ex, this.getClass().getName(), "run", "Attempting to listen() on port " + this.Port);
        }

        this.LogHandler("No longer listening on Port " + this.Port);
    }

    public void initiateShutdown() {
        this.LogHandler("Initiating Http Server Shutdown");
        this.isClosing = true;
    }

    public abstract void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing);

    public abstract void LogHandler(String message);
}