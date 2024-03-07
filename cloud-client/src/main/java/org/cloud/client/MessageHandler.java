package org.cloud.client;

import javafx.application.Platform;
import org.cloud.common.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MessageHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CloudMainController.class);
    private Socket socket;
    private DataInputStream dis;
    private MessageHandlerListener listener;
    private boolean interrupted = false;

    private final int BATCH_SIZE = 256;
    byte[] batch;

    public MessageHandler(Socket socket, MessageHandlerListener listener) {
        this.socket = socket;
        this.listener = listener;
        batch = new byte[BATCH_SIZE];
    }
    @Override
    public void run() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            log.info("input stream created");
            log.info("Socket start listening...");
            while (!isInterrupted()) {
                String message = dis.readUTF();
                log.info("Message received: " + message);
                handleMessage(message);
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
        } finally {
            try {
                socket.close();
                log.info("Socket closed");
            } catch (IOException e) {
                log.debug("Failed to close the socket");
            }
            log.info("MessageHandler closed");
        }
    }

    private void handleMessage(String message) throws IOException{
        String[] messageArr = message.split(Commands.DELIMITER);
        String messageType = messageArr[0];
        if (messageType.equals(Commands.DIR_STRUCTURE)) {
            Platform.runLater(() -> {
                listener.onDirStructureReceived(message);
            });
        } else if (messageType.equals(Commands.SEND_FILE)) {
            String fileName = listener.getCurrentDirectory() + "/" + messageArr[1];
            long size = dis.readLong();
            log.info("file name and length received");
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                    int bytesRead = dis.read(batch);
                    fos.write(batch, 0, bytesRead);
                }
                log.info("file written");
                Platform.runLater(() -> {
                    listener.onReceivedFile();
                });
            } catch (IOException e) {
                log.error("failed to write file", e);
                throw new RuntimeException(e);
            }
        } else {
            log.error("Unknown message received: " + message);
            throw new RuntimeException("Unknown message received: " + message);
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
