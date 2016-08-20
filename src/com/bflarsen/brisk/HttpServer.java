package com.bflarsen.brisk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import com.bflarsen.brisk.pumps.*;
import com.bflarsen.brisk.responders.*;

public abstract class HttpServer extends Thread {

    public int Port = 80;
    public final Map<Pattern, Class<? extends HttpResponder>> Routes = new LinkedHashMap<>();
    public Class<? extends HttpResponder> Error404Responder = DefaultError404Responder.class;
    public Class<? extends ExceptionResponder> Error500Responder = DefaultError500Responder.class;

    public boolean isClosing = false;
    public final LinkedBlockingQueue<Socket> IncomingRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> AllRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> ParsedRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> RoutedRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> ResponseReady = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> DoneSending = new LinkedBlockingQueue<>();

    private final HttpRequestParsingPump RequestParser = new HttpRequestParsingPump(this);
    private final HttpRequestRoutingPump RequestRouter = new HttpRequestRoutingPump(this);
    private final HttpResponseBuildingPump ResponseBuilder = new HttpResponseBuildingPump(this);
    private final HttpResponseSendingPump ResponseSender = new HttpResponseSendingPump(this);
    private final HttpContextCleanupPump ContextCleanup = new HttpContextCleanupPump(this);

    @Override
    public void run() {
        this.isClosing = false;
        this.RequestParser.run();
        this.RequestRouter.run();
        this.ResponseBuilder.run();
        this.ResponseSender.run();
        this.ContextCleanup.run();

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

    public void addRoute(String path, Class<? extends HttpResponder> responderClass) throws Exception {
        this.addRoute(buildRegexPattern(path), responderClass);
    }

    public void addRoute(Pattern regex, Class<? extends HttpResponder> responderClass) {
        this.Routes.put(regex, responderClass);
    }

    public Pattern buildRegexPattern(String path) throws Exception {
        String regexPattern;

        // if it smells like a protocol relative path in the form of //domain.com/resource.html ...
        if (path.startsWith("//")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^(http|https)\\:" + path + "$";
        }
        // if it smells like its relative to site base
        else if (path.startsWith("/")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^(http|https)\\:\\/\\/[^\\/]+" + path + "$";
        }
        // if it smells like it has a protocol prefix
        else if (path.startsWith("http://") || path.startsWith("https://")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^" + path + "$";
        }
        else {
            throw new Exception(String.format("'%s' is not in one of the expected formats", path));
        }

        return Pattern.compile(regexPattern);
    }

    public abstract void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing);

    public abstract void LogHandler(String message);
}