package org.cloud.model;

import lombok.Getter;

@Getter
public class DeleteRequest implements CloudMessage {
    private final String fileName;

    public DeleteRequest(String fileName) {
        this.fileName = fileName;
    }
    @Override
    public MessageType getType() {
        return MessageType.DELETE;
    }
}
