package com.bflarsen.brisk;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;

public class HttpRequestParsingPump implements Runnable {

    private HttpServer httpServerInstance;
    private Worker[] workers;

    public HttpRequestParsingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        workers = new Worker[8];
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

    public static void parseRequest(HttpContext context) {
        if (context == null || context.RequestStream == null)
            return;  // TODO: perhaps log this?

        String line = tryReadLine(context.RequestStream);
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
//                            if (Regex.isMatch(paramWithSubscriptRegex, key))
//                          {
//                                pos = key.indexOf('[');
//                                var baseKey = Str.slice(key, 0, pos);
//                                var subKeys = Str.split(Str.slice(key, pos + 1, Str.length(key) - 1), "][");
//                              var lastSubkey = subKeys.pop();
//                                request.params[baseKey] = request.params[baseKey] || {};
//                                var prop = request.params[baseKey];
//                                List.each(subKeys, function(subkey) {
//                              prop[subkey] = prop[subkey] || {};
//                                prop = prop[subkey];
//                            });
//                                prop[lastSubkey] = value;
//                          }
//                            else
//                            {
                        context.Request.Params.put(key, value);
//                          }
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
                String key = line.substring(0, pos);
                String value = line.substring(pos + 1).trim();

                if (context.Request.Headers.containsKey(key)) {
                    if (key.equals("Accept") || key.equals("Accept-Encoding")) {
                        String oldValue = context.Request.Headers.get(key).toString();
                        context.Request.Headers.put(key, oldValue + ", " + value);
                    }
                    // else:
                    // TODO: ?? console.log("key already exists, '" + key + "' with value '" + request.headers[key] + "'\nSo I dont want to overwrite it with " + value);
                }
                else {
                    context.Request.Headers.put(key, value);
                }
            }

            line = tryReadLine(context.RequestStream);
        }

        // extract the "Host" header for easy access
        if (context.Request.Headers.containsKey("Host")) {
            context.Request.Host = context.Request.Headers.get("Host").toString();
        }
    }


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
                        context = new HttpContext(socket);
                        context.Stats.RequestParserStarted = System.nanoTime();
                        parseRequest(context);
                    }
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    parentPump.httpServerInstance.ExceptionHandler.handle(ex, this.getClass().getName(), "run()", "parsing a request");
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
