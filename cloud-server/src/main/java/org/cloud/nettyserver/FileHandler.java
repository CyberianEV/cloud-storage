package org.cloud.nettyserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.common.FileUtils;
import org.cloud.model.*;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path serverDir;
    private Path currentDir;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        serverDir = Path.of("server_files");
        currentDir = serverDir;
        ctx.writeAndFlush(new ListDirMessage(currentDir, serverDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(currentDir.resolve(fileMessage.getFileName()), fileMessage.getFileBytes());
            ctx.writeAndFlush(new ListDirMessage(currentDir, serverDir));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            FileUtils.sendFileToNetwork(currentDir.toString(), fileRequest.getFileName(), ctx);
        } else if (cloudMessage instanceof NavigationRequest navigationRequest) {
            Path dir = currentDir.resolve(Path.of(navigationRequest.getDirName()));
            if (Files.isDirectory(dir)) {
                currentDir = dir.normalize();
                ctx.writeAndFlush(new ListDirMessage(currentDir, serverDir));
            }
        } else if (cloudMessage instanceof FileCutMessage fileCut) {
            FileUtils.writeCutToFile(currentDir.toString(), fileCut.getFileName(), fileCut.getFileBytes(),
                    fileCut.getCutSize());
            log.debug("file cut #{} from {} written", fileCut.getCutNumber(), fileCut.getFileName());
            if (fileCut.isLastCut()) {
                ctx.writeAndFlush(new ListDirMessage(currentDir, serverDir));
                log.debug("FILE '{}' WRITTEN", fileCut.getFileName());
            }
        }
    }
}
