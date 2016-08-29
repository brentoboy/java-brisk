package com.bflarsen.brisk;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import com.bflarsen.brisk.pumps.*;
import com.bflarsen.brisk.responders.*;
import com.bflarsen.convert.AutoConvert;

public abstract class HttpServer extends Thread {

    public int Port = 80;
    public final Map<Pattern, HttpResponder.Factory> Routes = new LinkedHashMap<>();

    public HttpResponder.Factory Error404ResponderFactory = DefaultError404Responder::new;
    public HttpResponder.Factory Error500ResponderFactory = DefaultError500Responder::new;

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

    public final AutoConvert AutoConverter = new AutoConvert();
    // public freemarker.template.Configuration ViewEngine;

    public HttpServer() {
        AutoConverter.ExceptionHandler = (ex) -> this.LogHandler(String.format("AutoConvert Error: %s", ex.getMessage()));
    }

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

    public void addRoute(String path, HttpResponder.Factory factory) throws Exception {
        this.addRoute(buildRegexPattern(path), factory);
    }

    public void addRoute(Pattern regex, HttpResponder.Factory factory) {
        this.Routes.put(regex, factory);
    }

//    public void addRoute(String path, Class<? extends HttpResponder> cls) throws Exception {
//        this.addRoute(path, HttpResponder.createFactory(cls));
//    }
//
//    public void addRoute(Pattern regex, Class<? extends HttpResponder> cls) throws Exception {
//        this.Routes.put(regex, HttpResponder.createFactory(cls));
//    }

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

    public void addRouteToFiles(String pattern, String path) throws Exception {
        addRouteToFiles(buildRegexPattern(pattern), path);
    }

    public void addRouteToFiles(Pattern pattern, String baseDirectory) throws Exception {
        addRoute(pattern, StaticFileResponder.createFactory(Paths.get(baseDirectory)));
    }

//    public void initViewEngine(Path templateFolder) throws Exception {
//        ViewEngine = new Configuration(Configuration.VERSION_2_3_25);
//        ViewEngine.setDirectoryForTemplateLoading(templateFolder.toFile());
//        ViewEngine.setDefaultEncoding("UTF-8");
//        ViewEngine.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
//        ViewEngine.setLogTemplateExceptions(false);
//    }
    public abstract void ExceptionHandler(Exception ex, String className, String functionName, String whileDoing);

    public abstract void LogHandler(String message);

    public Map<String, Object> createWorkerThreadResources() throws Exception {
        return new LinkedHashMap<>();
    }
    public void freeWorkerThreadResources(Map<String, Object> resources) throws Exception {
        for (Map.Entry<String, Object> entry : resources.entrySet()) {
            String key = entry.getKey();
            Object resource = entry.getValue();
            if (resource instanceof AutoCloseable) {
                AutoCloseable closeable = (AutoCloseable) resource;
                try {
                    closeable.close();
                } catch (Exception ex) {
                    ExceptionHandler(ex, "HttpServer", "freeWorkerThreadResources", "closing '" + key + "'");
                }
            }
        }
    }
    public void resetWorkerThreadResources(Map<String, Object> resources) throws Exception {}

}