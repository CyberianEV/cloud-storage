package org.cloud.model;

import lombok.Getter;

@Getter
public class FileCutMessage implements CloudMessage {
    private final String fileName;
    private final int cutSize;
    private final long cutNumber;
    private final byte[] fileBytes;
    private final boolean lastCut;

    public FileCutMessage(String fileName, int cutSize, byte[] fileBytes, long cutNumber, boolean lastCut) {
        this.fileName = fileName;
        this.cutSize = cutSize;
        this.fileBytes = fileBytes;
        this.lastCut = lastCut;
        this.cutNumber = cutNumber;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE_CUT;
    }
}
