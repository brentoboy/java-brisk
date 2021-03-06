package com.bflarsen.brisk;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bflarsen.brisk.pumps.*;
import com.bflarsen.brisk.responders.*;
import com.bflarsen.util.AutoConvert;
import com.bflarsen.util.FileStatCache;
import javax.net.ssl.*;

import static com.bflarsen.util.Logger.*;

public class HttpServer extends Thread {
    private final String CLASS_NAME = "HttpServer";

    public int Port = 80;
    public final Map<Pattern, HttpResponder.Factory> Routes = new LinkedHashMap<>();

    public HttpResponder.Factory Error404ResponderFactory = DefaultError404Responder::new;
    public HttpResponder.Factory Error500ResponderFactory = DefaultError500Responder::new;
    public HttpResponder.Factory WebSocketUpgradeResponderFactory = DefaultWebSocketUpgradeResponder::new;

    public boolean isClosing = false;
    public final LinkedBlockingQueue<Socket> IncomingRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> AllRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> ParsedRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> RoutedRequests = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> ResponseReady = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<HttpContext> DoneSending = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<WebSocketMessage> InboundWebSocketMessages = new LinkedBlockingQueue<>();

    private final HttpRequestParsingPump RequestParser = new HttpRequestParsingPump(this);
    private final HttpRequestRoutingPump RequestRouter = new HttpRequestRoutingPump(this);
    private final HttpResponseBuildingPump ResponseBuilder = new HttpResponseBuildingPump(this);
    private final HttpResponseSendingPump ResponseSender = new HttpResponseSendingPump(this);
    private final HttpContextCleanupPump ContextCleanup = new HttpContextCleanupPump(this);
    private final WebSocketMessagePump WebSocketMessageHandler = new WebSocketMessagePump(this);

    public boolean DisplayErrorDetails = false;

    public boolean CreateSessions = false;
    public long SessionExpiresAfterMillis = 2 * 60 * 60 * 1000L; // 2 hours
    public final Map<String, HttpSession> Sessions = new ConcurrentHashMap<>();
    public String SessionDomain;
    public String SessionCookieName = "SessionID";

    public final FileStatCache FileCache = new FileStatCache();

    public int NumberOfRequestParsingThreadsToCreate = 8;
    public int NumberOfRequestRoutingThreadsToCreate = 8;
    public int NumberOfResponseBuildingThreadsToCreate = 8;
    public int NumberOfResponseSendingThreadsToCreate = 8;
    public int NumberOfContextCleanupThreadsToCreate = 8;
    public int NumberOfWebSocketMessageHandlingThreadsToCreate = 8;

    public final AutoConvert AutoConverter = new AutoConvert();

    public String ViewFolder = null;
    public String SslKeyStorePath = null;
    public String SslKeyStorePassphrase = null;

    public void runAsync() {
        start();
    }

    @Override
    public void run() {
        this.isClosing = false;
        this.RequestParser.run();
        this.RequestRouter.run();
        this.ResponseBuilder.run();
        this.ResponseSender.run();
        this.ContextCleanup.run();
        this.WebSocketMessageHandler.run();

        try (ServerSocket serverSocket = createServerSocket()) {
            logInfo("Listening on Port " + this.Port, CLASS_NAME, "run()", "Initializing");

            while (!this.isClosing) {
                try {
                    Socket socket = serverSocket.accept();
                    this.IncomingRequests.add(socket);
                }
                catch (Exception ex) {
                    logEx(ex, CLASS_NAME, "run()", "Attempting to accept() on port " + this.Port);
                }
            }
        }
        catch(Exception ex){
            logEx(ex, CLASS_NAME, "run", "Attempting to listen() on port " + this.Port);
        }

        logInfo("No longer listening on Port " + this.Port, CLASS_NAME, "run()", "Cleaning Up");
    }

    private ServerSocket createServerSocket() throws Exception {
        if (SslKeyStorePath != null) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(SslKeyStorePath), SslKeyStorePassphrase.toCharArray());

            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance("SunX509");
            kmFactory.init(keystore, SslKeyStorePassphrase.toCharArray());

            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance("SunX509");
            tmFactory.init(keystore);

            SSLContext sc = SSLContext.getInstance("TLSv1");
            sc.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), null);

            SSLServerSocketFactory sslSocketFactory = sc.getServerSocketFactory();
            SSLServerSocket socket = (SSLServerSocket) sslSocketFactory.createServerSocket(this.Port);

            return socket;
        }
        else {
            return new ServerSocket(this.Port);
        }
    }

    public void initiateShutdown() {
        logInfo("Initiating Shutdown Sequence", CLASS_NAME, "initiateShutdown()", "");
        this.isClosing = true;
    }

    public void addRoute(String path, HttpResponder.Factory factory) throws Exception {
        this.addRoute(buildRegexPattern(path), factory);
    }

    public void addRoute(Pattern regex, HttpResponder.Factory factory) {
        this.Routes.put(regex, factory);
    }

    public Pattern buildRegexPattern(String path) throws Exception {
        String regexPattern;

        // if it smells like a protocol relative path in the form of //domain.com/resource.html ...
        if (path.startsWith("//")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^(http|https)\\:" + path + "$";
        }
        // if it smells like its relative to site base
        else if (path.startsWith("/")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^(http|https)\\:\\/\\/[^\\/]+" + path + "$";
        }
        // if it smells like it has a protocol prefix
        else if (path.startsWith("http://") || path.startsWith("https://")) {
            final String[] pieces = path.split(Pattern.quote("*"));
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = Pattern.quote(pieces[i]);
            }
            path = String.join(".*", pieces);
            regexPattern = "^" + path + "$";
        }
        else {
            throw new Exception(String.format("'%s' is not in one of the expected formats", path));
        }

        return Pattern.compile(regexPattern);
    }

    public void addRouteToFiles(String pattern, String path) throws Exception {
        addRouteToFiles(buildRegexPattern(pattern), path);
    }

    public void addRouteToFiles(Pattern pattern, String baseDirectory) throws Exception {
        addRoute(pattern, StaticFileResponder.createFactory(Paths.get(baseDirectory)));
    }

//    public String encodeJson(Object obj) throws Exception {
//        return "{\"error\":\"If you wish to send Json Encoded objects, you'll have to override HttpServer.encodeJson and use something like GSON to encode, I'd do that for you, but I don't want to introduce dependencies or force you to use an encoder you don't like. /r/n -- cheers\"}";
//    }

    public Map<String, Object> createWorkerThreadResources() throws Exception {
        return new LinkedHashMap<>();
    }

    public void freeWorkerThreadResources(Map<String, Object> resources) throws Exception {
        for (Map.Entry<String, Object> entry : resources.entrySet()) {
            String key = entry.getKey();
            Object resource = entry.getValue();
            if (resource instanceof AutoCloseable) {
                AutoCloseable closeable = (AutoCloseable) resource;
                try {
                    closeable.close();
                } catch (Exception ex) {
                    logEx(ex, "HttpServer", "freeWorkerThreadResources", "closing '" + key + "'");
                }
            }
        }
    }

    public void resetWorkerThreadResources(Map<String, Object> resources) throws Exception {}

    public void logRequestResponseCompleted(HttpContext context) {
        logInfo(
                context.Stats.totalMs + "ms"
                + "\t" + context.Request.Method
                + " " + context.Request.Protocol
                + " " + context.Request.Host
                + " " + context.Request.Resource
                , "HttpServer"
                , "logRequestResponseCompleted()"
                , String.format("%d", context.Id)
        );
    }

    public HttpSession createSessionObject(String uuid, HttpContext context) throws Exception {
        HttpSession session = this.loadPersistedSession(uuid);
        if (session == null) {
            session = new HttpSession(uuid);
            session.Expires = System.currentTimeMillis() + context.Server.SessionExpiresAfterMillis;
            this.persistSession(session);
        }
        else {
            session.Expires = System.currentTimeMillis() + context.Server.SessionExpiresAfterMillis;
            this.persistSessionExpires(uuid, session.Expires);
        }
        return session;
    }

    public void persistSession(HttpSession session) throws Exception {
        // override this if you need to persist sessions
    }

    public void persistSessionExpires(String uuid, long expiresAt) {
        // override this if you need to persist sessions
    }

    public HttpSession loadPersistedSession(String uuid) throws Exception {
        // override this if you need to persist sessions
        return null;
    }

    public void destroySessionObject(HttpSession session) {
        // override this if you need to cleanup resources you created in createSessionObject
    }

    public void onWebSocketOpened(WebSocketContext webSocketContext) {
        logInfo("WebSocket Opened", "HttpServer", "onWebSocketOpened", webSocketContext.SessionId);
    }

    public void onWebSocketClosed(WebSocketContext webSocketContext) {
        logInfo("WebSocket Closed", "HttpServer", "onWebSocketClosed", webSocketContext.SessionId);
    }

    public void handleWebSocketMessage(WebSocketMessage message) throws Exception {
        // override this if you do WebSockets
    }
}