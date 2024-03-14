package org.cloud.common;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class FileUtils {
    private static final int BATCH_SIZE = 256;

    public static void readFileFromStream (String dstDirectory, String fileName, DataInputStream dis) throws IOException {
        byte[] batch = new byte[BATCH_SIZE];
        String path = dstDirectory + "/" + fileName;
        long size = dis.readLong();
        log.info("file name and length received");
        try (FileOutputStream fos = new FileOutputStream(path)) {
            for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                int bytesRead = dis.read(batch);
                fos.write(batch, 0, bytesRead);
            }
            log.info("file written");
        } catch (
                IOException e) {
            log.error("failed to write file", e);
            throw new RuntimeException(e);
        }
    }
}
