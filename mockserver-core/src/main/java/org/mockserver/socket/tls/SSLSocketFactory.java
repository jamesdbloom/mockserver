package org.mockserver.socket.tls;

import com.google.common.annotations.VisibleForTesting;
import org.mockserver.logging.MockServerLogger;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;

/**
 * @author jamesdbloom
 */
public class SSLSocketFactory {

    public static SSLSocketFactory sslSocketFactory() {
        return new SSLSocketFactory();
    }

    @VisibleForTesting
    public synchronized SSLSocket wrapSocket(Socket socket) throws IOException {
        // ssl socket factory
        javax.net.ssl.SSLSocketFactory sslSocketFactory = new KeyStoreFactory(new MockServerLogger()).sslContext().getSocketFactory();

        // ssl socket
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        sslSocket.setUseClientMode(true);
        sslSocket.startHandshake();
        return sslSocket;
    }
}
