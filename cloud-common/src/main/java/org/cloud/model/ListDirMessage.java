package org.cloud.model;

import lombok.Getter;
import org.cloud.common.FileUtils;

import java.nio.file.Path;
import java.util.List;

@Getter
public class ListDirMessage implements CloudMessage {
    private final List<String> files;

    public ListDirMessage(Path currentDir, Path topDir) {
        if (currentDir.equals(topDir)) {
            List<String> list = FileUtils.getFilesFromDir(currentDir.toString());
            list.remove(0);
            files = list;
        } else {
            files = FileUtils.getFilesFromDir(currentDir.toString());
        }
    }

    public ListDirMessage(List<String> files) {
        this.files = files;
    }

    @Override
    public MessageType getType() {
        return MessageType.LIST_DIR;
    }
}
