package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responders.*;
import static com.bflarsen.util.Logger.*;


public class HttpResponseBuildingPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpResponseBuildingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfResponseBuildingThreadsToCreate];
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

    public static void buildResponse(HttpContext context) throws Exception {
        if (context.Responder == null)
            throw new Exception("Responder is null in buildResponse");

        try {
            context.Response = context.Responder.respond(context);
        }
        catch(Exception ex) {
            logEx(ex, "HttpResponseBuildingPump", "buildResponse", "attempting to build response of type " + context.Responder.getClass().getSimpleName());
            context.ResponderException = ex;
            HttpResponder responder = context.Server.Error500ResponderFactory.create();
            if (responder == null) {
                responder = new DefaultError500Responder();
            }
            try {
                context.Response = responder.respond(context);
            }
            catch (Exception ex2) {
                logEx(ex2, "HttpResponseBuildingPump", "buildResponse", "attempting to build an exception response of type " + context.Responder.getClass().getSimpleName());
                responder = new DefaultError500Responder();
                try {
                    context.Response = responder.respond(context);
                }
                catch (Exception ex3) {
                    // yeah, ... about that.   I've got no more ideas
                    logEx(ex2, "HttpResponseBuildingPump", "buildResponse", "attempting to build the default exception response.");
                }
            }
        }
    }

    private static class Worker extends Thread {
        HttpResponseBuildingPump parentPump;

        Worker(HttpResponseBuildingPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.RoutedRequests.take();
                    context.Stats.ResponseBuilderStarted = System.nanoTime();
                    buildResponse(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
                finally {
                    if (context != null) {
                        context.Stats.ResponseBuilderEnded = System.nanoTime();
                        parentPump.httpServerInstance.ResponseReady.add(context);
                    }
                }
            }
        }
    }


/*

			context = Workflow.routedRequests.take();
			context.stats.responseBuilderStarted = Util.getMoment();
			context.responseStream = context.socket.getOutputStream();
			context.responseWriter = new JavaPrintWriter(context.responseStream, true);
			buildResponse(context);

function buildResponse(context) {
	var response = context.response = context.response || {};

	if (context.action == "_static") {
		var in24Hours = new Date();
		in24Hours.setTime(in24Hours.getTime() + 24 * 3600 * 1000);

		if (fileCache[context.request.resource])
		{
			var cachedVersion = fileCache[context.request.resource];
			response.headers = {};
			response.headers["Content-Type"] = cachedVersion.contentType;
			response.headers["Content-Length"] = cachedVersion.contentLength;
			response.headers["Last-Modified"] = cachedVersion.lastModified;
			response.headers["Expires"] =  in24Hours.toUTCString();
			response.bodyBytes = cachedVersion.buffer;
			//console.log("Using statCache: " + context.request.resource);
		}
		else
		{
			var file = new JavaFile(context.root + context.site.name + "/static" + context.request.resource);
			if ( ! file.exists() || ! file.canRead() || file.isDirectory()) {
				file = new JavaFile(context.root + "shared/static" + context.request.resource);
			}

			if (file.exists() && file.canRead() && !file.isDirectory()) {
				response.file = file;
				response.headers = {};
				response.headers["Content-Type"] = lookupMimeType(context.request.resource);
				response.headers["Content-Length"] = response.file.length().toString();
				response.headers["Last-Modified"] = new Date(response.file.lastModified()).toUTCString();
				response.headers["Expires"] =  in24Hours.toUTCString();

				if (response.file.length() < 8196) {
					//console.log("Adding to statCache: " + context.request.resource);
					var buffer = JavaFiles.readAllBytes(file.toPath());
					fileCache[context.request.resource] = {
						contentType: response.headers["Content-Type"],
						contentLength: response.headers["Content-Length"],
						lastModified: response.headers["Last-Modified"],
						buffer: buffer,
					};
					response.bodyBytes = buffer;
				}
				else {
					response.sendBody = function(sendStream) {
						try {
							var fileStream = new JavaFileInputStream(file);
							var buffer = new JavaByteArray(4096);
							var readCount = 0;
							while ((readCount = fileStream.read(buffer)) != -1) {
								sendStream.write(buffer, 0, readCount);
							}
						}
						catch (ex if ex instanceof JavaException) {
							console.log(JavaException.format(ex));
						}
						catch (ex) {
							console.log(ex.message);
						}
						finally {
							Safe.close(fileStream);
						}
					};
				}
			}
			else {
				response.statusCode = 404;
			}
		}
	}
}

function lookupMimeType(filename) {
	if (filename) {
		var dotPos = filename.lastIndexOf(".");
		if (dotPos >= 0) {
			var ext = filename.substring(dotPos + 1);
			return mimeTypes[ext] || "application/octet-stream";
		}
	}
	return filename;
}

var mimeTypes = {
	"323": "text/h323",
	"acx": "application/internet-property-stream",
	"ai": "application/postscript",
	"aif": "audio/x-aiff",
	"aifc": "audio/x-aiff",
	"aiff": "audio/x-aiff",
	"asf": "video/x-ms-asf",
	"asr": "video/x-ms-asf",
	"asx": "video/x-ms-asf",
	"au": "audio/basic",
	"avi": "video/x-msvideo",
	"axs": "application/olescript",
	"bas": "text/plain",
	"bcpio": "application/x-bcpio",
	"bin": "application/octet-stream",
	"bmp": "image/bmp",
	"c": "text/plain",
	"cat": "application/vnd.ms-pkiseccat",
	"cdf": "application/x-netcdf",
	"cer": "application/x-x509-ca-cert",
	"class": "application/octet-stream",
	"clp": "application/x-msclip",
	"cmx": "image/x-cmx",
	"cod": "image/cis-cod",
	"cpio": "application/x-cpio",
	"crd": "application/x-mscardfile",
	"crl": "application/pkix-crl",
	"crt": "application/x-x509-ca-cert",
	"csh": "application/x-csh",
	"css": "text/css",
	"dcr": "application/x-director",
	"der": "application/x-x509-ca-cert",
	"dir": "application/x-director",
	"dll": "application/x-msdownload",
	"dms": "application/octet-stream",
	"doc": "application/msword",
	"dot": "application/msword",
	"dvi": "application/x-dvi",
	"dxr": "application/x-director",
	"eps": "application/postscript",
	"etx": "text/x-setext",
	"evy": "application/envoy",
	"exe": "application/octet-stream",
	"fif": "application/fractals",
	"flr": "x-world/x-vrml",
	"gif": "image/gif",
	"gtar": "application/x-gtar",
	"gz": "application/x-gzip",
	"h": "text/plain",
	"hdf": "application/x-hdf",
	"hlp": "application/winhlp",
	"hqx": "application/mac-binhex40",
	"hta": "application/hta",
	"htc": "text/x-component",
	"htm": "text/html",
	"html": "text/html",
	"htt": "text/webviewhtml",
	"ico": "image/x-icon",
	"ief": "image/ief",
	"iii": "application/x-iphone",
	"ins": "application/x-internet-signup",
	"isp": "application/x-internet-signup",
	"jfif": "image/pipeg",
	"jpe": "image/jpeg",
	"jpeg": "image/jpeg",
	"jpg": "image/jpeg",
	"js": "application/x-javascript",
	"latex": "application/x-latex",
	"less": "text/css",
	"lha": "application/octet-stream",
	"lsf": "video/x-la-asf",
	"lsx": "video/x-la-asf",
	"lzh": "application/octet-stream",
	"m13": "application/x-msmediaview",
	"m14": "application/x-msmediaview",
	"m3u": "audio/x-mpegurl",
	"man": "application/x-troff-man",
	"mdb": "application/x-msaccess",
	"me": "application/x-troff-me",
	"mht": "message/rfc822",
	"mhtml": "message/rfc822",
	"mid": "audio/mid",
	"mny": "application/x-msmoney",
	"mov": "video/quicktime",
	"movie": "video/x-sgi-movie",
	"mp2": "video/mpeg",
	"mp3": "audio/mpeg",
	"mpa": "video/mpeg",
	"mpe": "video/mpeg",
	"mpeg": "video/mpeg",
	"mpg": "video/mpeg",
	"mpp": "application/vnd.ms-project",
	"mpv2": "video/mpeg",
	"ms": "application/x-troff-ms",
	"msg": "application/vnd.ms-outlook",
	"mvb": "application/x-msmediaview",
	"nc": "application/x-netcdf",
	"nws": "message/rfc822",
	"oda": "application/oda",
	"p10": "application/pkcs10",
	"p12": "application/x-pkcs12",
	"p7b": "application/x-pkcs7-certificates",
	"p7c": "application/x-pkcs7-mime",
	"p7m": "application/x-pkcs7-mime",
	"p7r": "application/x-pkcs7-certreqresp",
	"p7s": "application/x-pkcs7-signature",
	"pbm": "image/x-portable-bitmap",
	"pdf": "application/pdf",
	"pfx": "application/x-pkcs12",
	"pgm": "image/x-portable-graymap",
	"pko": "application/ynd.ms-pkipko",
	"pma": "application/x-perfmon",
	"pmc": "application/x-perfmon",
	"pml": "application/x-perfmon",
	"pmr": "application/x-perfmon",
	"pmw": "application/x-perfmon",
	"pnm": "image/x-portable-anymap",
	"png": "image/png",
	"pot": "application/vnd.ms-powerpoint",
	"ppm": "image/x-portable-pixmap",
	"pps": "application/vnd.ms-powerpoint",
	"ppt": "application/vnd.ms-powerpoint",
	"prf": "application/pics-rules",
	"ps": "application/postscript",
	"pub": "application/x-mspublisher",
	"qt": "video/quicktime",
	"ra": "audio/x-pn-realaudio",
	"ram": "audio/x-pn-realaudio",
	"ras": "image/x-cmu-raster",
	"rgb": "image/x-rgb",
	"rmi": "audio/mid",
	"roff": "application/x-troff",
	"rtf": "application/rtf",
	"rtx": "text/richtext",
	"scd": "application/x-msschedule",
	"sct": "text/scriptlet",
	"setpay": "application/set-payment-initiation",
	"setreg": "application/set-registration-initiation",
	"sh": "application/x-sh",
	"shar": "application/x-shar",
	"sit": "application/x-stuffit",
	"snd": "audio/basic",
	"spc": "application/x-pkcs7-certificates",
	"spl": "application/futuresplash",
	"src": "application/x-wais-source",
	"sst": "application/vnd.ms-pkicertstore",
	"stl": "application/vnd.ms-pkistl",
	"stm": "text/html",
	"sv4cpio": "application/x-sv4cpio",
	"sv4crc": "application/x-sv4crc",
	"svg": "image/svg+xml",
	"swf": "application/x-shockwave-flash",
	"t": "application/x-troff",
	"tar": "application/x-tar",
	"tcl": "application/x-tcl",
	"tex": "application/x-tex",
	"texi": "application/x-texinfo",
	"texinfo": "application/x-texinfo",
	"tgz": "application/x-compressed",
	"tif": "image/tiff",
	"tiff": "image/tiff",
	"tr": "application/x-troff",
	"trm": "application/x-msterminal",
	"tsv": "text/tab-separated-values",
	"txt": "text/plain",
	"uls": "text/iuls",
	"ustar": "application/x-ustar",
	"vcf": "text/x-vcard",
	"vrml": "x-world/x-vrml",
	"wav": "audio/x-wav",
	"wcm": "application/vnd.ms-works",
	"wdb": "application/vnd.ms-works",
	"wks": "application/vnd.ms-works",
	"wmf": "application/x-msmetafile",
	"wps": "application/vnd.ms-works",
	"wri": "application/x-mswrite",
	"wrl": "x-world/x-vrml",
	"wrz": "x-world/x-vrml",
	"xaf": "x-world/x-vrml",
	"xbm": "image/x-xbitmap",
	"xla": "application/vnd.ms-excel",
	"xlc": "application/vnd.ms-excel",
	"xlm": "application/vnd.ms-excel",
	"xls": "application/vnd.ms-excel",
	"xlt": "application/vnd.ms-excel",
	"xlw": "application/vnd.ms-excel",
	"xml": "application/xml",
	"xof": "x-world/x-vrml",
	"xpm": "image/x-xpixmap",
	"xwd": "image/x-xwindowdump",
	"z": "application/x-compress",
	"zip": "application/zip",
};
------------------tests



*/
}
