package com.bflarsen.brisk.responses;

import com.bflarsen.brisk.HttpStatusCode;
import com.bflarsen.brisk.MimeType;

public class SimpleStatusResponse extends BaseBufferedResponse {
    public SimpleStatusResponse(int statusCode, String noteToUser) {
        super(
                statusCode
                , MimeType.lookupByExtension("html")
                ,  "<html>\n"
                + "<head>\n"
                + " <title>" + HttpStatusCode.lookupDescription(statusCode) + "</title>\n"
                + "</head>\n"
                + "<body>\n"
                + " <h1>" + HttpStatusCode.lookupDescription(statusCode) + "</h1>\n"
                + " <p>" + noteToUser + "</p>\n"
                + "</body>\n"
                + "</html>\n"
        );
    }
    public SimpleStatusResponse(int statusCode) {
        super(
                statusCode
                , MimeType.lookupByExtension("html")
                ,  "<html>\n"
                + "<head>\n"
                + " <title>" + HttpStatusCode.lookupDescription(statusCode) + "</title>\n"
                + "</head>\n"
                + "<body>\n"
                + " <h1>" + HttpStatusCode.lookupDescription(statusCode) + "</h1>\n"
                + "</body>\n"
                + "</html>\n"
        );
    }
}
