package org.example.Server;

import lombok.AccessLevel;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final static Logger myLogger = LogManager.getLogger(Server.class);
    @Setter(AccessLevel.PUBLIC)
    private int portNumber;
    @Setter(AccessLevel.PUBLIC)
    private int threadPoolSize;
    private static Server instance = null;
    private ExecutorService myThreadPool;

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
                            Connection myConnection = new Connection(clientSocketChannel.socket(), validPaths);
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