package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;

import java.security.MessageDigest;
import java.util.Base64;


public class DefaultWebSocketUpgradeResponder implements HttpResponder {

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) {
        BaseResponse response;
        try {
            // detect obviously malformed requests
            if (context == null
                    || context.Request == null
                    || context.Request.getHeader("Request_Upgrade") == null
                    || context.Request.getHeader("Request_Connection") == null
                    || !context.Request.getHeader("Request_Upgrade").toLowerCase().equals("websocket")
                    || !context.Request.getHeader("Request_Connection").toLowerCase().contains("upgrade")
            ) {
                return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "malformed upgrade request");
            }

            // detect version 13 protocol
            if ("13".equals(context.Request.getHeader("Request_SecWebSocketVersion"))) {
                // detect malformed requests
                if (context.Request.getHeader("Request_SecWebSocketKey") == null) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "malformed upgrade request");
                }

                response = new SimpleStatusResponse(HttpStatusCode.SWITCHING_PROTOCOLS);
                response.setHeader("Upgrade", "websocket");
                response.setHeader("Connection", "Upgrade");
                response.setHeader("Sec-WebSocket-Accept", generateWebSocketAcceptCode(context.Request.getHeader("Request_SecWebSocketKey")));
                WebSocketContext webSocketContext = new WebSocketContext(context.Socket, context.Server, context.Session, "13");
                if (context.Request.getHeader("Request_SecWebSocketExtensions") != null
                        && context.Request.getHeader("Request_SecWebSocketExtensions").contains("permessage-deflate")
                ) {
                    response.setHeader("Sec-WebSocket-Extensions", "permessage-deflate");
                    webSocketContext.isDeflateEnabled = true;
                }
                context.Server.onWebSocketOpened(webSocketContext);
                if (webSocketContext.Protocol != null) {
                    response.setHeader("Sec-WebSocket-Protocol", webSocketContext.Protocol);
                }
                webSocketContext.Init();
                ((BaseBufferedResponse)response).buffer = new byte[] {};
                return response;
            }

            // detect draft-76 protocol
            if (context.Request.getHeader("Request_SecWebSocketKey1") != null
                    && context.Request.getHeader("Request_SecWebSocketKey2") != null
            ) {
                // detect malformed requests
                if (context.Request.getHeader("Request_Origin") == null
                        || context.Request.RawBody == null
                ) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "malformed upgrade request");
                }
                String key1 = context.Request.getHeader("Request_SecWebSocketKey1");
                long numberValue = 0;
                long spaceCount = 0;
                for (int i = 0; i < key1.length(); i++) {
                    char charAtIndex = key1.charAt(i);
                    if (charAtIndex == ' ') {
                        spaceCount++;
                    }
                    else if (Character.isDigit(charAtIndex)) {
                        numberValue = numberValue * 10 + Character.digit(charAtIndex, 10);
                    }
                    else {
                        // ignore other characters
                    }
                }
                // must have some spaces
                if (spaceCount == 0) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "No Spaces in Key1");
                }
                // final number value must be evenly divisible by spaceCount
                if (numberValue != (numberValue / spaceCount) * spaceCount) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "Key1 Not evenly divisible");
                }
                long int1 = numberValue / spaceCount;

                String key2 = context.Request.getHeader("Request_SecWebSocketKey2");
                numberValue = 0;
                spaceCount = 0;
                for (int i = 0; i < key2.length(); i++) {
                    char charAtIndex = key2.charAt(i);
                    if (charAtIndex == ' ') {
                        spaceCount++;
                    }
                    else if (Character.isDigit(charAtIndex)) {
                        numberValue = numberValue * 10 + Character.digit(charAtIndex, 10);
                    }
                    else {
                        // ignore other characters
                    }
                }
                // must have some spaces
                if (spaceCount == 0) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "No Spaces in Key2");
                }
                // final number value must be evenly divisible by spaceCount
                if (numberValue != (numberValue / spaceCount) * spaceCount) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "Key2 Not evenly divisible");
                }
                long int2 = numberValue / spaceCount;

                if (context.Request.RawBody.length != 8) {
                    return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "Request Body Wrong Size");
                }

                byte[] checksum = new byte[16];
                checksum[0] = (byte) (int1 >> 24);
                checksum[1] = (byte) (int1 >> 16);
                checksum[2] = (byte) (int1 >> 8);
                checksum[3] = (byte) (int1);
                checksum[4] = (byte) (int2 >> 24);
                checksum[5] = (byte) (int2 >> 16);
                checksum[6] = (byte) (int2 >> 8);
                checksum[7] = (byte) (int2);
                for (int i = 0; i < 8; i++) {
                    checksum[8+i] = context.Request.RawBody[i];
                }

                response = new BaseBufferedResponse(HttpStatusCode.SWITCHING_PROTOCOLS, MimeType.lookupByExtension(".bin"), null);
                response.setHeader("Upgrade", "WebSocket");
                response.setHeader("Connection", "Upgrade");
                response.setHeader("Sec-WebSocket-Origin", context.Request.getHeader("Request_Origin"));
                response.setHeader("Sec-WebSocket-Location", context.Request.getHeader("Request_Origin").replaceFirst("http", "ws").toLowerCase() + "/");
                WebSocketContext webSocketContext = new WebSocketContext(context.Socket, context.Server, context.Session, "draft-76");
                context.Server.onWebSocketOpened(webSocketContext);
                if (webSocketContext.Protocol != null) {
                    response.setHeader("Sec-WebSocket-Protocol", webSocketContext.Protocol);
                }
                byte[] buffer = java.security.MessageDigest.getInstance("MD5").digest(checksum);
                ((BaseBufferedResponse) response).buffer = buffer;
                return response;
            }

            // else
            return new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST, "malformed upgrade request");
        }
        catch (Exception ex) {
            try {
                return context.Server.Error500ResponderFactory.create().respond(context);
            }
            catch(Exception ex2) {
                return new SimpleStatusResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public static String generateWebSocketAcceptCode(String secWebSocketKey) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.reset();
        sha1.update((secWebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes());
        return Base64.getEncoder().encodeToString(sha1.digest());
    }

}
