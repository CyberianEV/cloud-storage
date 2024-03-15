package org.cloud.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.Commands;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.cloud.common.DaemonThreadFactory;
import org.cloud.common.FileUtils;
import org.cloud.model.CloudMessage;
import org.cloud.model.FileMessage;
import org.cloud.model.FileRequest;
import org.cloud.model.ListDirMessage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;

    DaemonThreadFactory factory = new DaemonThreadFactory();

    private String currentDirectory;

    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;
    private Socket socket;
    private String host = "127.0.0.1";
    private int port = 8189;


    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        Path filePath = Path.of(currentDirectory).resolve(fileName);
        network.getOutputStream().writeObject(new FileMessage(filePath));
    }

    public void retrieveFromServer(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName));
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
            network = new Network<>(
                    new ObjectDecoderInputStream(socket.getInputStream()),
                    new ObjectEncoderOutputStream(socket.getOutputStream())
            );
            log.info("Socket ready");
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
            log.info("Socket start listening...");
            while (true) {
                CloudMessage message = (CloudMessage) network.getInputStream().readObject();
                log.info("Message with type {} received: ", message.getType());
                handleMessage(message);
            }
        } catch (IOException e) {
            log.debug("Client disconnected");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(CloudMessage message) throws IOException{
        if (message instanceof ListDirMessage listDirMessage) {
            Platform.runLater(() -> fillView(serverView, listDirMessage.getFiles()));
        } else if (message instanceof FileMessage fileMessage) {
            Files.write(Path.of(currentDirectory).resolve(fileMessage.getFileName()), fileMessage.getFileBytes());
            Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
        }
    }
}
