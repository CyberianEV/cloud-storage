package org.cloud.server;

import org.cloud.common.Commands;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class CloudServer {
    public static void main(String[] args) {
        int port = 8189;

        ThreadFactory serviceThreadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("file-handler-thread-%");
            thread.setDaemon(true);
            return thread;
        };

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server started");
            while (true) {
                Socket socket = serverSocket.accept();
                log.info("Socket accepted");
                serviceThreadFactory.newThread(new FileHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
