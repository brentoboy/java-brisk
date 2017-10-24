package com.bflarsen.brisk;

import java.util.Map;


public class WebSocketMessage {
    public WebSocketContext Context;
    public boolean IsText;
    public String Text;
    public byte[] Blob;
    public int OpCode;
    public Map<String, Object> WorkerThreadResources;
}
