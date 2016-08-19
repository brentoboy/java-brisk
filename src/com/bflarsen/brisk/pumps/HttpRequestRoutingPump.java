package com.bflarsen.brisk.pumps;

public class HttpRequestRoutingPump implements Runnable {
/*

var JavaException = require('libraries/exception.js');
var Str = require('core/string.js');
var List = require('core/list.js');
var Dict = require('core/dictionary.js');
var Regex = require('core/regularExpression.js');
var Thread = require('libraries/thread.js');
var Util = require('core/utilities.js');
var Workflow = require('framework/workflow.js');
var UrlPatterns = require('framework/urlPatterns.js');

module.exports = {
	spawnWorkers: spawnWorkers,
	chooseAction: chooseAction,
};

var sites = require('sites.js');
var defaultSite;

function byLength(a,b) {
	return a.length - b.length;
}
function byLengthDescending(a,b) {
	return b.length - a.length;
}

(function initSites()
{
	var i, site, routes;
	var defaultSiteName = sites[0];

	sites = List.sort(sites, byLengthDescending);

	for (i = 0; i < sites.length; i++)
	{
		var site = sites[i];
		var routes = tryRequire(site + "/routes.js");

		if ( ! routes)
		{
			routes = {};
			console.log("Site: '" + site + "' doesn't define any routes.\n\tRoute file should be located at:" + site + "/routes.js");
		}

		sites[i] = {
			name: site,
			routes: routes,
		};

		if (site == defaultSiteName)
		{
			defaultSite = sites[i];
		}

		routes._dev = routes._dev || "/dev/(path:theWidget)";
		routes._ajax = routes._ajax || "/ajax/(path:theWidget)";

		for(routeKey in routes)
		{
			if (Util.isStr(routes[routeKey]))
			{
				routes[routeKey] = {
					action: routeKey,
					pattern: routes[routeKey],
				};
			}
			var route = routes[routeKey];

			// if they defined a pattern instead of a regex, make a regex out of it
			if (route.pattern && !route.regex)
			{
				route.regex = UrlPatterns.createRegex(route.pattern)
			}

			// if a route has no getUrl function create one for it
			if (route.pattern && !route.getUrl)
			{
				route.getUrl = UrlPatterns.createBuilder(route.pattern)
			}

			if (route.pattern && !route.scrape)
			{
				route.scrape = UrlPatterns.createScraper(route.pattern)
			}
		}

		if (!routes._static)
		{
			routes._static = {
				regex: null,
				action: "_static",
				getUrl: function() {},
				scrape: function() {},
			};
		}

		if (!routes._404)
		{
			routes._404 = {
				regex: null,
				action: "_404",
				getUrl: function() {},
				scrape: function() {},
			};
		}
	}
}());



function workerThread()
{
	while ( ! Thread.interrupted())
	{
		var context = null;
		try
		{
			Thread.yield();
			context = Workflow.parsedRequests.take();
			context.stats.requestRouterStarted = Util.getMoment();
			chooseAction(context);
		}
		catch (ex if ex instanceof Thread.InterruptedException)
		{
			return;
		}
		catch (ex if ex instanceof JavaException)
		{
			console.log(JavaException.format(ex));
		}
		catch (e)
		{
			console.log(e);
			console.log(e.stack);
		}
		finally
		{
			if (context)
			{
				context.stats.requestRouterEnded = Util.getMoment();
				Workflow.routedRequests.add(context);
			}
		}
	}
}

function spawnWorkers(count)
{
	count = count || 4;
	Thread.spawn(workerThread, count);
}

function chooseAction(context)
{
	var i, route, routeKey;

	// first choose a website
	context.site = defaultSite; // unless we find a match
	List.each(sites, function(site) {
		if (Str.endsWith(context.request.host, site.name))
		{
			context.site = site;
			return List.Break;
		}
	});

	// next choose a route
	context.route = context.site.routes._static; // unless we find a match
	Dict.each(context.site.routes, function(key, route) {
		if (Regex.isMatch(route.regex, context.request.path))
		{
			context.route = route;
			return List.Break;
		}
	});

	// then choose an action
	context.action = context.route.action;
}

--------------tests
var Assert = require('core/assert.js');
var List = require('core/list.js');
var Dict = require('core/dictionary.js');
var Util = require('core/utilities.js');
var RequestRouter = require("framework/requestRouter.js");

exports.chooseSite = {
	"should use the first site in sites.js when no match is found":
	function testDefaultHostname() {
		var hostnames = [
			"localhost"
			, "127.0.0.1"
			, "192.168.0.1"
			, "bla"
		];

		List.each(hostnames, function(hostname) {
			var context = { request: { host: hostname, path: "/" } };
			RequestRouter.chooseAction(context);
			Assert.equals("bflarsen.org", context.site.name);
		});
	},

	"should match sites based on request.host":
	function testMatchingHosts() {
		var hostnames = [
			"bflarsen.org",
			"gamanac.com",
			"test.site",
			"test2.test.site",
		];

		List.each(hostnames, function(hostname) {
			var context = { request: { host: hostname, path: "/" } };
			RequestRouter.chooseAction(context);
			Assert.equals(hostname, context.site.name);
		});
	},

	"should match subdomains to the parent domain when subdomain doesnt exist":
	function testSubdomainDefaultToParentDomain() {
		var targetDomains = {
			"bflarsen.org": [
				"bflarsen.org",
				"www.bflarsen.org",
				"dev.bflarsen.org",
				"test.bflarsen.org",
				"lots.of.subdomains.bflarsen.org",
				"www4.bflarsen.org",
			],
			"gamanac.com": [
				"gamanac.org",
				"www.gamanac.org",
				"dev.gamanac.org",
				"test.gamanac.org",
				"lots.of.subdomains.gamanac.org",
				"www4.gamanac.org",
			],
			"test.site": [
				"test.site",
				"www.test.site",
				"you-get-the-idea.test.site",
			],
			"test2.test.site": [
				"test2.test.site",
				"local.test2.test.site",
				"dev.test2.test.site",
			],
		};

		List.each(targetDomains.keys, function(targetDomain) {
			List.each(targetDomains[targetDomain], function (hostname) {
				var context = { request: { host: hostname, path: "/" } };
				RequestRouter.chooseAction(context);
				Assert.equals(targetDomain, context.site.name);
			});
		});
	},

	"should match subdomains to subdomain when it does exist":
	function() {
		// testing this behaviors is satisfied by the test2.test.site tests from the last test.
	},
};

exports.chooseAction = {
	"should find matches in the route table":
	function testExistingRoutes() {
		var sampleResources = {
			"homePage": [
				"/",
			],
			"itemListPage": [
				"/items/list.html",
			],
			"itemPage": [
				"/items/0.html",
				"/items/1.html",
				"/items/2.html",
				"/items/99999.html",
			],
		};
		Dict.each(sampleResources, function(targetAction, urls) {
			List.each(urls, function (resource) {
				var context = { request: { host: "test.site", path: resource } };
				RequestRouter.chooseAction(context);
				Assert.equals("test.site", context.site.name);
				Assert.equals(targetAction, context.action);
			});
		});
	},

	"should default to _static when no match is found":
	function testStatic()
	{
		var resources = [
			"/non-existant-resource",
			"/js/somefile.js",
			"/img/my-ugly-mug.png",
			"/img/animated.gif",
			"/favicon.ico",
		];
		List.each(resources, function(resource) {
			var context = { request: { host: "test.site", path: resource } };
			RequestRouter.chooseAction(context);
			Assert.equals("_static", context.route.action);
		});
	},

	"magical _dev route should work":
	function test_devRoute()
	{
		var resources = [
			"/dev/some/widget",
			"/dev/path/to/widget",
			"/dev/widget-x",
			"/dev/myWidget",
		];
		List.each(resources, function(resource) {
			var context = { request: { host: "test.site", path: resource } };
			RequestRouter.chooseAction(context);
			Assert.equals("_dev", context.route.action);
		});
	},

	"magical _ajax route should work":
	function test_ajaxRoute()
	{
		var resources = [
			"/ajax/some/widget",
			"/ajax/path/to/widget",
			"/ajax/widget-x",
			"/ajax/myWidget",
		];
		List.each(resources, function(resource) {
			var context = { request: { host: "test.site", path: resource } };
			RequestRouter.chooseAction(context);
			Assert.equals("_ajax", context.route.action);
		});
	},
};




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

module.exports = {
	createRegex: createRegex,
	createBuilder: createBuilder,
	createScraper: createScraper,
};


*/

}
