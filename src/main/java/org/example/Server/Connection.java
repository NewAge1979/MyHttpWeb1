package org.example.Server;

import org.apache.commons.fileupload.FileItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Server.Request.Request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.stream.Collectors;

public class Connection extends Thread {
    private final static Logger myLogger = LogManager.getLogger(Connection.class);
    private final Socket clientSocket;

    public Connection(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (clientSocket;
             BufferedInputStream myRequest = new BufferedInputStream(clientSocket.getInputStream());
             BufferedOutputStream myResponse = new BufferedOutputStream(clientSocket.getOutputStream())
        ) {
            final int limit = 4096;
            myRequest.mark(limit);
            final byte[] buffer = new byte[limit];
            final int read = myRequest.read(buffer);
            Request myRequestObj = Request.getRequest(buffer, read, myRequest);
            if (
                    myRequestObj == null ||
                            (myRequestObj.getParts() != null &&
                                    myRequestObj.getParts().stream()
                                            .collect(Collectors.groupingBy(FileItem::getFieldName))
                                            .values()
                                            .stream().anyMatch(x -> x.size() > 1)
                            )
            ) {
                myLogger.debug("Bad Request");
                Server myServer = Server.getInstance();
                myServer.badRequest(myResponse);
                return;
            }
            final Optional<String> contentType = myRequestObj.getHeader("Content-Type");
            if (contentType.isPresent() && contentType.get().startsWith("multipart/form-data")) {
                myLogger.info(String.format("Login = \"%s\"", myRequestObj.getPart("login")));
                myLogger.info(String.format("Password = \"%s\"", myRequestObj.getPart("password")));
                myLogger.info(String.format("File = \"%s\"", myRequestObj.getPart("foto")));
                myLogger.info(myRequestObj.getParts().toString());
            } else {
                myLogger.info(myRequestObj.toString());
            }
            Server.getInstance().runHandler(myRequestObj, myResponse);
        } catch (IOException e) {
            myLogger.error(String.format("Error message: %s", e.getMessage()));
        }
    }
}