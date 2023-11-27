package org.example.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Connection extends Thread {
    private final static Logger myLogger = LogManager.getLogger(Connection.class);
    private final Socket clientSocket;
    private final List<String> validPaths;

    public Connection(Socket clientSocket, List<String> validPaths) {
        this.clientSocket = clientSocket;
        this.validPaths = validPaths;
    }

    @Override
    public void run() {
        try (clientSocket;
             BufferedReader request = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedOutputStream response = new BufferedOutputStream(clientSocket.getOutputStream())
        ) {
            final String requestLine = request.readLine();
            final String[] requestParts = requestLine.split(" ");
            if (requestParts.length == 3) {
                for (int i = 0; i < requestParts.length; i++) {
                    myLogger.info(String.format("requestParts[%d] = %s", i, requestParts[i]));
                }
                final String currentPath = requestParts[1];
                if (!validPaths.contains(currentPath)) {
                    response.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    response.flush();
                    myLogger.info("Error 404!");
                } else {
                    final Path filePath = Path.of(".", "public", currentPath);
                    final String mimeType = Files.probeContentType(filePath);

                    if (currentPath.equals("/classic.html")) {
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
                    } else {
                        final long length = Files.size(filePath);
                        response.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        Files.copy(filePath, response);
                        response.flush();
                    }
                }
            }
        } catch (IOException e) {
            myLogger.error(String.format("Error message: %s", e.getMessage()));
        }
    }
}