package org.cloud.common;

import lombok.extern.slf4j.Slf4j;
import org.cloud.model.FileCutMessage;

import java.io.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {
    private static final int BATCH_SIZE = 256;
    private static final int CUT_SIZE = 1024*256;

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

    public static void sendFileToNetwork(String srcDirectory, String fileName, NetworkHandler networkHandler) {
        File file = new File(srcDirectory + "/" + fileName);
        if (file.isFile()) {
            long fileSize = file.length();
            byte[] bytes = new byte[CUT_SIZE];
            long numberOfCuts = (fileSize + CUT_SIZE - 1) / CUT_SIZE;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                for (int i = 1; i <= numberOfCuts; i++) {
                    int bytesRead = in.read(bytes);
                    networkHandler.writeToNetwork(new FileCutMessage(fileName, bytesRead, bytes, i,
                                i == numberOfCuts));
                    log.debug("file cut #{} / {} from {} sent", i, numberOfCuts, fileName);
                }
            } catch (IOException e) {
                log.error("file transfer interrupted", e);
            }
            log.debug("file '{}' sent", fileName);
        } else {
            log.debug("can't send a directory");
        }
    }

    public static void writeCutToFile(String dstDirectory, String fileName, byte[] cutBytes, int cutSize) {
        File file = new File(dstDirectory + "/" + fileName);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
            out.write(cutBytes, 0, cutSize);
            out.flush();
        } catch (IOException e) {
            log.error("file writing is interrupted", e);
        }
    }

    public static List<String> getFilesFromDir(String directory, NetworkHandler networkHandler) {
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
                networkHandler.addUpwardNavigation(list);
                list.addAll(directories);
                list.addAll(files);
                return list;
            }
        }
        return List.of();
    }
}
