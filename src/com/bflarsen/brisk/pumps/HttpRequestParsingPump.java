package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpCookie;
import com.bflarsen.brisk.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpRequestParsingPump implements Runnable {

    private HttpServer httpServerInstance;
    private static Pattern paramWithSubscriptRegex = Pattern.compile("^.+\\[.+\\]$");

    public HttpRequestParsingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfRequestParsingThreadsToCreate];
        // spawn a bunch of workers
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(this);
            workers[i].start();
        }
        // wait for them to close
        for (int i = 0; i < workers.length; i++) {
            try {
                workers[i].wait();
            }
            catch(Exception ex) {}
        }
    }

    private static String tryReadLine(BufferedReader stream) {
        try {
            return stream.readLine();
        }
        catch (IOException ex) {
            if (!ex.getMessage().equals("Connection reset")) {
                // hmm
                // parentPump.httpServerInstance.ExceptionHandler.handle(ex, this.getClass().getName(), "tryReadLine", "stream.readLine()");
            }
            return "";
        }
    }

    public static void parseRequest(HttpContext context) throws Exception {
        if (context == null)
            throw new Exception("Empty Context object in parseRequest.");
        if (context.Request == null)
            throw new Exception("Empty Request object in parseRequest.");
        if (context.RequestStream == null)
            throw new Exception("Empty RequestStream object in parseRequest.");

        String line = tryReadLine(context.RequestStream);
        if (line == null || line.isEmpty()) {
            throw new EmptyRequestException();
        }
        String[] parts = line.split(" ", 3);
        context.Request.Method = (parts.length > 0) ? parts[0].toUpperCase() : "";
        context.Request.Resource = (parts.length > 1) ? parts[1] : "";
        context.Request.HttpVersion = (parts.length > 2) ? parts[2] : "";

        parts = context.Request.Resource.split("\\?", 2);
        context.Request.Path = parts[0];

        // check for the rest of the params
        if (parts.length == 2) {
            String paramString = parts[1].replace("&amp;", "&");
            parts = paramString.split("&");
            for (String part : parts) {
                int pos = part.indexOf('=');
                if (pos != -1) {
                    try {
                        String key = URLDecoder.decode(part.substring(0, pos), "UTF-8");
                        String value = URLDecoder.decode(part.substring(pos + 1), "UTF-8");
                        if (paramWithSubscriptRegex.matcher(key).matches())
                        {
                            pos = key.indexOf('[');
                            String baseKey = key.substring(0, pos);
                            String[] subKeys = key.substring(pos + 1, key.length() - 1).split(Pattern.quote("]["));
                            if (!context.Request.Params.containsKey(baseKey)
                                    || ! (context.Request.Params.get(baseKey) instanceof Map<?, ?>)
                            ) {
                                context.Request.Params.put(baseKey, new HashMap<String, Object>());
                            }
                            Map<String, Object> prop = (Map<String, Object>) context.Request.Params.get(baseKey);
                            for (int i = 0; i < subKeys.length - 1; i++) {
                                if (!prop.containsKey(subKeys[i])
                                        || !(prop.get(subKeys[i]) instanceof Map<?, ?>)
                                ) {
                                    prop.put(subKeys[i], new HashMap<String, Object>());
                                }
                                prop = (Map<String, Object>) prop.get(subKeys[i]);
                            }
                            prop.put(subKeys[subKeys.length - 1], value);
                        }
                        else
                        {
                            context.Request.Params.put(key, value);
                        }
                    } catch (Exception ex) {
                        // HMM
                    }
                }
            }
        }

        // read in the headers line by line
        line = tryReadLine(context.RequestStream);
        while (!line.equals("")) {
            int pos = line.indexOf(':');
            if (pos != -1)
            {
                String key = "Request_" + line.substring(0, pos).replace("-", "");
                String value = line.substring(pos + 1).trim();

                if (key.equals("Request_Cookie")) {
                    String[] allCookies = value.split(";");
                    for (String cookie : allCookies) {
                        int splitCookieAt = cookie.indexOf('=');
                        String cookieName = cookie.substring(0, splitCookieAt).trim();
                        String cookieValue = cookie.substring(splitCookieAt + 1).trim();
                        context.Request.Cookies.put(cookieName, cookieValue);
                    }
                }
                else if (context.Request.Headers.containsKey(key)) {
                    if (key.equals("Request_Accept") || key.equals("Request_AcceptEncoding")) {
                        String oldValue = context.Request.Headers.get(key);
                        context.Request.Headers.put(key, oldValue + ", " + value);
                    }
                }
                else {
                    context.Request.Headers.put(key, value);
                }
            }

            line = tryReadLine(context.RequestStream);
        }

        // extract the "Host" header for easy access
        if (context.Request.Headers.containsKey("Request_Host")) {
            context.Request.Host = context.Request.Headers.get("Request_Host");
        }

        // attach a session
        if (context.Server.CreateSessions) {
            String sessionID = context.Request.Cookies.get(context.Server.SessionCookieName);
            if (sessionID != null) {
                context.Session = context.Server.Sessions.get(sessionID);
            }
            else {
                sessionID = java.util.UUID.randomUUID().toString().replace("-", "");
            }
            if (context.Session == null || context.Session.Expires < System.currentTimeMillis()) {
                context.Session = context.Server.createSessionObject();
                context.Session.UniqueID = sessionID;
            }
            context.Session.Expires = System.currentTimeMillis() + context.Server.SessionExpiresAfterMillis;
            context.Server.Sessions.put(context.Session.UniqueID, context.Session);
            // tell the browser to add (or update the expiration on) the session cookie
            HttpCookie cookie = new HttpCookie(context.Server.SessionCookieName, context.Session.UniqueID);
            cookie.Expires = context.Session.Expires;
            cookie.Domain = context.Server.SessionDomain;
            context.SendCookie(cookie);
        }


        // TODO: parse body
    }

    public static class EmptyRequestException extends Exception {}

    private static class Worker extends Thread {
        HttpRequestParsingPump parentPump;

        Worker(HttpRequestParsingPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                Socket socket = null;
                HttpContext context = null;
                try {
                    Thread.yield();
                    socket = parentPump.httpServerInstance.IncomingRequests.take();
                    if (socket != null) {
                        socket.setSoTimeout(5000);
                        context = new HttpContext(parentPump.httpServerInstance, socket);
                        context.Stats.RequestParserStarted = System.nanoTime();
                        try {
                            parseRequest(context);
                        }
                        catch (EmptyRequestException ex) {
                            context.Stats.RequestParserAborted = System.nanoTime();
                            context = null;
                            socket.close();
                            parentPump.httpServerInstance.LogHandler("Empty request, discarded");
                        }
//                        catch (takingTooLong ex) {
//                            parentPump.httpServerInstance.IncomingRequests.put(socket);
//                            context = null;
//                            parentPump.httpServerInstance.LogHandler("initial socket read was taking too long, we recycled it");
//                        }
                    }
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    parentPump.httpServerInstance.ExceptionHandler(ex, this.getClass().getName(), "run()", "parsing a request");
                }
                finally {
                    if (context != null) {
                        context.Stats.RequestParserEnded = System.nanoTime();
                        parentPump.httpServerInstance.ParsedRequests.add(context);
                        parentPump.httpServerInstance.AllRequests.add(context);
                    }
                }
            }
        }
    }
}
