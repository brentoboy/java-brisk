package com.bflarsen.brisk;

import java.util.Map;


/**
 * Created by brent on 10/19/2017.
 */
public class WebSocketMessage {
    public WebSocketContext Context;
    public boolean IsText;
    public String Text;
    public byte[] Blob;
    public int OpCode;
    public Map<String, Object> WorkerThreadResources;
    public String Target;
    public String UniqueId;
}
