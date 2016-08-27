package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.BaseResponse;
import com.bflarsen.brisk.responses.FileResponse;
import com.bflarsen.brisk.responses.PlainTextResponse;

import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class StaticFileResponder implements HttpResponder {
    public static final SimpleDateFormat modifiedSinceDateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz");
    { modifiedSinceDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT")); }

    public Path BasePath;

    public StaticFileResponder(Path baseDirectory) {
        this.BasePath = baseDirectory;
    }

    public static HttpResponder.Factory createFactory(Path path) throws Exception {
        if (!Files.isDirectory(path))
            throw new Exception("'" + path.toString() + "' is not a directory.");

        final Path baseFolder = path;

        // TODO: you could have one instance handle all of these, and not create a new object instance every time.
        return () -> new StaticFileResponder(baseFolder);
    }

    @Override
    public boolean canHandle(HttpContext context) {
        String path = Paths.get(BasePath.toString(), context.Request.Resource).toString();
        return context.Server.FileCache.get(path).isReadable;
    }

    @Override
    public HttpResponse respond(HttpContext context) throws Exception {
        String path = Paths.get(BasePath.toString(), context.Request.Resource).toString();
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
                modifiedSince = modifiedSinceDateFormatter.parse(ifModifiedSince).getTime() + 1000;
            }
            catch (Exception ex) {
                context.Server.ExceptionHandler(ex, "StaticFileResponder", "respond", "parsing ModifiedSince header");
            }
        }
        if (file.whenModified <= modifiedSince) {
            BaseResponse response = new PlainTextResponse("Not Modified");
            response.StatusCode = 304;
            return response;
        }

        // context.Server.LogHandler(String.format("File '%s' has been modified (%d <= %d)", path, modifiedSince, file.whenModified));

        // respond with a normal file response
        return new FileResponse(file.absolutePath, context.Server.FileCache);
    }
}

