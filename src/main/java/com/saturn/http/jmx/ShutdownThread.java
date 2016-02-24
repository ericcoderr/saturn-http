//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.saturn.http.jmx;

import io.netty.channel.EventLoopGroup;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import com.saturn.util.log.Log;

/* ------------------------------------------------------------ */
/**
 * <pre>
 * ShutdownThread is a shutdown hook thread implemented as singleton that maintains a list of lifecycle instances that are registered with it and provides ability to stop these lifecycles upon
 * shutdown of the Java Virtual Machine
 * 
 * 此类在Jetty ShutdownThread上修改而来
 * </pre>
 */
public class ShutdownThread extends Thread {

    private static final Logger LOG = Log.getLogger(ShutdownThread.class);
    private static final ShutdownThread _thread = new ShutdownThread();

    private boolean _hooked;
    private final List<EventLoopGroup> _eventLoopGroups = new CopyOnWriteArrayList<EventLoopGroup>();

    private static boolean isSystemExit = false;

    /* ------------------------------------------------------------ */
    /**
     * Default constructor for the singleton Registers the instance as shutdown hook with the Java Runtime
     */
    private ShutdownThread() {
    }

    /* ------------------------------------------------------------ */
    private synchronized void hook() {
        try {
            if (!_hooked)
                Runtime.getRuntime().addShutdownHook(this);
            _hooked = true;
        } catch (Exception e) {
            LOG.info("shutdown already commenced");
        }
    }

    /* ------------------------------------------------------------ */
    private synchronized void unhook() {
        try {
            _hooked = false;
            Runtime.getRuntime().removeShutdownHook(this);
        } catch (Exception e) {
            LOG.debug("shutdown already commenced");
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the instance of the singleton
     * 
     * @return the singleton instance of the {@link ShutdownThread}
     */
    public static ShutdownThread getInstance(boolean isSystemExit) {
        ShutdownThread.isSystemExit = isSystemExit;
        return _thread;
    }

    public static ShutdownThread getInstance() {
        return _thread;
    }

    public static boolean isSystemExit() {
        return isSystemExit;
    }

    /* ------------------------------------------------------------ */
    public static synchronized void register(EventLoopGroup... eventLoopGroups) {
        _thread._eventLoopGroups.addAll(Arrays.asList(eventLoopGroups));
        if (_thread._eventLoopGroups.size() > 0)
            _thread.hook();
    }

    /* ------------------------------------------------------------ */
    public static synchronized void register(int index, EventLoopGroup... eventLoopGroups) {
        _thread._eventLoopGroups.addAll(index, Arrays.asList(eventLoopGroups));
        if (_thread._eventLoopGroups.size() > 0)
            _thread.hook();
    }

    /* ------------------------------------------------------------ */
    public static synchronized void deregister(EventLoopGroup eventLoopGroup) {
        _thread._eventLoopGroups.remove(eventLoopGroup);
        if (_thread._eventLoopGroups.size() == 0)
            _thread.unhook();
    }

    /* ------------------------------------------------------------ */
    /**
     * 如果是jmx调用，不建议Sytem.exit(0),会关闭jvm
     */
    @Override
    public void run() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("开始停止服务器...当前时间为 : %s\r\n", new Date()));
        long begin = System.currentTimeMillis();
        for (EventLoopGroup eventLoopGroup : _thread._eventLoopGroups) {
            try {
                if (!eventLoopGroup.isShutdown()) {
                    eventLoopGroup.shutdownGracefully();
                }
            } catch (Exception ex) {
                LOG.debug("", ex);
            }
        }
        sb.append(String.format("服务器停止完毕,耗时... %d ms", System.currentTimeMillis() - begin));
        System.out.println(sb.toString());
        if (isSystemExit()) {
            System.exit(0);
        }
    }
}
