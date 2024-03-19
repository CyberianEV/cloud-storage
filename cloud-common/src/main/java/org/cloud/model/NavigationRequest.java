package org.cloud.model;

import lombok.Getter;

@Getter
public class NavigationRequest implements CloudMessage {
    private final String dirName;

    public NavigationRequest(String dirName) {
        this.dirName = dirName;
    }

    @Override
    public MessageType getType() {
        return MessageType.NAVIGATION;
    }
}
