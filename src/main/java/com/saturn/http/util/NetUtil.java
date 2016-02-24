package com.saturn.http.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import org.slf4j.Logger;
import com.saturn.util.log.Log;

/**
 * @author ericcoderr@gmail.com
 * @since 2011-4-15 下午10:13:22
 */
public class NetUtil {

    private static Logger log = Log.getLogger();

    /**
     * 校验端口是否被占用,如果被占用程序退出
     * 
     * @throws IOException
     */
    public static void checkSocketPortBind(boolean exitWhenError, int... ports) throws IOException {
        if (System.getProperty("os.name").startsWith("Win")) {
            // 在linux中不用判断是否被占用
            for (int port : ports) {
                if (port > 0) {
                    boolean isBind = true;
                    Socket socket = null;
                    try {
                        socket = new Socket("localhost", port);
                    } catch (ConnectException e1) {
                        isBind = false;
                    } catch (Exception e1) {
                        log.error("", e1);
                        isBind = true;
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                    if (isBind) {
                        String errStr = "Failed to bind to " + port;
                        if (exitWhenError) {
                            System.err.println(errStr);
                            System.exit(1);
                        } else {
                            throw new RuntimeException(errStr);
                        }
                    }
                }
            }
        }
    }

    /**
     * 校验端口是否被占用,如果被占用程序退出
     * 
     * @throws IOException
     */
    public static void checkSocketPortBind(int... ports) throws IOException {
        checkSocketPortBind(false, ports);
    }
}
