package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.HttpServer;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class HttpResponseSendingPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpResponseSendingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[8];
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

    public static void sendResponse(HttpContext context) throws Exception {
        if (context == null)
            throw new Exception("Empty Context object in sendResponse.");
        if (context.Response == null)
            throw new Exception("Empty Response object in sendResponse.");
        if (context.RequestStream == null)
            throw new Exception("Empty ResponseStream object in sendResponse.");

        HttpResponse response = context.Response;
        OutputStream stream = context.ResponseStream;
        PrintWriter streamWriter = new java.io.PrintWriter(stream, true);

        byte[] bodyBytes = null;
        try {
            bodyBytes = response.getBodyBytes();
        }
        catch (Exception ex) {
            context.Server.ExceptionHandler(ex, "HttpResponseSendingPump", "sendResponse", "getBodyBytes");
        }
        if (bodyBytes != null) {
            response.setHeader("Content-Length", ((Integer)bodyBytes.length).toString());
        }
        else {
            Long length = response.getContentLength();
            if (length != null) {
                response.setHeader("Content-Length", length.toString());
            }
        }

        // send the first line ... something like this :  "HTTP/1.1 200 OK"
        streamWriter.println(String.format(
                "%s %d %s"
                , response.getHttpVersion()
                , response.getStatusCode()
                , response.getStatusDescription()
        ));

        // send all the header lines ... something like this: "Header-Name: header-value"
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            streamWriter.println(String.format(
                    "%s: %s"
                    , header.getKey()
                    , header.getValue()
            ));
        }

        // send a blank line signifying "</end headers>"
        streamWriter.println("");
        streamWriter.flush();

        // record the time to first byte
        context.Stats.SendBodyStarted = System.nanoTime();

        // send body
        if (bodyBytes != null) {
            try {
                stream.write(bodyBytes);
            }
            catch (Exception ex) {
                context.Server.ExceptionHandler(ex, "HttpResponseSendingPump", "sendResponse", "writing bodyBytes");
            }
        } else {
            try {
                response.sendBody(stream);
            }
            catch (Exception ex) {
                context.Server.ExceptionHandler(ex, "HttpResponseSendingPump", "sendResponse", "response.sendBody()");
            }
        }
        try {
            stream.flush();
        }
        catch (Exception ex) {
            context.Server.ExceptionHandler(ex, "HttpResponseSendingPump", "sendResponse", "final flush");
        }

        context.Stats.SendBodyEnded = System.nanoTime();
    }

    private static class Worker extends Thread {
        HttpResponseSendingPump parentPump;

        Worker(HttpResponseSendingPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.ResponseReady.take();
                    context.ResponseStream = context.Socket.getOutputStream();
                    context.Stats.ResponseSenderStarted = System.nanoTime();
                    sendResponse(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    parentPump.httpServerInstance.ExceptionHandler(ex, this.getClass().getName(), "run()", "building a response");
                }
                finally {
                    if (context != null) {
                        context.Stats.ResponseSenderEnded = System.nanoTime();
                        parentPump.httpServerInstance.DoneSending.add(context);
                    }
                }
            }
        }
    }


/*

function sendResponse(context) {
	var response = context.response = context.response || {};
	var stream = context.responseStream;
	var streamWriter = context.responseWriter;

	response.httpVersion = response.httpVersion || "HTTP/1.1";
	response.statusCode = response.statusCode || 200;
	response.statusDescription = response.statusDescription || lookupStatusDescription(response.statusCode);

	response.headers = response.headers || {};
	response.headers["Date"] = response.headers["Date"] || new Date().toUTCString();
	response.headers["Server"] = response.headers["Server"] || "JBrisk/0.1.0";
	response.headers["Content-Type"] = response.headers["Content-Type"] || "text/html; charset=UTF-8";
	// probably depends
	//   response.headers["Content-Encoding"] = response.headers["Content-Encoding"] || "utf-8";
	// if they ask for persistent connections
	//   response.headers["Connection"] = response.headers["Connection"] || "keep-alive";
	response.headers["Cache-Control"] = response.headers["Cache-Control"] || "max-age=0";

	if (response.bodyText) {
		response.headers["Content-Length"] = response.bodyText.length;
	}

	//these are more optional
	//response.headers[] = response.headers[] ||
	if ("Location" in response.headers) {
	}
	if ("Last-Modified" in response.headers) {
	}
	if ("Expires" in response.headers) {
	}
	//"Set-Cookie"

	// send the first line ... something like this: "HTTP/1.1 200 OK"
	streamWriter.println(
		response.httpVersion
		+ " " + response.statusCode
		+ " " + response.statusDescription
	);

	// send all the header lines ... something like this: "Header-Name: header-value"
	for (header in response.headers) {
		streamWriter.println(
			header + ": " + response.headers[header]
		);
	}

	// send a blank line signifying "</end headers>"
	streamWriter.println("");
	streamWriter.flush();
	context.stats.ttfb = Util.getMoment();

	// send the body
	if (response.bodyText) {
		streamWriter.print(response.bodyText);
		streamWriter.flush();
	}
	else if (response.bodyBytes) {
		context.responseStream.write(response.bodyBytes);
		context.responseStream.flush();
	}
	else if (typeof response.sendBody === "function") {
		response.sendBody(context.responseStream);
		context.responseStream.flush();
	}
	context.stats.ttlb = Util.getMoment();
}



 */

}
