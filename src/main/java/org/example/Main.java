package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Server.Request.RequestMethod;
import org.example.Server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final static Logger myLogger = LogManager.getLogger(Main.class);
    private static final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js",
            "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js", "/post.html"
    );

    public static void main(String[] args) {
        myLogger.info("Program started! To stop the server, enter: \"/stop\".");
        Server myServer = Server.getInstance();
        myServer.setPortNumber(9999);
        myServer.setThreadPoolSize(64);
        myServer.setValidPaths(validPaths);
        for (String validPath : validPaths) {
            if (validPath.equals("/classic.html")) {
                myServer.addHandler(RequestMethod.GET, validPath,(request, response) -> {
                    final Path filePath = Path.of(".", "public", request.getPath());
                    if (Files.exists(filePath)) {
                        try {
                            final String mimeType = Files.probeContentType(filePath);
                            final String template = Files.readString(filePath);
                            final byte[] content = template.replace(
                                    "{time}",
                                    LocalDateTime.now().toString()
                            ).getBytes();
                            response.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + content.length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            response.write(content);
                            response.flush();
                        } catch (IOException e) {
                            myLogger.error(e.getMessage());
                        }
                    } else {
                        myServer.notFound(response);
                    }
                });
            } else {
                myServer.addHandler(RequestMethod.GET, validPath,(request, response) -> {
                    final Path filePath = Path.of(".", "public", request.getPath());
                    if (Files.exists(filePath)) {
                        try {
                            final String mimeType = Files.probeContentType(filePath);
                            final long size = Files.size(filePath);
                            response.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + size + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            Files.copy(filePath, response);
                            response.flush();
                        } catch (IOException e) {
                            myLogger.error(e.getMessage());
                        }
                    } else {
                        myServer.notFound(response);
                    }
                });
            }
        }
        myServer.addHandler(RequestMethod.POST, "/post.html",(request, response) -> {
            final Path filePath = Path.of(".", "public", request.getPath());
            if (Files.exists(filePath)) {
                try {
                    final String mimeType = Files.probeContentType(filePath);
                    final long size = Files.size(filePath);
                    response.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + size + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, response);
                    response.flush();
                } catch (IOException e) {
                    myLogger.error(e.getMessage());
                }
            } else {
                myServer.notFound(response);
            }
        });
        myServer.start();
        Scanner myScanner = new Scanner(System.in);
        while (true) {
            String myInput = myScanner.nextLine();
            if (myInput.equals("/stop")) {
                myServer.interrupt();
                break;
            }
        }
        myLogger.info("Program finished!");
    }
}