package org.javafour;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TerminalServer {

    private Path current;
    private ServerSocketChannel server;
    private Selector selector;

    private ByteBuffer buf;

    public TerminalServer() throws IOException {
        current = Path.of("server_files");
        buf = ByteBuffer.allocate(256);
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                keyIterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        buf.clear();
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = channel.read(buf);
            if (read == 0) {
                break;
            }
            if (read == -1) {
                channel.close();
                return;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                sb.append((char) buf.get());
            }
            buf.clear();
        }
        log.info("Received: " + sb);
        String[] arrMessage = sb.toString().trim().split(" ");
        String command = arrMessage[0];
        String msg;
        if (arrMessage.length < 2 && !command.equals("ls")) {
            msg = "wrong parameter\n";
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
            return;
        }
        if (command.equals("ls")) {
            String files = Files.list(current)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.joining("\r\n")) + "\n";
            channel.write(ByteBuffer.wrap(files.getBytes(StandardCharsets.UTF_8)));
        } else if (command.equals("cd")) {
            Path newPath = current.resolve(Path.of(arrMessage[1]));
            if (Files.isDirectory(newPath)) {
                current = newPath.normalize().toAbsolutePath();
                msg = current + "\n";
            } else {
                msg = "non-existent directory: " + newPath + "\n";
            }
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        } else if (command.equals("touch")) {
            String fileName = arrMessage[1];
            try {
                Files.createFile(current.resolve(fileName));
                msg = "file " + fileName + " has been created\n";
            } catch (FileAlreadyExistsException e) {
                msg = "file already existed\n";
            }
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        } else if (command.equals("mkdir")) {
            String dirName = arrMessage[1];
            try {
                Files.createDirectory(current.resolve(dirName));
                msg = "directory " + dirName + " has been created\n";
            } catch (FileAlreadyExistsException e) {
                msg = "directory already existed\n";
            }
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        } else if (command.equals("cat")) {
            Path filePath = current.resolve(arrMessage[1]);
            if (Files.isRegularFile(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                channel.write(ByteBuffer.wrap(bytes));
                channel.write(ByteBuffer.wrap("\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                msg = "non-existent file: " + filePath + "\n";
                channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
            }
        } else {
            msg = "received: " + command + "\n";
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel socketChannel = server.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        log.info("client accepted");
    }

    public static void main(String[] args) {
        try {
            new TerminalServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
