package org.mockserver.mockserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.mockserver.client.netty.proxy.ProxyConfiguration;
import org.mockserver.lifecycle.LifeCycle;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockserver.client.netty.proxy.ProxyConfiguration.proxyConfiguration;
import static org.mockserver.mock.action.ActionHandler.REMOTE_SOCKET;
import static org.mockserver.mockserver.MockServerHandler.PROXYING;
import static org.mockserver.proxy.relay.RelayConnectHandler.HTTP_CONNECT_SOCKET;

/**
 * @author jamesdbloom
 */
public class MockServer extends LifeCycle<MockServer> {

    private InetSocketAddress remoteSocket;

    /**
     * Start the instance using the ports provided
     *
     * @param localPorts the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final Integer... localPorts) {
        this(proxyConfiguration(), localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param proxyConfiguration the proxy configuration to send requests forwarded or proxied by MockServer via another proxy
     * @param localPorts         the local port(s) to use, use 0 or no vararg values to specify any free port
     */
    public MockServer(final ProxyConfiguration proxyConfiguration, final Integer... localPorts) {
        createServerBootstrap(proxyConfiguration, localPorts);
    }

    /**
     * Start the instance using the ports provided
     *
     * @param localPorts the local port(s) to use
     * @param remoteHost the hostname of the remote server to connect to
     * @param remotePort the port of the remote server to connect to
     */
    public MockServer(final String remoteHost, final Integer remotePort, final Integer... localPorts) {
        this(proxyConfiguration(), remoteHost, remotePort, localPorts);
    }

    /**
     * Start the instance using the ports provided configuring forwarded or proxied requests to go via an additional proxy
     *
     * @param localPorts the local port(s) to use
     * @param remoteHost the hostname of the remote server to connect to
     * @param remotePort the port of the remote server to connect to
     */
    public MockServer(final ProxyConfiguration proxyConfiguration, final String remoteHost, final Integer remotePort, final Integer... localPorts) {
        if (remoteHost == null) {
            throw new IllegalArgumentException("You must specify a remote port");
        }
        if (remotePort == null) {
            throw new IllegalArgumentException("You must specify a remote hostname");
        }

        remoteSocket = new InetSocketAddress(remoteHost, remotePort);
        createServerBootstrap(proxyConfiguration, localPorts);
    }

    private void createServerBootstrap(final ProxyConfiguration proxyConfiguration, final Integer... localPorts) {
        List<Integer> portBindings = singletonList(0);
        if (localPorts != null && localPorts.length > 0) {
            portBindings = Arrays.asList(localPorts);
        }

        serverBootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
            .childHandler(new MockServerUnificationInitializer(MockServer.this, httpStateHandler, proxyConfiguration))
            .childAttr(REMOTE_SOCKET, remoteSocket)
            .childAttr(PROXYING, remoteSocket != null);

        bindToPorts(portBindings);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                // Shut down all event loops to terminate all threads.
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();

                // Wait until all threads are terminated.
                try {
                    bossGroup.terminationFuture().sync();
                    workerGroup.terminationFuture().sync();
                } catch (InterruptedException e) {
                    // ignore interrupted exceptions
                }
            }
        }));
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteSocket;
    }

}
