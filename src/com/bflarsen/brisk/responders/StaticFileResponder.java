package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.responses.FileResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileResponder implements HttpResponder {

    public Path BasePath;

    public StaticFileResponder(Path baseDirectory) {
        this.BasePath = baseDirectory;
    }

    public static HttpResponder.Factory createFactory(Path path) throws Exception {
        if (!Files.isDirectory(path))
            throw new Exception("'" + path.toString() + "' is not a directory.");

        final Path baseFolder = path;

        return () -> new StaticFileResponder(baseFolder);
    }

    @Override
    public boolean canHandle(HttpContext context) {
        return Files.isReadable(Paths.get(BasePath.toString(), context.Request.Resource));
    }

    @Override
    public HttpResponse handleRequest(HttpContext context) {
        return new FileResponse(Paths.get(BasePath.toString(), context.Request.Resource));
    }
}

