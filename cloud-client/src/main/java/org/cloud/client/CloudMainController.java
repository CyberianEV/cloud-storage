package org.cloud.client;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.Commands;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.cloud.common.DaemonThreadFactory;
import org.cloud.common.FileUtils;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

@Slf4j
public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;

    DaemonThreadFactory factory = new DaemonThreadFactory();

    private String currentDirectory;

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String host = "127.0.0.1";
    private int port = 8189;


    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(Commands.getSendFile(fileName));
                dos.writeLong(file.length());
                log.info("command size sent");
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                    log.info("file sent");
                } catch (IOException e) {
                    log.error("failed to send file", e);
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                log.debug("failed to send command", e);
            }
        }
    }

    public void retrieveFromServer(ActionEvent actionEvent) {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        try {
            dos.writeUTF(Commands.getRetrieveFile(fileName));
        } catch (IOException e) {
            log.debug("failed to send command", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setCurrentDirectory(System.getProperty("user.home"));
        networkInit(host, port);
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = currentDirectory + "/" + clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(selected);
                }
            }
        });
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFiles(currentDirectory));
    }

    private List<String> getFiles(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>();
                files.add("..");
                files.addAll(Arrays.asList(list));
                return files;
            }
        }
        return List.of();
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        Collections.sort(data);
        view.getItems().setAll(data);
    }

    private void networkInit(String host, int port) {
        try {
            socket = new Socket(host, port);
            factory.getNamedThread(this::readMessages,
                    "client-listener-thread"
            ).start();
        } catch (IOException e) {
            log.error("failed to connect to the server");
            throw new RuntimeException(e);
        }
    }

    private void readMessages() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            log.info("Socket ready");
            log.info("Socket start listening...");
            while (true) {
                String message = dis.readUTF();
                log.info("Message received: " + message);
                handleMessage(message);
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
        }
    }

    private void handleMessage(String message) throws IOException{
        String[] messageArr = message.split(Commands.DELIMITER);
        String messageType = messageArr[0];
        if (messageType.equals(Commands.DIR_STRUCTURE)) {
            String filesString = message.substring(Commands.DIR_STRUCTURE.length()
                    + Commands.DELIMITER.length());
            List<String> files = new ArrayList<>(Arrays.asList(filesString.split(Commands.DELIMITER)));
            Platform.runLater(() -> fillView(serverView, files));
        } else if (messageType.equals(Commands.SEND_FILE)) {
            FileUtils.readFileFromStream(currentDirectory, messageArr[1], dis);
            Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
        } else {
            log.error("Unknown message received: " + message);
            throw new RuntimeException("Unknown message received: " + message);
        }
    }
}
