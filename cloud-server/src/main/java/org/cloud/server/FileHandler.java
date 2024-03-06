package org.cloud.server;

import lombok.extern.slf4j.Slf4j;
import org.cloud.common.Commands;

import java.io.*;
import java.net.Socket;

@Slf4j
public class FileHandler implements Runnable {
    private Socket socket;
    private boolean interrupted = false;
    private DataInputStream dis;
    private DataOutputStream dos;

    private final String SERVER_DIR = "server_files";
    private final int BATCH_SIZE = 256;
    byte[] batch;

    public FileHandler(Socket socket) {
        this.socket = socket;
        File file = new File(SERVER_DIR);
        batch = new byte[BATCH_SIZE];
        if (!file.exists()) {
            file.mkdir();
        }
    }

    @Override
    public void run() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            sendDirectoryStructure(SERVER_DIR);
            log.info("Socket start listening...");
            while (!isInterrupted()) {
                String command = dis.readUTF();
                log.info("Command received: " + command);
                handleCommand(command);
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("Failed to close the socket");
            }
            log.info("Socket closed");
        }
    }

    private void handleCommand(String command) throws IOException {
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
                sendDirectoryStructure(SERVER_DIR);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.error("Unknown command received: " + command);
            throw new RuntimeException("Unknown command received: " + command);
        }
    }

    private void sendDirectoryStructure (String directory) {
        File file = new File(directory);
        if (file.isDirectory()) {
            String directoryContent = buildStringWithContent(file.list());
            send(Commands.getDirStructure(directoryContent));
        }
    }

    private String buildStringWithContent (String[] content) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length; i++) {
            sb.append(content[i]).append(Commands.DELIMITER);
        }
        return sb.toString();
    }

    public boolean send(String msg) {
        try {
            dos.writeUTF(msg);
            dos.flush();
            return true;
        } catch (IOException e) {
            log.error("message failure");
            return false;
        }
    }

    private boolean isInterrupted() {
        return interrupted;
    }

    private void interrupt() {
        interrupted = true;
    }

    public synchronized void close() {
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            log.debug("Failed to close the socket");
        }
    }
}
