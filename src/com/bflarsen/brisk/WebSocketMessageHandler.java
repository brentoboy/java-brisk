package com.bflarsen.brisk;


public interface WebSocketMessageHandler {
    interface Factory { WebSocketMessageHandler create() throws Exception; }

    void handle(WebSocketMessage message) throws Exception;
}