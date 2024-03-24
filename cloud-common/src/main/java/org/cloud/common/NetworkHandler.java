package org.cloud.common;

import org.cloud.model.CloudMessage;

import java.util.List;

public interface NetworkHandler {
    <T extends CloudMessage> void writeToNetwork(T cloudMessage);

    void addUpwardNavigation(List<String> list);
}
