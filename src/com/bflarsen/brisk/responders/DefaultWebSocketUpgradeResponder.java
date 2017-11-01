package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;

import java.security.MessageDigest;
import java.util.Base64;

import static com.bflarsen.util.Logger.*;


public class DefaultWebSocketUpgradeResponder implements HttpResponder {

    @Override
    public boolean canHandle(HttpContext context) {
        return true;
    }

    @Override
    public HttpResponse respond(HttpContext context) {
        BaseResponse response;
        try {
            if (context == null
                    || context.Request == null
                    || context.Request.getHeader("Request_Upgrade") == null
                    || context.Request.getHeader("Request_Connection") == null
                    || context.Request.getHeader("Request_SecWebSocketKey") == null
                    || context.Request.getHeader("Request_SecWebSocketVersion") == null
                    || !context.Request.getHeader("Request_Upgrade").equals("websocket")
                    || !context.Request.getHeader("Request_Connection").contains("Upgrade")
                    || !context.Request.getHeader("Request_SecWebSocketVersion").equals("13")
            ) {
                response = new SimpleStatusResponse(HttpStatusCode.BAD_REQUEST);
            }
            else {
                response = new SimpleStatusResponse(HttpStatusCode.SWITCHING_PROTOCOLS);
                response.setHeader("Upgrade", "websocket");
                response.setHeader("Connection", "Upgrade");
                response.setHeader("Sec-WebSocket-Accept", generateWebSocketAcceptCode(context.Request.getHeader("Request_SecWebSocketKey")));
                WebSocketContext webSocketContext = new WebSocketContext(context.Socket, context.Server, context.Session);
                context.Server.onWebSocketOpened(webSocketContext);
                if (webSocketContext.Protocol != null) {
                    response.setHeader("Sec-WebSocket-Protocol", webSocketContext.Protocol);
                }
                webSocketContext.Init();
                ((BaseBufferedResponse)response).buffer = new byte[] {};
            }
            return response;
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
