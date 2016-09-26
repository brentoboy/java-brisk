package com.bflarsen.brisk.responses;


import com.bflarsen.util.FileStatCache;

import java.nio.file.Files;
import java.nio.file.Paths;


public class TempFileResponse extends FileResponse {

    public TempFileResponse(String filePath) {
        super(filePath, new FileStatCache());
        // dont store anything in the cache (we only use the cache so that all files are streamed the same way)
        this.cache.CONTENT_CACHE_MAX_FILE_SIZE = 1;
        this.cache.CONTENT_CACHE_MAX_SIZE = 1;
    }

    @Override
    public void close() throws Exception {
        if (this.fileInfo != null && this.fileInfo.absolutePath != null) {
            Files.deleteIfExists(Paths.get(this.fileInfo.absolutePath));
        }
        super.close();
    }
}
