package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import static com.bflarsen.util.Logger.*;

import java.util.Map;
import java.util.regex.Pattern;


public class HttpRequestRoutingPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpRequestRoutingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfRequestRoutingThreadsToCreate];
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

    public static void chooseRoute(HttpContext context) {
        String RequestedUpgrade = context.Request.getHeader("Request_Upgrade");
        if (RequestedUpgrade != null) {
            try {
                switch (RequestedUpgrade.toLowerCase()) {
                    case "websocket": {
                        context.Responder = context.Server.WebSocketUpgradeResponderFactory.create();
                        return;
                    }
                    default: { // fail due to unrecognized upgrade request
                        logInfo(String.format("Unrecognized http protocol upgrade: %s", RequestedUpgrade), "HttpRequestRoutingPump", "chooseRoute", "upgrading request");
                        context.Responder = context.Server.Error404ResponderFactory.create();
                        return;
                    }
                }
            }
            catch (Exception ex) {
                logInfo(ex.getMessage(), "HttpRequestRoutingPump", "chooseRoute", "creating a responder instance for upgrade request");
            }
            return;
        }
        String url = context.Request.getUrl();
        for(Map.Entry<Pattern, HttpResponder.Factory> route : context.Server.Routes.entrySet()) {
            if (route.getKey().matcher(url).matches()) {
                HttpResponder.Factory factory = route.getValue();
                try {
                    HttpResponder responder = factory.create();
                    if (responder.canHandle(context)) {
                        context.Responder = responder;
                        return;
                    }
                }
                catch (Exception ex) {
                    logInfo(ex.getMessage(), "HttpRequestRoutingPump", "chooseRoute", "creating a responder instance");
                }
            }
        }
        logInfo("No route found to match: " + url, "HttpRequestRoutingPump", "chooseRoute", "");
        try {
            context.Responder = context.Server.Error404ResponderFactory.create();
        }
        catch (Exception ex) {
            logInfo(ex.getMessage(), "HttpRequestRoutingPump", "chooseRoute", "creating a 404 responder instance");
        }
    }

    private static class Worker extends Thread {
        HttpRequestRoutingPump parentPump;

        Worker(HttpRequestRoutingPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.ParsedRequests.take();
                    context.Stats.RequestRouterStarted = System.nanoTime();
                    chooseRoute(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "choosing a route");
                }
                finally {
                    if (context != null) {
                        context.Stats.RequestRouterEnded = System.nanoTime();
                        parentPump.httpServerInstance.RoutedRequests.add(context);
                    }
                }
            }
        }
    }

    /*
-------------- URL parser thing
var commonPatterns = {
	domain: "(https?:\\/\\/)?([\\da-z_\\.-]+)\\.([a-z\\.]{2,6})",
	int: "(\\-?\\d+)",
	slug: "([a-z0-9_\\-]+)",
	string: "([a-z0-9 !@#$%^&*()_+=`{}\\[\\]:;'<>?,.\\-]+)",
	date: "(\\d{4}-\\d{1,2}-\\d{1,2})",
	decimal: "(\\-?\\d+(\\\.\\d+)?)",
	path: "([a-z0-9 !@#$%^&*()_+=`{}\\[\\]:;'<>?,.\\\\\\/\\-]+)",
	csv: "([%a-z0-9\\-,]+)",
}

var paramRegex = /\((int|slug|date|decimal|string|csv|path)\:([a-z0-9\-]+)\)/i

function createRegex(pattern)
{
	pattern = pattern.replace("/", "\\/")
		.replace("[", "(:?")
		.replace("]", ")?")
		.replace(/\(int\:([a-z0-9\-]+)\)/ig, commonPatterns.int)
		.replace(/\(slug\:([a-z0-9\-]+)\)/ig, commonPatterns.slug)
		.replace(/\(string\:([a-z0-9\-]+)\)/ig, commonPatterns.string)
		.replace(/\(path\:([a-z0-9\/\-]+)\)/ig, commonPatterns.path)
		.replace(/\(csv\:([a-z0-9\-]+)\)/ig, commonPatterns.csv)
		.replace(/\(date\:([a-z0-9\-]+)\)/ig, commonPatterns.date)
		.replace(/\(decimal\:([a-z0-9\-]+)\)/ig, commonPatterns.decimal)
	;
	return new RegExp("^" + pattern + "$", "i");
}

function createBuilder(pattern, nullReplacements)
{
	var builderTemplate = pattern;
	var urlParams = [];
	var paramTypes = [];
	var match = paramRegex.exec(builderTemplate);
	while (match != null)
	{
		var paramType = match[1];
		var paramName = match[2];
		urlParams.push(paramName);
		paramTypes.push(paramType);
		builderTemplate = builderTemplate.replace(match[0], "{" + paramName + "}");
		match = paramRegex.exec(builderTemplate);
	}

	return function(obj)
	{
		var returnValue = builderTemplate;
		obj = obj || {};
		nullReplacements = nullReplacements || {};
		for(var i = 0; i < urlParams.length; i++) {
			var replacementValue = obj[urlParams[i]];
			if (replacementValue === undefined || replacementValue === null) {
				replacementValue = nullReplacements[urlParams[i]];
				if (replacementValue === undefined || replacementValue === null) {
					switch(paramTypes[i]) {
						case "int":
						case "decimal":
						case "csv":
							replacementValue = "0";
							break;
						case "slug":
						case "string":
						case "path":
							replacementValue = "undefined";
							break;
						case "date":
							replacementValue = "0000-00-00";
							break;
					}
				}
			}
			returnValue = returnValue.replace("{" + urlParams[i] + "}", encodeURIComponent(replacementValue));
		}
		return returnValue;
	}
}

function createScraper(pattern)
{
	var regex = createRegex(pattern);
	var builderTemplate = pattern;
	var urlParams = [];

	var match = paramRegex.exec(builderTemplate);
	while (match != null)
	{
		var paramType = match[1];
		var paramName = match[2];
		urlParams.push(paramName);
		builderTemplate = builderTemplate.replace(match[0], "{" + paramName + "}");
		match = paramRegex.exec(builderTemplate);
	}

	return function(path)
	{
		var matches = regex.exec(path) || [];
		var values = {};
		for (var i = 0; i < urlParams.length && i < matches.length; i++)
		{
			values[urlParams[i]] = matches[i + 1];
		}
		return values;
	}
}


*/

}
