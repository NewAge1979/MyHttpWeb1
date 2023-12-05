package org.example.Server.Handlers;

import org.example.Server.Request.Request;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedOutputStream response);
}
