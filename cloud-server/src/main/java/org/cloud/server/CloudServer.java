package org.cloud.server;

import lombok.extern.slf4j.Slf4j;
import org.cloud.common.DaemonThreadFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class CloudServer {
    public static void main(String[] args) {
        int port = 8189;
        int counter = 0;

        DaemonThreadFactory serviceThreadFactory = new DaemonThreadFactory();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server started");
            while (true) {
                Socket socket = serverSocket.accept();
                log.info("Socket accepted");
                serviceThreadFactory.getNamedThread(
                        new FileHandler(socket),
                        "file-handler-thread-" + counter++
                ).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
