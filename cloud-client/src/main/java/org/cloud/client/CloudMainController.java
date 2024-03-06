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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class CloudMainController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(CloudMainController.class);
    public ListView<String> clientView;
    public ListView<String> serverView;
    private String currentDirectory;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String host = "127.0.0.1";
    private int port = 8189;


    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(Commands.SEND_FILE);
                log.info("command sent");
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                log.info("file name and size sent");
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    log.info("file read to bytes");
                    dos.write(bytes);
                    log.info("file sent");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                log.debug("error" + e.getMessage());
            }
        }
    }

    public void retrieveFromServer(ActionEvent actionEvent) {
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
        view.getItems().setAll(data);
    }

    private void networkInit(String host, int port) {
        try {
            socket = new Socket(host, port);
            log.info("client socket created");
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            log.info("client socket ready");
        } catch (IOException e) {
            log.error("failed to connect to the server");
            throw new RuntimeException(e);
        }
    }
}
