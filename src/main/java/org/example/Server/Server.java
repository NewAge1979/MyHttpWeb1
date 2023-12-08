package org.example.Server;

import lombok.AccessLevel;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Server.Handlers.Handler;
import org.example.Server.Request.Request;
import org.example.Server.Request.RequestMethod;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {
    @Setter(AccessLevel.PUBLIC)
    private List<String> validPaths;
    private final static Logger myLogger = LogManager.getLogger(Server.class);
    @Setter(AccessLevel.PUBLIC)
    private int portNumber;
    @Setter(AccessLevel.PUBLIC)
    private int threadPoolSize;
    private static Server instance = null;
    private ExecutorService myThreadPool;
    private final Map<RequestMethod, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    private Server() {
    }

    public static Server getInstance() {
        if (instance == null) {
            synchronized (Server.class) {
                if (instance == null) {
                    instance = new Server();
                }
            }
        }
        return instance;
    }

    public void addHandler(RequestMethod method, String path, Handler handler) {
        Map<String, Handler> map = new ConcurrentHashMap<>();
        if (handlers.containsKey(method)) {
            map = handlers.get(method);
        }
        map.put(path, handler);
        handlers.put(method, map);
    }

    public void runHandler(Request myRequestObj, BufferedOutputStream myResponse) {
        Handler handler = handlers.get(myRequestObj.getMethod()).get(myRequestObj.getPath());
        //myLogger.info(String.format("Handler: %s", handler.toString()));
        if (handler == null) {
            notFound(myResponse);
        } else {
            handler.handle(myRequestObj, myResponse);
        }
    }

    public void badRequest(BufferedOutputStream myResponse) {
        try {
            myResponse.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            myResponse.flush();
        } catch (IOException e) {
            myLogger.error(e.getMessage());
        }
    }

    public void notFound(BufferedOutputStream myResponse) {
        try {
            myResponse.write((
                    "HTTP/1.1 404 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            myResponse.flush();
        } catch (IOException e) {
            myLogger.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        if (portNumber == 0) {
            myLogger.error("The server port is not set!");
        } else {
            if (threadPoolSize == 0) {
                myLogger.error("Thread pool size is not set!");
            } else {
                myThreadPool = Executors.newFixedThreadPool(threadPoolSize);
                try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                    myLogger.info("Server successful started!");
                    serverSocketChannel.socket().bind(new InetSocketAddress(portNumber));
                    serverSocketChannel.configureBlocking(false);
                    while (!Thread.currentThread().isInterrupted()) {
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
                        if (clientSocketChannel != null) {
                            Connection myConnection = new Connection(clientSocketChannel.socket());
                            myThreadPool.submit(myConnection);
                            myLogger.info("Client connected!");
                        }
                    }
                    myThreadPool.shutdown();
                } catch (IOException e) {
                    myLogger.error(String.format("Server not started! Error message: %s", e.getMessage()));
                }
            }
        }
    }
}