package com.bflarsen.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileStatCache {
    public static final int KB = 1024;
    public static final int MB = KB*KB;
    public static final long GB = KB*KB*KB;
    public static final long TB = (long)KB*KB*KB*KB;

    public static String formatFileSize(Long size) {
        if (size == null)
            return "";
        if (size >= 10*TB)
            return String.format("%.0f TB", (Math.round((float)size / TB * 10f) / 10f));
        if (size >= 0.9*TB)
            return String.format("%.1f TB", (Math.round((float)size / TB * 10f) / 10f));
        if (size >= 10*GB)
            return String.format("%.0f GB", (Math.round((float)size / GB * 10f) / 10f));
        if (size >= 0.9*GB)
            return String.format("%.1f GB", (Math.round((float)size / GB * 10f) / 10f));
        if (size >= 10*MB)
            return String.format("%.0f MB", (Math.round((float)size / MB * 10f) / 10f));
        if (size >= 0.9*MB)
            return String.format("%.1f MB", (Math.round((float)size / MB * 10f) / 10f));
        if (size >= 10*KB)
            return String.format("%.0f KB", (Math.round((float)size / KB * 10f) / 10f));
        if (size >= 0.9*KB)
            return String.format("%.1f KB", (Math.round((float)size / KB * 10f) / 10f));
        else
            return String.format("%d Bytes", size);
    }

    public final Map<String, FileStat> statCache = new ConcurrentHashMap<>();
    public long statsExpireAfter = 60000L; // one minute

    public long CONTENT_CACHE_MAX_SIZE = 16*MB;
    public int CONTENT_CACHE_MAX_FILE_SIZE = 24*KB;
    public final Map<String, byte[]> contentCache = new ConcurrentHashMap<>();
    public long contentCacheTotalBytes = 0l;

    public static class FileStat {
        public String problems;
        public long whenLastChecked;
        public long whenCreated;
        public long whenModified;
        public long whenAccessed;
        public long size;
        public boolean exists;
        public boolean isReadable;
        public boolean isDirectory;
        public boolean isRegularFile;
        public boolean isLink;
        public boolean isOther;
        public String absolutePath;
        public String path;

        public FileStat(String pathString) {
            path = pathString;
            whenLastChecked = System.currentTimeMillis();
            try {
                Path path = Paths.get(pathString);
                absolutePath = path.toAbsolutePath().toString();
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                exists = true;
                isReadable = path.toFile().canRead();
                whenCreated = attributes.creationTime().toMillis();
                whenAccessed = attributes.lastAccessTime().toMillis();
                whenModified = attributes.lastModifiedTime().toMillis();
                size = attributes.size();
                isDirectory = attributes.isDirectory();
                isRegularFile = attributes.isRegularFile();
                isLink = attributes.isSymbolicLink();
                isOther = attributes.isOther();
            }
            catch (NoSuchFileException ex) {
                exists = false;
                problems = "no such file: " + absolutePath;
            }
            catch (Exception ex) {
                problems = ex.toString();
                if (problems == null) {
                    problems = ex.getClass().getName();
                }
//                System.out.println(problems + " while getting file attributes for " + pathString);
            }
        }

    }

    public FileStat get(String path) {
        FileStat stat = statCache.get(path);
        if (stat == null ) {
            stat = new FileStat(path);
            statCache.put(path, stat);
        }
        else if (stat.whenLastChecked < System.currentTimeMillis() - statsExpireAfter) {
            FileStat oldStat = stat;
            stat = new FileStat(path);
            statCache.put(path, stat);
            if (oldStat.whenModified != stat.whenModified) {
                discardCachedContent(oldStat.absolutePath);
            }
        }
        return stat;
    }

    public void discardCachedContent(String absolutePath) {
        byte[] cachedContent = contentCache.get(absolutePath);
        if (cachedContent != null) {
            contentCache.remove(absolutePath);
            contentCacheTotalBytes -= cachedContent.length;
        }
    }

    public String readString(String path) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        streamTo(path, stream);
        return new String(stream.toByteArray(), UTF_8);
    }

    public void streamTo(String path, OutputStream outputStream) throws Exception {
        FileStat stat = get(path);
        if (stat.size == 0) {
            // if somehow this is in the contentCache, we should throw it out
            discardCachedContent(stat.absolutePath);
            return;
        }

        if (!stat.isReadable) {
            throw new Exception("No read access on '" + path + "'");
        }

        if ( stat.size > CONTENT_CACHE_MAX_FILE_SIZE ) {
            // if somehow this is in the contentCache, we should throw it out
            discardCachedContent(stat.absolutePath);

            // and send it out in chunks
            try(InputStream fileStream = new FileInputStream(stat.absolutePath)) {
                byte buffer[] = new byte[CONTENT_CACHE_MAX_FILE_SIZE];
                int read;
                while ((read = fileStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            return;
        }

        byte[] content = contentCache.get(stat.absolutePath);
        if (content == null) {
            try {
                content = Files.readAllBytes(Paths.get(stat.absolutePath));
            }
            catch (Exception ex) {
                statCache.remove(path);
                stat = get(path);
            }
            if (content == null) {
                // refresh stats, since they are obviously wrong
                statCache.remove(path);
                stat = get(path);
                // if stat.size still > 0, you have an issue
                return;
            }
            if (content.length != stat.size) {
                // refresh stats, since they are obviously wrong
                statCache.remove(path);
                stat = get(path);
            }
            if (contentCacheTotalBytes + content.length < CONTENT_CACHE_MAX_SIZE) {
                contentCacheTotalBytes += content.length;
                contentCache.put(stat.absolutePath, content);
            }
        }
        outputStream.write(content);
    }
}
