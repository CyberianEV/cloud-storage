package org.cloud.server;

import lombok.extern.slf4j.Slf4j;
import org.cloud.common.Commands;
import org.cloud.common.FileUtils;

import java.io.*;
import java.net.Socket;

@Slf4j
public class FileHandler implements Runnable {
    private Socket socket;
//    private boolean interrupted = false;
    private DataInputStream dis;
    private DataOutputStream dos;

    private final String SERVER_DIR = "server_files";

    public FileHandler(Socket socket) {
        this.socket = socket;
        File file = new File(SERVER_DIR);
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
            while (true) {
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
        String[] messageArr = command.split(Commands.DELIMITER);
        String commandType = messageArr[0];
        if (commandType.equals(Commands.SEND_FILE)) {
            FileUtils.readFileFromStream(SERVER_DIR, messageArr[1], dis);
            sendDirectoryStructure(SERVER_DIR);
        } else if (commandType.equals(Commands.RETRIEVE_FILE)) {
            String fileName = messageArr[1];
            String filePath = SERVER_DIR + "/" + fileName;
            File file = new File(filePath);
            if (file.isFile()) {
                try {
                    dos.writeUTF(Commands.getSendFile(fileName));
                    dos.writeLong(file.length());
                    log.info("command and size sent");
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] bytes = fis.readAllBytes();
                        dos.write(bytes);
                        log.info("file sent");
                    } catch (IOException e) {
                        log.error("failed to send file", e);
                        throw new RuntimeException(e);
                    }
                }  catch (Exception e) {
                    log.debug("failed to send command", e);
                }
            }
        } else {
            log.error("Unknown command received: " + command);
            throw new RuntimeException("Unknown command received: " + command);
        }
    }

    private void sendDirectoryStructure (String directory) {
        File file = new File(directory);
        if (file.isDirectory()) {
            String[] list = file.list();
            assert list != null;
            String directoryContent = buildStringWithContent(list);
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

//    private boolean isInterrupted() {
//        return interrupted;
//    }

//    private void interrupt() {
//        interrupted = true;
//    }

//    public synchronized void close() {
//        interrupt();
//        try {
//            socket.close();
//        } catch (IOException e) {
//            log.debug("Failed to close the socket");
//        }
//    }
}
