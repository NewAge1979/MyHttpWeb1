package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.Server.Server;

import java.util.Scanner;

public class Main {
    private final static Logger myLogger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        myLogger.info("Program started! To stop the server, enter: \"/end\".");
        Server myServer = Server.getInstance();
        myServer.setPortNumber(9999);
        myServer.setThreadPoolSize(64);
        myServer.start();
        Scanner myScanner = new Scanner(System.in);
        while (true) {
            String myInput = myScanner.nextLine();
            if (myInput.equals("/end")) {
                myServer.interrupt();
                break;
            } else {
                continue;
            }
        }
        myLogger.info("Program finished!");
    }
}