package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.*;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class BaseResponse implements HttpResponse {
    public int StatusCode;
    public String StatusDescription;
    public String HttpVersion;
    public Map<String, String> Headers = new LinkedHashMap<>();

    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final SimpleDateFormat UtcFormatter = new SimpleDateFormat();
    static {
        UtcFormatter.setTimeZone(new SimpleTimeZone(0, "GMT"));
        UtcFormatter.applyPattern("dd MMM yyyy HH:mm:ss z");
    }

    protected BaseResponse(int statusCode) {
        this.HttpVersion = "HTTP/1.1";
        this.StatusCode = statusCode;
        this.setHeader("Date", UtcFormatter.format(new Date()));
        this.setHeader("Server", "Brisk/0.1.0");
        this.setHeader("Content-Type", "text/html; charset=UTF-8");
        this.setHeader("Cache-Control", "max-age=0");
    }

    public int getStatusCode() {
        return this.StatusCode;
    }

    public String getStatusDescription() {
        if (this.StatusDescription == null || this.StatusDescription.isEmpty()) {
            return lookupStatusDescription(this.StatusCode);
        }
        else {
            return this.StatusDescription;
        }
    }

    public static String lookupStatusDescription(int statusCode) {
        switch(statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 408: return "Request Timeout";
            case 411: return "Length Required";
            case 413: return "Request Entity Too Large";
            case 414: return "Request-URI Too Long";
            case 418: return "I'm a teapot";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            default: return "Unknown Status";
        }
    }

    public static String lookupMimeType(String filename) {
        String ext;
        int dotPos = filename.lastIndexOf(".");
        if (dotPos == -1) {
            ext = filename;
        }
        else {
            ext = filename.substring(dotPos + 1);
        }
        switch(ext) {
            case "323": return "text/h323";
            case "acx": return "application/internet-property-stream";
            case "ai": return "application/postscript";
            case "aif": return "audio/x-aiff";
            case "aifc": return "audio/x-aiff";
            case "aiff": return "audio/x-aiff";
            case "asf": return "video/x-ms-asf";
            case "asr": return "video/x-ms-asf";
            case "asx": return "video/x-ms-asf";
            case "au": return "audio/basic";
            case "avi": return "video/x-msvideo";
            case "axs": return "application/olescript";
            case "bas": return "text/plain";
            case "bcpio": return "application/x-bcpio";
            case "bin": return "application/octet-stream";
            case "bmp": return "image/bmp";
            case "c": return "text/plain";
            case "cat": return "application/vnd.ms-pkiseccat";
            case "cdf": return "application/x-netcdf";
            case "cer": return "application/x-x509-ca-cert";
            case "class": return "application/octet-stream";
            case "clp": return "application/x-msclip";
            case "cmx": return "image/x-cmx";
            case "cod": return "image/cis-cod";
            case "cpio": return "application/x-cpio";
            case "crd": return "application/x-mscardfile";
            case "crl": return "application/pkix-crl";
            case "crt": return "application/x-x509-ca-cert";
            case "csh": return "application/x-csh";
            case "css": return "text/css";
            case "dcr": return "application/x-director";
            case "der": return "application/x-x509-ca-cert";
            case "dir": return "application/x-director";
            case "dll": return "application/x-msdownload";
            case "dms": return "application/octet-stream";
            case "doc": return "application/msword";
            case "dot": return "application/msword";
            case "dvi": return "application/x-dvi";
            case "dxr": return "application/x-director";
            case "eps": return "application/postscript";
            case "etx": return "text/x-setext";
            case "evy": return "application/envoy";
            case "exe": return "application/octet-stream";
            case "fif": return "application/fractals";
            case "flr": return "x-world/x-vrml";
            case "gif": return "image/gif";
            case "gtar": return "application/x-gtar";
            case "gz": return "application/x-gzip";
            case "h": return "text/plain";
            case "hdf": return "application/x-hdf";
            case "hlp": return "application/winhlp";
            case "hqx": return "application/mac-binhex40";
            case "hta": return "application/hta";
            case "htc": return "text/x-component";
            case "htm": return "text/html";
            case "html": return "text/html";
            case "htt": return "text/webviewhtml";
            case "ico": return "image/x-icon";
            case "ief": return "image/ief";
            case "iii": return "application/x-iphone";
            case "ins": return "application/x-internet-signup";
            case "isp": return "application/x-internet-signup";
            case "jfif": return "image/pipeg";
            case "jpe": return "image/jpeg";
            case "jpeg": return "image/jpeg";
            case "jpg": return "image/jpeg";
            case "js": return "application/x-javascript";
            case "latex": return "application/x-latex";
            case "less": return "text/css";
            case "lha": return "application/octet-stream";
            case "lsf": return "video/x-la-asf";
            case "lsx": return "video/x-la-asf";
            case "lzh": return "application/octet-stream";
            case "m13": return "application/x-msmediaview";
            case "m14": return "application/x-msmediaview";
            case "m3u": return "audio/x-mpegurl";
            case "man": return "application/x-troff-man";
            case "mdb": return "application/x-msaccess";
            case "me": return "application/x-troff-me";
            case "mht": return "message/rfc822";
            case "mhtml": return "message/rfc822";
            case "mid": return "audio/mid";
            case "mny": return "application/x-msmoney";
            case "mov": return "video/quicktime";
            case "movie": return "video/x-sgi-movie";
            case "mp2": return "video/mpeg";
            case "mp3": return "audio/mpeg";
            case "mpa": return "video/mpeg";
            case "mpe": return "video/mpeg";
            case "mpeg": return "video/mpeg";
            case "mpg": return "video/mpeg";
            case "mpp": return "application/vnd.ms-project";
            case "mpv2": return "video/mpeg";
            case "ms": return "application/x-troff-ms";
            case "msg": return "application/vnd.ms-outlook";
            case "mvb": return "application/x-msmediaview";
            case "nc": return "application/x-netcdf";
            case "nws": return "message/rfc822";
            case "oda": return "application/oda";
            case "p10": return "application/pkcs10";
            case "p12": return "application/x-pkcs12";
            case "p7b": return "application/x-pkcs7-certificates";
            case "p7c": return "application/x-pkcs7-mime";
            case "p7m": return "application/x-pkcs7-mime";
            case "p7r": return "application/x-pkcs7-certreqresp";
            case "p7s": return "application/x-pkcs7-signature";
            case "pbm": return "image/x-portable-bitmap";
            case "pdf": return "application/pdf";
            case "pfx": return "application/x-pkcs12";
            case "pgm": return "image/x-portable-graymap";
            case "pko": return "application/ynd.ms-pkipko";
            case "pma": return "application/x-perfmon";
            case "pmc": return "application/x-perfmon";
            case "pml": return "application/x-perfmon";
            case "pmr": return "application/x-perfmon";
            case "pmw": return "application/x-perfmon";
            case "pnm": return "image/x-portable-anymap";
            case "png": return "image/png";
            case "pot": return "application/vnd.ms-powerpoint";
            case "ppm": return "image/x-portable-pixmap";
            case "pps": return "application/vnd.ms-powerpoint";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "prf": return "application/pics-rules";
            case "ps": return "application/postscript";
            case "pub": return "application/x-mspublisher";
            case "qt": return "video/quicktime";
            case "ra": return "audio/x-pn-realaudio";
            case "ram": return "audio/x-pn-realaudio";
            case "ras": return "image/x-cmu-raster";
            case "rgb": return "image/x-rgb";
            case "rmi": return "audio/mid";
            case "roff": return "application/x-troff";
            case "rtf": return "application/rtf";
            case "rtx": return "text/richtext";
            case "scd": return "application/x-msschedule";
            case "sct": return "text/scriptlet";
            case "setpay": return "application/set-payment-initiation";
            case "setreg": return "application/set-registration-initiation";
            case "sh": return "application/x-sh";
            case "shar": return "application/x-shar";
            case "sit": return "application/x-stuffit";
            case "snd": return "audio/basic";
            case "spc": return "application/x-pkcs7-certificates";
            case "spl": return "application/futuresplash";
            case "src": return "application/x-wais-source";
            case "sst": return "application/vnd.ms-pkicertstore";
            case "stl": return "application/vnd.ms-pkistl";
            case "stm": return "text/html";
            case "sv4cpio": return "application/x-sv4cpio";
            case "sv4crc": return "application/x-sv4crc";
            case "svg": return "image/svg+xml";
            case "swf": return "application/x-shockwave-flash";
            case "t": return "application/x-troff";
            case "tar": return "application/x-tar";
            case "tcl": return "application/x-tcl";
            case "tex": return "application/x-tex";
            case "texi": return "application/x-texinfo";
            case "texinfo": return "application/x-texinfo";
            case "tgz": return "application/x-compressed";
            case "tif": return "image/tiff";
            case "tiff": return "image/tiff";
            case "tr": return "application/x-troff";
            case "trm": return "application/x-msterminal";
            case "tsv": return "text/tab-separated-values";
            case "txt": return "text/plain";
            case "uls": return "text/iuls";
            case "ustar": return "application/x-ustar";
            case "vcf": return "text/x-vcard";
            case "vrml": return "x-world/x-vrml";
            case "wav": return "audio/x-wav";
            case "wcm": return "application/vnd.ms-works";
            case "wdb": return "application/vnd.ms-works";
            case "wks": return "application/vnd.ms-works";
            case "wmf": return "application/x-msmetafile";
            case "wps": return "application/vnd.ms-works";
            case "wri": return "application/x-mswrite";
            case "wrl": return "x-world/x-vrml";
            case "wrz": return "x-world/x-vrml";
            case "xaf": return "x-world/x-vrml";
            case "xbm": return "image/x-xbitmap";
            case "xla": return "application/vnd.ms-excel";
            case "xlc": return "application/vnd.ms-excel";
            case "xlm": return "application/vnd.ms-excel";
            case "xls": return "application/vnd.ms-excel";
            case "xlt": return "application/vnd.ms-excel";
            case "xlw": return "application/vnd.ms-excel";
            case "xml": return "application/xml";
            case "xof": return "x-world/x-vrml";
            case "xpm": return "image/x-xpixmap";
            case "xwd": return "image/x-xwindowdump";
            case "z": return "application/x-compress";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }

    public void setHeader(String key, String value) {
        this.Headers.put(key, value);
    }

    @Override
    public String getHttpVersion() {
        return this.HttpVersion;
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.Headers;
    }

    @Override
    public boolean hasBodySender() {
        return false;  // if you override sendBody, override this too, and return true
    }

    @Override
    public Long getContentLength() {
        return null; // if you override sendBody, you should override this
    }

    @Override
    public void sendBody(OutputStream stream) throws Exception {
        throw new Exception("Send Body Not Implemented for class: " + this.getClass().getName());
    }
}
