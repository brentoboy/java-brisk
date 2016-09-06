package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.HttpStatusCode;
import com.bflarsen.brisk.MimeType;
import com.bflarsen.util.FileStatCache;

import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Date;

public class FileResponse extends BaseResponse {

    FileStatCache cache;
    FileStatCache.FileStat fileInfo;

    public FileResponse(String filePath, FileStatCache cache) {
        super(HttpStatusCode.OK, MimeType.lookupByExtension(Paths.get(filePath).getFileName().toString()));
        this.cache = cache;
        this.fileInfo = cache.get(filePath);
// todo: throw some kind of exception if the file doesnt exist.  at least log something
        this.setHeader("Last-Modified", UtcFormatter.format(new Date(this.fileInfo.whenModified)));
        this.setHeader("Expires", UtcFormatter.format(new Date(System.currentTimeMillis() + 24*60*60*1000L))); // 24 hours
    }

    @Override
    public void sendBody(OutputStream os) throws Exception {
        this.cache.streamTo(this.fileInfo.path, os);
    }

    @Override
    public Long getContentLength() {
        return this.fileInfo.size;
    }
}
