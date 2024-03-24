package org.cloud.model;

import lombok.Getter;
import org.cloud.common.FileUtils;
import org.cloud.common.NetworkHandler;

import java.util.List;

@Getter
public class ListDirMessage implements CloudMessage {
    private final List<String> files;

    public ListDirMessage(String directory, NetworkHandler networkHandler) {
        files = FileUtils.getFilesFromDir(directory, networkHandler);
    }

    public ListDirMessage(List<String> files) {
        this.files = files;
    }

    @Override
    public MessageType getType() {
        return MessageType.LIST_DIR;
    }
}
