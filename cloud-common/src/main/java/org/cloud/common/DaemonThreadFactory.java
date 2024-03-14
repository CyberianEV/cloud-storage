package org.cloud.common;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }

    public Thread getNamedThread(Runnable r, String name) {
        Thread thread = new Thread(r);
        thread.setName(name);
        thread.setDaemon(true);
        return thread;
    }
}
