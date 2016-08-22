package com.bflarsen.brisk.responses;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class FileResponse extends BaseResponse {

    Path path;
    byte[] buffer;
    Date lastModified;

    public FileResponse(Path filePath) {
        super(200);
        this.path = filePath;
        try {
            this.lastModified = new Date(Files.getLastModifiedTime(this.path).toMillis());
        } catch (Exception ex) {}
        try {
            this.buffer = Files.readAllBytes(this.path);
        } catch (Exception ex) {}

        this.setHeader("Content-Type", lookupMimeType(this.path.getFileName().toString()));
        this.setHeader("Last-Modified", UtcFormatter.format(this.lastModified));
        // response.headers["Expires"] =  in24Hours.toUTCString();
    }

    @Override
    public byte[] getBodyBytes() throws Exception {
        return this.buffer;
    }
}
