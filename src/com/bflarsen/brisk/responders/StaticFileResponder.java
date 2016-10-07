package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;
import com.bflarsen.util.FileStatCache;

import static com.bflarsen.util.Logger.*;

import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class StaticFileResponder implements HttpResponder {
    public static SimpleDateFormat getModifiedSinceDateParser() {
        SimpleDateFormat parser = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
        parser.setTimeZone(TimeZone.getTimeZone("GMT"));
        return parser;
    }

    public Path BasePath;

    public StaticFileResponder(Path baseDirectory) {
        this.BasePath = baseDirectory;
    }

    public static Factory createFactory(Path path) throws Exception {
        if (!Files.isDirectory(path))
            throw new Exception("'" + path.toString() + "' is not a directory. CWD:" + Paths.get(".").toAbsolutePath().normalize().toString());
        final StaticFileResponder responder = new StaticFileResponder(path);
        return () -> responder;  // the static file responder is stateless enough that a single instance can handle many responses
    }

    @Override
    public boolean canHandle(HttpContext context) {
        // don't ever serve up "dot" files
        if (Paths.get(context.Request.Path).getFileName().startsWith(".")) {
            return false;
        }
        String path = Paths.get(BasePath.toString(), context.Request.Path).toString();
        return context.Server.FileCache.get(path).isReadable;
    }

    @Override
    public HttpResponse respond(HttpContext context) throws Exception {
        String path = Paths.get(BasePath.toString(), context.Request.Path).toString();
        FileStatCache.FileStat file = context.Server.FileCache.get(path);

        // check for not-exists response
        if (!file.exists) {
            return context.Server.Error404ResponderFactory.create().respond(context);
        }

        // check for opportunity to send a not-modified response
        String ifModifiedSince = context.Request.Headers.get("Request_IfModifiedSince");
        long modifiedSince = -1;
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            try {
                // I give 1000 milliseconds of leeway here, because the http protocol passes a date rounded to the second.
                modifiedSince = getModifiedSinceDateParser().parse(ifModifiedSince).getTime() + 1000;
            }
            catch (Exception ex) {
                logEx(ex, "StaticFileResponder", "respond", "parsing ModifiedSince header: '" + ifModifiedSince + "'");
            }
        }
        if (file.whenModified <= modifiedSince) {
            return new SimpleStatusResponse(HttpStatusCode.NOT_MODIFIED);
        }

        // respond with a normal file response
        return new FileResponse(file.absolutePath, context.Server.FileCache);
    }
}

