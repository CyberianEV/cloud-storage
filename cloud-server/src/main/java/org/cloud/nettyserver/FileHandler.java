package org.cloud.nettyserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FileUtils;
import org.cloud.common.NetworkHandler;
import org.cloud.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> implements NetworkHandler {

    private Path serverDir;
    private Path currentDir;
    ChannelHandlerContext ctx;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        serverDir = Path.of("server_files");
        currentDir = serverDir;
        writeToNetwork(new ListDirMessage(currentDir.toString(), this));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        this.ctx = ctx;
        log.debug("Received: {}", cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(currentDir.resolve(fileMessage.getFileName()), fileMessage.getFileBytes());
            writeToNetwork(new ListDirMessage(currentDir.toString(), this));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            FileUtils.sendFileToNetwork(currentDir.toString(), fileRequest.getFileName(), this);
        } else if (cloudMessage instanceof NavigationRequest navigationRequest) {
            Path dir = currentDir.resolve(Path.of(navigationRequest.getDirName()));
            if (Files.isDirectory(dir)) {
                currentDir = dir.normalize();
                writeToNetwork(new ListDirMessage(currentDir.toString(), this));
            }
        } else if (cloudMessage instanceof FileCutMessage fileCut) {
            FileUtils.writeCutToFile(currentDir.toString(), fileCut.getFileName(), fileCut.getFileBytes(),
                    fileCut.getCutSize());
            log.debug("file cut #{} from {} written", fileCut.getCutNumber(), fileCut.getFileName());
            if (fileCut.isLastCut()) {
                writeToNetwork(new ListDirMessage(currentDir.toString(), this));
                log.debug("FILE '{}' WRITTEN", fileCut.getFileName());
            }
        } else if (cloudMessage instanceof RenameRequest renameRequest) {
            Path file = currentDir.resolve(Path.of(renameRequest.getOldName()));
            Files.move(file, file.resolveSibling(renameRequest.getNewName()));
            writeToNetwork(new ListDirMessage(currentDir.toString(), this));
        } else if (cloudMessage instanceof DeleteRequest deleteRequest) {
            Path file = currentDir.resolve(Path.of(deleteRequest.getFileName()));
            Files.delete(file);
            writeToNetwork(new ListDirMessage(currentDir.toString(), this));
        }
    }

    @Override
    public <T extends CloudMessage> void writeToNetwork(T cloudMessage) {
        ctx.writeAndFlush(cloudMessage);
    }

    @Override
    public void addUpwardNavigation(List<String> list) {
        if (!currentDir.equals(serverDir)) {
            list.add("..");
        }
    }
}
