package org.mockserver.mockserver;

import com.google.common.net.MediaType;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.mockserver.client.netty.proxy.ProxyConfiguration;
import org.mockserver.client.serialization.PortBindingSerializer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.mock.action.ActionHandler;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.PortBinding;
import org.mockserver.proxy.connect.HttpConnectHandler;
import org.mockserver.responsewriter.NettyResponseWriter;
import org.mockserver.responsewriter.ResponseWriter;
import org.mockserver.socket.KeyAndCertificateFactory;

import javax.annotation.Nullable;
import java.net.BindException;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.mockserver.exception.ExceptionHandler.closeOnFlush;
import static org.mockserver.exception.ExceptionHandler.shouldNotIgnoreException;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.PortBinding.portBinding;
import static org.mockserver.proxy.Proxy.*;
import static org.mockserver.unification.PortUnificationHandler.enabledSslUpstreamAndDownstream;

/**
 * @author jamesdbloom
 */
@ChannelHandler.Sharable
public class MockServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private MockServerLogger mockServerLogger;
    // generic handling
    private HttpStateHandler httpStateHandler;
    // serializers
    private PortBindingSerializer portBindingSerializer;
    // server
    private MockServer server;
    // expectations
    private ActionHandler actionHandler;

    public MockServerHandler(MockServer server, HttpStateHandler httpStateHandler, @Nullable ProxyConfiguration proxyConfiguration) {
        super(false);
        this.server = server;
        this.httpStateHandler = httpStateHandler;
        this.mockServerLogger = httpStateHandler.getMockServerLogger();
        portBindingSerializer = new PortBindingSerializer(mockServerLogger);
        this.actionHandler = new ActionHandler(httpStateHandler, proxyConfiguration);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {

        ResponseWriter responseWriter = new NettyResponseWriter(ctx);
        try {

            if (!httpStateHandler.handle(request, responseWriter, false)) {

                if (request.matches("PUT", "/status")) {

                    responseWriter.writeResponse(request, OK, portBindingSerializer.serialize(portBinding(server.getPorts())), "application/json");

                } else if (request.matches("PUT", "/bind")) {

                    PortBinding requestedPortBindings = portBindingSerializer.deserialize(request.getBodyAsString());
                    try {
                        List<Integer> actualPortBindings = server.bindToPorts(requestedPortBindings.getPorts());
                        responseWriter.writeResponse(request, OK, portBindingSerializer.serialize(portBinding(actualPortBindings)), "application/json");
                    } catch (RuntimeException e) {
                        if (e.getCause() instanceof BindException) {
                            responseWriter.writeResponse(request, BAD_REQUEST, e.getMessage() + " port already in use", MediaType.create("text", "plain").toString());
                        } else {
                            throw e;
                        }
                    }

                } else if (request.matches("PUT", "/stop")) {

                    ctx.writeAndFlush(response().withStatusCode(OK.code()));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            server.stop();
                        }
                    }).start();

                } else if (request.getMethod().getValue().equals("CONNECT")) {

                    ctx.channel().attr(PROXYING).set(Boolean.TRUE);
                    // assume SSL for CONNECT request
                    enabledSslUpstreamAndDownstream(ctx.channel());
                    // add Subject Alternative Name for SSL certificate
                    KeyAndCertificateFactory.addSubjectAlternativeName(request.getPath().getValue());
                    ctx.pipeline().addLast(new HttpConnectHandler(mockServerLogger, request.getPath().getValue(), -1));
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(request);

                } else {

                    actionHandler.processAction(request, responseWriter, ctx, getLocalAddresses(ctx), isProxyingRequest(ctx), false);

                }
            }
        } catch (IllegalArgumentException iae) {
            mockServerLogger.error(request, "Exception processing " + request + "\n" + iae.getMessage());
            // send request without API CORS headers
            responseWriter.writeResponse(request, BAD_REQUEST, iae.getMessage(), MediaType.create("text", "plain").toString());
        } catch (Exception e) {
            mockServerLogger.error(request, e, "Exception processing " + request);
            responseWriter.writeResponse(request, response().withStatusCode(BAD_REQUEST.code()).withBody(e.getMessage()), true);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (shouldNotIgnoreException(cause)) {
            new MockServerLogger(this.getClass()).error("Exception caught by " + server.getClass() + " handler -> closing pipeline " + ctx.channel(), cause);
        }
        closeOnFlush(ctx.channel());
    }
}
