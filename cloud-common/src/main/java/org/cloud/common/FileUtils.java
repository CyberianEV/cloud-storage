package org.cloud.common;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

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

    public static List<String> getFilesFromDir(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            File[] content = dir.listFiles();
            if (content != null) {
                List<String> directories = new ArrayList<>();
                List<String> files = new ArrayList<>();
                for (File file : content) {
                    if (file.isDirectory()) {
                        directories.add(file.getName());
                    } else {
                        files.add(file.getName());
                    }
                }
                directories.sort(Collator.getInstance());
                files.sort(Collator.getInstance());
                List<String> list = new ArrayList<>();
                list.add("..");
                list.addAll(directories);
                list.addAll(files);
                return list;
            }
        }
        return List.of();
    }
}
