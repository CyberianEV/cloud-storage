package org.cloud.server;

import lombok.extern.slf4j.Slf4j;
import org.cloud.common.Commands;

import java.io.*;
import java.net.Socket;

@Slf4j
public class FileHandler implements Runnable {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private final String SERVER_DIR = "server_files";
    private final int BATCH_SIZE = 256;
    byte[] batch;

    public FileHandler(Socket socket) throws IOException{
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        File file = new File(SERVER_DIR);
        batch = new byte[BATCH_SIZE];
        if (!file.exists()) {
            file.mkdir();
        }
        log.info("Socket ready");
    }

    @Override
    public void run() {
        try {
            log.info("start listening...");
            while (true) {
                String command = dis.readUTF();
                log.info("Command received");
                if (command.equals(Commands.SEND_FILE)) {
                    String fileName = SERVER_DIR + "/" + dis.readUTF();
                    long size = dis.readLong();
                    log.info("file name and length received");
                    try (FileOutputStream fos = new FileOutputStream(fileName)) {
                        for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                            int bytesRead = dis.read(batch);
                            fos.write(batch, 0, bytesRead);
                        }
                        log.info("file written");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    log.error("Unknown command received: " + command);
                    throw new RuntimeException("Unknown command received: " + command);
                }
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
        }
    }
}
