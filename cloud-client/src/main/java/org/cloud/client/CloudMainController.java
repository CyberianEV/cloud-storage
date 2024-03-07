package org.cloud.client;

import org.cloud.common.Commands;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class CloudMainController implements Initializable, MessageHandlerListener {
    private static final Logger log = LoggerFactory.getLogger(CloudMainController.class);
    public ListView<String> clientView;
    public ListView<String> serverView;

    @Override
    public void onDirStructureReceived(String structure) {
        String filesString = structure.substring(Commands.DIR_STRUCTURE.length()
                + Commands.DELIMITER.length());
        List<String> files = new ArrayList<>(Arrays.asList(filesString.split(Commands.DELIMITER)));
        fillView(serverView, files);
    }

    @Override
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    @Override
    public void onReceivedFile() {
        fillView(clientView, getFiles(currentDirectory));
    }

    private String currentDirectory;

    MessageHandler messageHandler;
    private Socket socket;
    private DataOutputStream dos;
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

    public void closeHandler() {
        messageHandler.close();
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
            new Thread(messageHandler = new MessageHandler(socket, this), "msg-handler-thread").start();
            log.info("client socket created");
            dos = new DataOutputStream(socket.getOutputStream());
            log.info("output stream created");
        } catch (IOException e) {
            log.error("failed to connect to the server");
            throw new RuntimeException(e);
        }
    }
}
