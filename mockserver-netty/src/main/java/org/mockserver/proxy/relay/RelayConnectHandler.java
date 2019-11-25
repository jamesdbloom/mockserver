package org.mockserver.proxy.relay;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.AttributeKey;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.lifecycle.LifeCycle;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.LoggingHandler;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.NettySslContextFactory;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;

import static org.mockserver.exception.ExceptionHandler.shouldNotIgnoreException;
import static org.mockserver.mock.action.ActionHandler.REMOTE_SOCKET;
import static org.mockserver.mockserver.MockServerHandler.PROXYING;
import static org.mockserver.unification.PortUnificationHandler.isSslEnabledDownstream;
import static org.mockserver.unification.PortUnificationHandler.isSslEnabledUpstream;
import static org.slf4j.event.Level.DEBUG;

@ChannelHandler.Sharable
public abstract class RelayConnectHandler<T> extends SimpleChannelInboundHandler<T> {

    public static final AttributeKey<List<Future<Channel>>> HTTP_CONNECT_SOCKET = AttributeKey.valueOf("HTTP_CONNECT_SOCKET");
    private final LifeCycle server;
    private final MockServerLogger mockServerLogger;
    private final String host;
    private final int port;

    public RelayConnectHandler(LifeCycle server, MockServerLogger mockServerLogger, String host, int port) {
        this.server = server;
        this.mockServerLogger = mockServerLogger;
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext serverCtx, final T request) {
        Bootstrap bootstrap = new Bootstrap()
            .group(serverCtx.channel().eventLoop())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(final ChannelHandlerContext clientCtx) {
                    serverCtx.channel()
                        .writeAndFlush(successResponse(request))
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                removeCodecSupport(serverCtx);
                                serverCtx.channel().attr(PROXYING).set(Boolean.TRUE);

                                // downstream
                                ChannelPipeline downstreamPipeline = clientCtx.channel().pipeline();

                                if (isSslEnabledDownstream(serverCtx.channel())) {
                                    downstreamPipeline.addLast(new NettySslContextFactory(mockServerLogger).createClientSslContext().newHandler(clientCtx.alloc(), host, port));
                                }

                                if (MockServerLogger.isEnabled(Level.TRACE)) {
                                    downstreamPipeline.addLast(new LoggingHandler("downstream                -->"));
                                }

                                downstreamPipeline.addLast(new HttpClientCodec(ConfigurationProperties.maxInitialLineLength(), ConfigurationProperties.maxHeaderSize(), ConfigurationProperties.maxChunkSize()));

                                downstreamPipeline.addLast(new HttpContentDecompressor());

                                downstreamPipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

                                downstreamPipeline.addLast(new DownstreamProxyRelayHandler(mockServerLogger, serverCtx.channel()));


                                // upstream
                                ChannelPipeline upstreamPipeline = serverCtx.channel().pipeline();

                                if (isSslEnabledUpstream(serverCtx.channel())) {
                                    upstreamPipeline.addLast(new NettySslContextFactory(mockServerLogger).createServerSslContext().newHandler(serverCtx.alloc()));
                                }

                                if (MockServerLogger.isEnabled(Level.TRACE)) {
                                    upstreamPipeline.addLast(new LoggingHandler("upstream <-- "));
                                }

                                upstreamPipeline.addLast(new HttpServerCodec(ConfigurationProperties.maxInitialLineLength(), ConfigurationProperties.maxHeaderSize(), ConfigurationProperties.maxChunkSize()));

                                upstreamPipeline.addLast(new HttpContentDecompressor());

                                upstreamPipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

                                upstreamPipeline.addLast(new UpstreamProxyRelayHandler(mockServerLogger, serverCtx.channel(), clientCtx.channel()));
                            }
                        });
                }
            });

        final InetSocketAddress remoteSocket = getDownstreamSocket(serverCtx.channel());
        bootstrap.connect(remoteSocket).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    failure("Connection failed to " + remoteSocket, future.cause(), serverCtx, failureResponse(request));
                } else {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(LogEntry.LogMessageType.DEBUG)
                            .setLogLevel(DEBUG)
                            .setMessageFormat("Connected to {}")
                            .setArguments(remoteSocket)
                    );
                }
            }
        });
    }

    private InetSocketAddress getDownstreamSocket(Channel channel) {
        if (channel.attr(REMOTE_SOCKET).get() != null) {
            return channel.attr(REMOTE_SOCKET).get();
        } else {
            return new InetSocketAddress(server.getLocalPort());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        failure("Exception caught by CONNECT proxy handler -> closing pipeline ", cause, ctx, failureResponse(null));
    }

    private void failure(String message, Throwable cause, ChannelHandlerContext ctx, Object response) {
        if (shouldNotIgnoreException(cause)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(LogEntry.LogMessageType.EXCEPTION)
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat(message)
                    .setThrowable(cause)
            );
        }
        Channel channel = ctx.channel();
        channel.writeAndFlush(response);
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected abstract void removeCodecSupport(ChannelHandlerContext ctx);

    protected abstract Object successResponse(Object request);

    protected abstract Object failureResponse(Object request);

    protected void removeHandler(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerType) {
        if (pipeline.get(handlerType) != null) {
            pipeline.remove(handlerType);
        }
    }

    protected void removeHandler(ChannelPipeline pipeline, ChannelHandler channelHandler) {
        if (pipeline.toMap().containsValue(channelHandler)) {
            pipeline.remove(channelHandler);
        }
    }

}
