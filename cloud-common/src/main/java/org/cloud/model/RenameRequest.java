package org.cloud.model;

import lombok.Getter;

@Getter
public class RenameRequest implements CloudMessage {
    private final String oldName;
    private final String newName;

    public RenameRequest(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }
    @Override
    public MessageType getType() {
        return MessageType.RENAME;
    }
}
