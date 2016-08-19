package com.bflarsen.brisk.pumps;

public class HttpContextCleanupPump implements Runnable {

/*
var JavaException = require('libraries/exception.js');
var Thread = require('libraries/thread.js');
var Util = require('core/utilities.js');
var Workflow = require('framework/workflow.js');

module.exports = {
	spawnWorkers: spawnWorkers,
	performCleanup: performCleanup,
}


function workerThread() {
	while ( ! Thread.interrupted())
	{
		var context = null;
		try {
			Thread.yield();
			context = Workflow.doneSending.take();
			context.stats.cleanupStarted = Util.getMoment();
			performCleanup(context);
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
		// we dont put it on a new queue, this context is done. it can be GC'd
		//context.stats.cleanupEnded = Util.getMoment();
	}
}

function catchAll() {
	while ( ! Thread.interrupted()) {
		var nextContext = null;
		var context = null;
		var now = Util.getMoment();
		var oldestAllowedStartTime = now - (60 * 100000000); // 60 seconds ago
		try {
			Thread.sleep(500);

			// flush out everything that has already completed successfully
			nextContext = Workflow.allRequests.peek();
			while (nextContext && nextContext.stats.completelyFinished) {
				context = Workflow.allRequests.take();
				if (context != nextContext) {
					console.log("catchAll context != nextContext ... that's totally unexpected and means something is broken!");
				}
				context = null;
				nextContext = Workflow.allRequests.peek();
			}

			while (nextContext && nextContext.stats.requestParserStarted < oldestAllowedStartTime) {
				context = Workflow.allRequests.take();
				if (context != nextContext) {
					console.log("catchAll context != nextContext ... that's totally unexpected and means something is broken -2");
				}
				context.stats.expired = Workflow.now
				Workflow.doneSending.add(context);
				context = null;
				console.log("request expired before cleanup!!!!");
				nextContext = Workflow.allRequests.peek();
			}
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
	}
}

function spawnWorkers(count) {
	count = count || 4;
	Thread.spawn(workerThread, count);
	Thread.spawn(catchAll, 1);
}

function performCleanup(context) {
	if (context) {
		//TODO: what about persistent connections / keep-alive stuff?

		// resource cleanup
		Util.close(
			context.requestStream
			, context.responseStream
			, context.socket
		);

		context.stats.completelyFinished = Util.getMoment();
		context.stats.totalMs = Math.round((context.stats.completelyFinished - context.stats.requestParserStarted) / 1000000);

		// logging
		context.request = context.request || {}

		if (context.request && context.request.resource) {
			console.log((context.request.method || "???") + " " + (context.request.resource || "???"));
			if (context.stats.completelyFinished - context.stats.requestParserStarted > 5000000000) { //5 seconds is long!
				console.log("super long request/response cycle: " + Math.round((context.stats.completelyFinished - context.stats.requestParserStarted) / 1000000) + "ms \t" + context.request.httpMethod + " " + context.request.resource);
			}
			if (!context.stats.ttlb) {
				console.log("no ttlb - that's really bad");
			}
		}
	}
}
 */
}
