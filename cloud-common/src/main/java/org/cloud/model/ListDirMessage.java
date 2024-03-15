package org.cloud.model;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ListDirMessage implements CloudMessage {
    private final List<String> files;

    public ListDirMessage(Path path) throws IOException {
        files = Files.list(path)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    @Override
    public MessageType getType() {
        return MessageType.LIST_DIR;
    }
}
