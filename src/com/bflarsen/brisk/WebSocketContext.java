package com.bflarsen.brisk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static com.bflarsen.util.Logger.*;

public class WebSocketContext {

    public static class FailConnectionException extends Exception {
        public final int Code;
        public FailConnectionException(int code) { super(String.format("Fail Connection: %d", code)); this.Code = code; }
    }

    private static final int OP_CONTINUATION_FRAME = 0x0;
    private static final int OP_TEXT_FRAME = 0x1;
    private static final int OP_BINARY_FRAME = 0x2;
    private static final int OP_CONNECTION_CLOSE_FRAME = 0x8;
    private static final int OP_PING_FRAME = 0x9;
    private static final int OP_PONG_FRAME = 0xA;

    public static final int CLOSED_NORMALLY = 1000;
    public static final int CLOSED_DUE_TO_GOING_AWAY = 1001;
    public static final int CLOSED_DUE_TO_PROTOCOL_ERROR = 1002;
    public static final int CLOSED_DUE_TO_RECEIVING_UNACCEPTABLE_DATA = 1002;
    public static final int CLOSED_DUE_TO_RECEIVING_DATA_INCONSISTENT_WITH_TYPE = 1007;
    public static final int CLOSED_DUE_TO_POLICY_VIOLATION = 1008;
    public static final int CLOSED_DUE_TO_MESSAGE_TOO_BIG = 1009;

    public static final int MAX_UNSIGNED_SHORT = 0xFFFF;

    private final Socket Socket;
    public final String Name;
    public final HttpServer Server;
    public final HttpSession Session;
    public final String Version;
    public final String SessionId;
    public String Protocol = null;
    private final Thread ReadThread = new Thread(() -> this.ReadThreadProc());
    private final Thread WriteThread = new Thread(() -> this.WriteThreadProc());
    private final LinkedBlockingQueue<WebSocketMessage> SendQueue = new LinkedBlockingQueue<>();
    private WebSocketMessage incomingMessage = null;
    private static final WebSocketMessage STOP_MESSAGE = new WebSocketMessage();

    public WebSocketContext(Socket socket, HttpServer server, HttpSession session, String version) {
        this.Socket = socket;
        this.Server = server;
        this.Session = session;
        this.Version = version;
        this.SessionId = (session != null) ? session.UniqueID : null;
        this.Name = UUID.randomUUID().toString();
    }

    public void Init() {
        this.ReadThread.start();
        this.WriteThread.start();
    }

    public void ReadThreadProc() {
        try {
            switch(Version) {
                case "13": ReadThreadProc_13(); break;
                case "draft-76": ReadThreadProc_Draft76(); break;
                default: logWarning(String.format("Unrecognized Protocol Version: %s", Version), "WebSocketContext", "ReadThreadProc", "Selecting Protocol Version");
            }
        }
        catch (FailConnectionException ex) {
            this.sendClose(ex.Code);
        }
        catch (InterruptedException ex) {
            // don't care
        }
        catch (SocketException ex) {
            // don't care
        }
        catch (Exception ex) {
            logEx(ex, "WebSocketContext", "ReadThreadProc", "processing incoming data");
            this.Term();
        }

        // cleanup
        try {
            this.WriteThread.wait();
            this.Socket.close();
        }
        catch (Exception ex) {
            // don't care
        }
        Server.onWebSocketClosed(this);
    }
    public void WriteThreadProc() {
        try {
            switch(Version) {
                case "13": WriteThreadProc_13(); break;
                case "draft-76": WriteThreadProc_Draft76(); break;
                default: logWarning(String.format("Unrecognized Protocol Version: %s", Version), "WebSocketContext", "WriteThreadProc", "Selecting Protocol Version");
            }
        }
        catch (InterruptedException ex) {
            // don't care
        }
        catch (SocketException ex) {
            // don't care
        }
        catch (Exception ex) {
            logEx(ex, "WebSocketContext", "WriteThreadProc", "processing incoming data");
            this.Term();
        }
    }

    public void ReadThreadProc_Draft76() throws Exception {
        final InputStream stream = this.Socket.getInputStream();

        main_loop: while (true) {
            byte[] frameHeader = readBytes(stream,1);
            byte[] payload = null;

            switch ((int)frameHeader[0]) {
                case 0x00: {  // string follows.  read bytes until you get 0xff, the result can be interpeted as a utf-8 string
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nextByte = stream.read();
                    if (nextByte == 0xff) {
                        // close
                    }
                    else {
                        buffer.write(nextByte);
                    }
                    while (nextByte != 0xff) {
                        try {
                            nextByte = stream.read();
                            if (nextByte != 0xff) {
                                buffer.write(nextByte);
                            }
                        }
                        catch (SocketTimeoutException ex) {
                            // dont care
                        }
                    }
                    payload = buffer.toByteArray();
                    break;
                }
                case 0xFF: // blob follows, length first
                    int nextByte = stream.read();
                    boolean highBit = (nextByte & 0x80) == 0x80;
                    int lowBits = (nextByte & 0x7f);
                    long payloadLength = lowBits;

                    while (highBit) {
                        nextByte = stream.read();
                        highBit = (nextByte & 0x80) == 0x80;
                        lowBits = (nextByte & 0x7f);
                        payloadLength = payloadLength * 128 + lowBits;
                        if (payloadLength > Integer.MAX_VALUE) {
                            throw new FailConnectionException(CLOSED_DUE_TO_MESSAGE_TOO_BIG);
                        }                    }

                    if (payloadLength == 0) {  // this is a close message
                        sendClose(CLOSED_NORMALLY);
                        break main_loop;
                    }
                    else {
                        payload = readBytes(stream, (int)payloadLength);
                    }
                    break;
                default:
                    throw new FailConnectionException(CLOSED_DUE_TO_PROTOCOL_ERROR);
            }

            if (payload != null) {
                WebSocketMessage message = new WebSocketMessage();
                message.Context = this;
                message.IsText = true;
                message.Text = new String(payload);
                this.Server.InboundWebSocketMessages.put(message);
            }
        }
    }
    public void ReadThreadProc_13() throws Exception {
        final InputStream stream = this.Socket.getInputStream();
        final List<byte[]> fragments = new ArrayList<>();
        Integer MessageEncoding = null; // OP_TEXT_FRAME or OP_BINARY_FRAME, depending on first frame

        main_loop: while (true) {

            byte[] frameHeader = readBytes(stream,2);
            boolean fin = (0x80 & frameHeader[0]) == 0x80;   // bin 10000000
            boolean rsv1 = (0x40 & frameHeader[0]) == 0x40;  // bin 01000000
            boolean rsv2 = (0x20 & frameHeader[0]) == 0x20;  // bin 00100000
            boolean rsv3 = (0x10 & frameHeader[0]) == 0x10;  // bin 00010000
            int opcode = 0x0F & frameHeader[0];   // bin 00001111
            boolean mask = (0x80 & frameHeader[1]) == 0x80;  // bin 10000000;
            int payloadLength = 0x7F & frameHeader[1];   // bin 01111111

            // per RFC6455, if rsv bits are set and no extension in use that wants them, we must fail
            if (rsv1 || rsv2 || rsv3) {
                throw new FailConnectionException(CLOSED_DUE_TO_PROTOCOL_ERROR);
            }

            // per RFC6455, if an unrecognized opcode is received, we MUST fail connection
            switch (opcode) {
                case OP_CONTINUATION_FRAME:
                case OP_TEXT_FRAME:
                case OP_BINARY_FRAME:
                case OP_CONNECTION_CLOSE_FRAME:
                case OP_PING_FRAME:
                case OP_PONG_FRAME:
                    break; // these are recognized opcodes
                default:
                    throw new FailConnectionException(CLOSED_DUE_TO_PROTOCOL_ERROR);
            }

            if (payloadLength == 126) {
                ByteBuffer buffer = ByteBuffer.wrap(readBytes(stream, 2));
                buffer.order(ByteOrder.BIG_ENDIAN); // protocol specifies network byte order
                payloadLength = buffer.getShort();
            }
            else if (payloadLength == 127) {
                ByteBuffer buffer = ByteBuffer.wrap(readBytes(stream, 8));
                buffer.order(ByteOrder.BIG_ENDIAN); // protocol specifies network byte order
                long value = buffer.getLong();
                // I'm sure its possible for us to deal with a "long" amount of data,
                // if we decide to we need to enhance some stuff before removing this check
                // for instance, the constructor for a byte array wants an int, not a long
                // which complicates the readBytes function
                if (value > Integer.MAX_VALUE) {
                    throw new FailConnectionException(CLOSED_DUE_TO_MESSAGE_TOO_BIG);
                }
                payloadLength = (int)value;
            }

            // per RFC6455, if traffic from client is not masked, we must fail
            if (!mask) {
                throw new FailConnectionException(CLOSED_DUE_TO_PROTOCOL_ERROR);
            }
            byte[] maskingKey = readBytes(stream, 4);
            byte[] payload = readBytes(stream, payloadLength);

            // unmask it
            for (int i = 0; i < payloadLength; i++) {
                payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
            }

            // decide what to do with it now that we have it
            switch (opcode) {
                case OP_TEXT_FRAME:
                case OP_BINARY_FRAME: {
                    MessageEncoding = opcode;
                    fragments.add(payload);
                    break;
                }
                case OP_CONTINUATION_FRAME: {
                    fragments.add(payload);
                    break;
                }
                case OP_CONNECTION_CLOSE_FRAME: {
                    sendClose(CLOSED_NORMALLY);
                    break main_loop;
                }
                case OP_PING_FRAME: {
                    sendPong(payload);
                    break;
                }
                case OP_PONG_FRAME: {
                    // hmm.  what's the use case? why did we send a ping?
                    // do we need to raise an event somewhere to let someone know they got their reply?
                    break;
                }
            }

            payload = null;

            // if its the last frame in the message, then assemble and process the message
            if (fin) {
                WebSocketMessage message = new WebSocketMessage();
                message.Context = this;
                int totalPayloadLength = 0;
                for (byte[] fragment : fragments) {
                    totalPayloadLength += fragment.length;
                }
                ByteBuffer buffer = ByteBuffer.allocate(totalPayloadLength);
                for (byte[] fragment : fragments) {
                    buffer.put(fragment);
                }
                if (MessageEncoding != null && MessageEncoding == OP_TEXT_FRAME) {
                    message.IsText = true;
                    message.Text = new String(buffer.array());
                }
                else {
                    message.IsText = false;
                    message.Blob = buffer.array();
                }
                this.Server.InboundWebSocketMessages.put(message);

                // reset for next message (the next frame will be a new message)
                MessageEncoding = null;
                fragments.clear();
            }
        }

    }

    private static byte[] readBytes(InputStream stream, int count) throws IOException, FailConnectionException {
        byte[] buffer = new byte[count];
        int totalBytesRead = 0;
        int loopCount = 0;
        while (totalBytesRead < count) {
            loopCount++;
            try {
                int bytesRead = stream.read(buffer, totalBytesRead, count - totalBytesRead);
                if (bytesRead < 0) {
                    throw new FailConnectionException(CLOSED_DUE_TO_RECEIVING_UNACCEPTABLE_DATA);
                }
                totalBytesRead += bytesRead;
            }
            catch (SocketTimeoutException ex) {
                // I don't care, long periods between transmissions are perfectly acceptable
            }
        }
        return buffer;
    }

    public void WriteThreadProc_Draft76() throws Exception {
        final OutputStream stream = Socket.getOutputStream();

        main_loop: while (true) {
            Thread.yield();
            WebSocketMessage message = this.SendQueue.take();
            if (message.OpCode == OP_CONNECTION_CLOSE_FRAME
                    || message == STOP_MESSAGE
            ) {
                stream.write(0xFF); // indicates close
                stream.write(0x00);
                stream.flush();
                break main_loop;
            }
            if (message.Blob != null) { // that's bad, since we dont support binary for this protocol
                // TODO: logWarning(
                continue;
            }
            if (message.Text == null || message.Text.length() == 0) {
                // TODO: nothing to send!
                continue;
            }
            stream.write(0x00); // indicates start of string
            stream.write(message.Text.getBytes());
            stream.flush();
        }
    }
    public void WriteThreadProc_13() throws Exception{
        final OutputStream stream = Socket.getOutputStream();

        main_loop: while (true) {
            Thread.yield();
            WebSocketMessage message = this.SendQueue.take();
            if (message == STOP_MESSAGE) {
                break main_loop;
            }
            if (message.Blob == null) {
                message.Blob = new byte[0];
            }
            stream.write(
                    0x80 // fin: yes
                    // middle 3 bits are RSV, and we dont use them
                    | (message.OpCode & 0xF) // last nibble is the ob code
            );
            if (message.Blob.length <= 125) {
                stream.write((byte)message.Blob.length);
            }
            else if (message.Blob.length <= MAX_UNSIGNED_SHORT) {
                stream.write((byte)126);
                byte[] payloadLength = new byte[] {
                        (byte)(message.Blob.length >> 8),
                        (byte)message.Blob.length,
                };
                stream.write(payloadLength);
            }
            else {
                stream.write((byte)127);
                byte[] payloadLength = new byte[] {
                        (byte)0,
                        (byte)0,
                        (byte)0,
                        (byte)0,
                        (byte)(message.Blob.length >> 24),
                        (byte)(message.Blob.length >> 16),
                        (byte)(message.Blob.length >> 8),
                        (byte)message.Blob.length,
                };
                stream.write(payloadLength);
            }
            if (message.Blob.length > 0) {
                stream.write(message.Blob);
            }
            stream.flush();
        }
    }

    private void sendPong(byte[] reply) {
        WebSocketMessage message = new WebSocketMessage();
        message.OpCode = OP_PONG_FRAME;
        message.Blob = reply;
        try {
            this.SendQueue.put(message);
        }
        catch (InterruptedException ex) {
            // don't care
        }
    }

    private void sendClose(int code) {
        sendClose(code, null);
    }
    private void sendClose(int code, String reason) {
        WebSocketMessage message = new WebSocketMessage();
        message.OpCode = OP_CONNECTION_CLOSE_FRAME;
        if (reason != null && reason.length() > 0) {
            message.Blob = reason.getBytes();
        }
        try {
            this.SendQueue.put(message);
            this.SendQueue.put(STOP_MESSAGE);
        }
        catch (InterruptedException ex) {
            // don't care
        }
    }

    public void Send(byte[] blob) {
        WebSocketMessage message = new WebSocketMessage();
        message.OpCode = OP_BINARY_FRAME;
        message.Blob = blob;
        try {
            this.SendQueue.put(message);
        }
        catch (InterruptedException ex) {
            // don't care
        }
    }

    public void Send(String text) {
        WebSocketMessage message = new WebSocketMessage();
        message.OpCode = OP_TEXT_FRAME;
        message.Blob = text.getBytes();
        try {
            this.SendQueue.put(message);
        }
        catch (InterruptedException ex) {
            // don't care
        }
    }

    public void Term() {
        this.sendClose(CLOSED_DUE_TO_GOING_AWAY);
    }

    public void Wait() {
        try {
            this.ReadThread.wait();
            this.WriteThread.wait();
        }
        catch (InterruptedException ex) {
            // don't care
        }
    }
}
