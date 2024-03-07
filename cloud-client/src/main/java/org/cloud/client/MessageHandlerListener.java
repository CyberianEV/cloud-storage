package org.cloud.client;

public interface MessageHandlerListener {
    void onDirStructureReceived(String structure);
    String getCurrentDirectory();
    void onReceivedFile();
}
