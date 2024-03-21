package org.cloud.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.cloud.common.DaemonThreadFactory;
import org.cloud.common.FileUtils;
import org.cloud.model.*;

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

    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        FileUtils.sendFileToNetwork(currentDirectory, fileName, network.getOutputStream());
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
        serverView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = serverView.getSelectionModel().getSelectedItem();
                try {
                    network.getOutputStream().writeObject(new NavigationRequest(selected));
                } catch (IOException e) {
                    log.error("failed to send navigation message");
                }
            }
        });
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
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
            Platform.runLater(() -> fillView(clientView, FileUtils.getFilesFromDir(currentDirectory)));
        } else if (message instanceof FileCutMessage fileCut) {
            FileUtils.writeCutToFile(currentDirectory, fileCut.getFileName(), fileCut.getFileBytes(), fileCut.getCutSize());
            log.debug("file cut #{} from {} written", fileCut.getCutNumber(), fileCut.getFileName());
            if (fileCut.isLastCut()) {
                Platform.runLater(() -> fillView(clientView, FileUtils.getFilesFromDir(currentDirectory)));
                log.debug("FILE '{}' WRITTEN", fileCut.getFileName());
            }
        }
    }
}
