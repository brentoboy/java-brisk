package com.bflarsen.brisk.pumps;

public class HttpResponseSendingPump implements Runnable {

/*

var JavaException = require('libraries/exception.js');
var Thread = require('libraries/thread.js');
var Util = require('core/utilities.js');
var Workflow = require('framework/workflow.js');

module.exports = {
	spawnWorkers: spawnWorkers,
	sendResponse: sendResponse,
	lookupStatusDescription: lookupStatusDescription,
};

function workerThread() {
	while ( ! Thread.interrupted())
	{
		var context = null;
		try {
			Thread.yield();
			context = Workflow.responseReady.take();
			context.stats.responseSenderStarted = Util.getMoment();
			context.responseStream = context.socket.getOutputStream();
			context.responseWriter = new java.io.PrintWriter(context.responseStream, true);
			sendResponse(context);
		}
		catch (ex if ex instanceof Thread.InterruptedException) {
			return;
		}
		catch (ex if ex instanceof JavaException) {
			console.log(JavaException.format(ex));
		}
		catch (e) {
			console.log(e);
			console.log(e.stack);
		}
		finally {
			if (context) {
				context.stats.responseSenderEnded = Util.getMoment();
				Workflow.doneSending.add(context);
			}
		}
	}
}

function spawnWorkers(count) {
	count = count || 4;
	Thread.spawn(workerThread, count);
}

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
	// if they ask for persistant connections
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

function lookupStatusDescription(statusCode) {
	return statusDescriptions[statusCode] || "Unknown Status";
}

var statusDescriptions = {
	"200": "OK",
	"201": "Created",
	"202": "Accepted",
	"204": "No Content",
	"301": "Moved Permanently",
	"302": "Found",
	"304": "Not Modified",
	"400": "Bad Request",
	"401": "Unauthorized",
	"402": "Payment Required",
	"403": "Forbidden",
	"404": "Not Found",
	"405": "Method Not Allowed",
	"406": "Not Acceptable",
	"408": "Request Timeout",
	"411": "Length Required",
	"413": "Request Entity Too Large",
	"414": "Request-URI Too Long",
	"418": "I'm a teapot",
	"500": "Internal Server Error",
	"501": "Not Implemented",
	"502": "Bad Gateway",
	"503": "Service Unavailable",
	"504": "Gateway Timeout",
	"505": "HTTP Version Not Supported",
};




 */

}
