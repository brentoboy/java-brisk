package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.FileResponse;

import java.nio.file.*;

public class StaticFileResponder implements HttpResponder {

    public Path BasePath;

    public StaticFileResponder(Path baseDirectory) {
        this.BasePath = baseDirectory;
    }

    public static HttpResponder.Factory createFactory(Path path) throws Exception {
        if (!Files.isDirectory(path))
            throw new Exception("'" + path.toString() + "' is not a directory. CWD:" + Paths.get(".").toAbsolutePath().normalize().toString());

        final Path baseFolder = path;

        // TODO: you could have one instance handle all of these, and not create a new object instance every time.
        return () -> new StaticFileResponder(baseFolder);
    }

    @Override
    public boolean canHandle(HttpContext context) {
        return Files.isReadable(Paths.get(BasePath.toString(), context.Request.Resource));
    }

    @Override
    public HttpResponse respond(HttpContext context) throws Exception {
        // TODO: send a NotModified Response when appropriate
        return new FileResponse(Paths.get(BasePath.toString(), context.Request.Resource));
    }
}

