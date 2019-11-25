package org.mockserver.mockserver;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.mockserver.mock.action.ActionHandler;
import org.mockserver.lifecycle.LifeCycle;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.socket.tls.NettySslContextFactory;
import org.mockserver.socket.tls.SniHandler;
import org.mockserver.unification.PortUnificationHandler;

@ChannelHandler.Sharable
public class MockServerUnificationInitializer extends ChannelHandlerAdapter {
    private final LifeCycle server;
    private final HttpStateHandler httpStateHandler;
    private final ActionHandler actionHandler;
    private final NettySslContextFactory nettySslContextFactory;

    public MockServerUnificationInitializer(LifeCycle server, HttpStateHandler httpStateHandler, ActionHandler actionHandler) {
        this.server = server;
        this.httpStateHandler = httpStateHandler;
        this.actionHandler = actionHandler;
        this.nettySslContextFactory = new NettySslContextFactory(httpStateHandler.getMockServerLogger());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ctx.pipeline().replace(this, null, new PortUnificationHandler(server, httpStateHandler, actionHandler, nettySslContextFactory));
    }
}
