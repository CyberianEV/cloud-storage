package org.cloud.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import org.cloud.common.DaemonThreadFactory;
import org.cloud.common.FileUtils;
import org.cloud.common.NetworkHandler;
import org.cloud.model.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class CloudMainController implements Initializable, NetworkHandler {
    @FXML
    private ListView<String> clientView;
    @FXML
    private ListView<String> serverView;

    DaemonThreadFactory factory = new DaemonThreadFactory();

    private String currentDirectory;

    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;
    private Socket socket;
    private String host = "127.0.0.1";
    private int port = 8189;

    public void handleUploadButton(ActionEvent actionEvent) {
        uploadFile();
    }

    public void handleDownloadButton(ActionEvent actionEvent) {
        downloadFile();
    }

    public void handleClientViewUploadMI(ActionEvent actionEvent) {
        uploadFile();
    }
    public void handleClientViewRenameMI(ActionEvent actionEvent) {
        renameFile(SideType.CLIENT);
    }

    public void handleClientViewDeleteMI(ActionEvent actionEvent) {
        deleteFile(SideType.CLIENT);
    }

    public void handleClientViewKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.DELETE)) {
            deleteFile(SideType.CLIENT);
        }
    }

    public void handleServerViewDownloadMI(ActionEvent actionEvent) {
        downloadFile();
    }

    public void handleServerViewRenameMI(ActionEvent actionEvent) {
        renameFile(SideType.SERVER);
    }

    public void handleServerViewDeleteMI(ActionEvent actionEvent) {
        deleteFile(SideType.SERVER);
    }

    public void handleServerViewKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.DELETE)) {
            deleteFile(SideType.SERVER);
        }
    }

    @Override
    public <T extends CloudMessage> void writeToNetwork(T cloudMessage) {
        try {
            network.getOutputStream().writeObject(cloudMessage);
        } catch (IOException e) {
            log.error("failed to send {} to server", cloudMessage.getType(), e);
        }
    }

    @Override
    public void addUpwardNavigation(List<String> list) {
        list.add("..");
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
                writeToNetwork(new NavigationRequest(selected));
            }
        });
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
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
            Platform.runLater(() -> fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this)));
        } else if (message instanceof FileCutMessage fileCut) {
            FileUtils.writeCutToFile(currentDirectory, fileCut.getFileName(), fileCut.getFileBytes(), fileCut.getCutSize());
            log.debug("file cut #{} from {} written", fileCut.getCutNumber(), fileCut.getFileName());
            if (fileCut.isLastCut()) {
                Platform.runLater(() -> fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this)));
                log.debug("FILE '{}' WRITTEN", fileCut.getFileName());
            }
        }
    }

    private void uploadFile() {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        FileUtils.sendFileToNetwork(currentDirectory, fileName, this);
    }

    private void downloadFile() {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        writeToNetwork(new FileRequest(fileName));
    }

    private void renameFile(SideType sideType) {
        String fileName = getSelectedFileName(sideType);
        TextInputDialog renameDialog = new TextInputDialog(fileName);
        renameDialog.setHeaderText("Rename?");
        renameDialog.showAndWait()
                .ifPresent(response -> {
                    if (sideType.equals(SideType.CLIENT)) {
                        File file = new File(currentDirectory + "/" + fileName);
                        file.renameTo(new File(currentDirectory, response));
                        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
                    } else if (sideType.equals(SideType.SERVER)) {
                        writeToNetwork(new RenameRequest(fileName, response));
                    }
                });
    }

    private void deleteFile(SideType sideType) {
        String fileName = getSelectedFileName(sideType);
        Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + fileName +"?");
        deleteAlert.setHeaderText("Delete?");
        deleteAlert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    if (sideType.equals(SideType.CLIENT)) {
                        new File(currentDirectory + "/" + fileName).delete();
                        fillView(clientView, FileUtils.getFilesFromDir(currentDirectory, this));
                    } else if (sideType.equals(SideType.SERVER)) {
                        writeToNetwork(new DeleteRequest(fileName));
                    }
                });
    }

    private String getSelectedFileName(SideType side) {
        if (side.equals(SideType.CLIENT)) {
            return clientView.getSelectionModel().getSelectedItem();
        } else if (side.equals(SideType.SERVER)) {
            return serverView.getSelectionModel().getSelectedItem();
        } else {
            log.error("unknown side type received");
            throw new RuntimeException("unknown side type received");
        }
    }
}
