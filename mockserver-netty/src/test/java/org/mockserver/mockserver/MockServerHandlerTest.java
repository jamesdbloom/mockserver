package org.mockserver.mockserver;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.client.serialization.PortBindingSerializer;
import org.mockserver.lifecycle.LifeCycle;
import org.mockserver.log.model.RequestLogEntry;
import org.mockserver.log.model.RequestResponseLogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.mock.action.ActionHandler;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RetrieveType;
import org.mockserver.responsewriter.NettyResponseWriter;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.mock.action.ActionHandler.REMOTE_SOCKET;
import static org.mockserver.mockserver.MockServerHandler.LOCAL_HOST_HEADERS;
import static org.mockserver.mockserver.MockServerHandler.PROXYING;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.PortBinding.portBinding;

/**
 * @author jamesdbloom
 */
public class MockServerHandlerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private HttpStateHandler httpStateHandler;
    protected LifeCycle server;
    private ActionHandler mockActionHandler;
    private EmbeddedChannel embeddedChannel;
    @InjectMocks
    private MockServerHandler mockServerHandler;
    private HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer(new MockServerLogger());
    private ExpectationSerializer expectationSerializer = new ExpectationSerializer(new MockServerLogger());
    private PortBindingSerializer portBindingSerializer = new PortBindingSerializer(new MockServerLogger());

    @Before
    public void setupFixture() {
        server = mock(MockServer.class);
        mockActionHandler = mock(ActionHandler.class);

        httpStateHandler = new HttpStateHandler();
        mockServerHandler = new MockServerHandler((MockServer) server, httpStateHandler, null);

        initMocks(this);

        embeddedChannel = new EmbeddedChannel(mockServerHandler);
    }

    private void assertResponse(int responseStatusCode, String responseBody) {
        HttpResponse httpResponse = embeddedChannel.readOutbound();
        assertThat(httpResponse.getStatusCode(), is(responseStatusCode));
        assertThat(httpResponse.getBodyAsString(), is(responseBody));
    }

    @Test
    public void shouldRetrieveRequests() {
        // given
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        HttpRequest expectationRetrieveRequestsRequest = request("/retrieve")
            .withMethod("PUT")
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            );

        // when
        embeddedChannel.writeInbound(expectationRetrieveRequestsRequest);

        // then
        assertResponse(200, httpRequestSerializer.serialize(Collections.singletonList(
            request("request_one")
        )));
    }

    @Test
    public void shouldClear() {
        // given
        httpStateHandler.add(new Expectation(request("request_one")).thenRespond(response("response_one")));
        httpStateHandler.log(new RequestLogEntry(request("request_one")));
        HttpRequest clearRequest = request("/clear")
            .withMethod("PUT")
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            );

        // when
        embeddedChannel.writeInbound(clearRequest);

        // then
        assertResponse(200, "");
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), is(nullValue()));
        assertThat(httpStateHandler.retrieve(request("/retrieve")
            .withMethod("PUT")
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            )), is(response().withBody("[]", JSON_UTF_8).withStatusCode(200)));
    }

    @Test
    public void shouldReturnStatus() {
        // given
        when(server.getLocalPorts()).thenReturn(Arrays.asList(1080, 1090));
        HttpRequest statusRequest = request("/status").withMethod("PUT");

        // when
        embeddedChannel.writeInbound(statusRequest);

        // then
        assertResponse(200, portBindingSerializer.serialize(
            portBinding(1080, 1090)
        ));
    }

    @Test
    public void shouldBindNewPorts() {
        // given
        when(server.bindToPorts(anyListOf(Integer.class))).thenReturn(Arrays.asList(1080, 1090));
        HttpRequest statusRequest = request("/bind")
            .withMethod("PUT")
            .withBody(portBindingSerializer.serialize(
                portBinding(1080, 1090)
            ));

        // when
        embeddedChannel.writeInbound(statusRequest);

        // then
        verify(server).bindToPorts(Arrays.asList(1080, 1090));
        assertResponse(200, portBindingSerializer.serialize(
            portBinding(1080, 1090)
        ));
    }

    @Test
    public void shouldStop() throws InterruptedException {
        // given
        HttpRequest statusRequest = request("/stop")
            .withMethod("PUT");

        // when
        embeddedChannel.writeInbound(statusRequest);

        // then
        assertResponse(200, null);
        TimeUnit.SECONDS.sleep(1); // ensure stop thread has run
        verify(server).stop();
    }

    @Test
    public void shouldRetrieveRecordedExpectations() {
        // given
        httpStateHandler.log(new RequestResponseLogEntry(
            request("request_one"),
            response("response_one")
        ));
        HttpRequest expectationRetrieveExpectationsRequest = request("/retrieve")
            .withMethod("PUT")
            .withQueryStringParameter("type", RetrieveType.RECORDED_EXPECTATIONS.name())
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            );

        // when
        embeddedChannel.writeInbound(expectationRetrieveExpectationsRequest);

        // then
        assertResponse(200, expectationSerializer.serialize(Collections.singletonList(
            new Expectation(request("request_one"), Times.once(), TimeToLive.unlimited()).thenRespond(response("response_one"))
        )));
    }

    @Test
    public void shouldRetrieveLogMessages() {
        // given
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        HttpRequest retrieveLogRequest = request("/retrieve")
            .withMethod("PUT")
            .withQueryStringParameter("type", RetrieveType.LOGS.name())
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            );

        // when
        embeddedChannel.writeInbound(retrieveLogRequest);

        // then
        HttpResponse response = embeddedChannel.readOutbound();
        assertThat(response.getStatusCode(), is(200));
        String[] splitBody = response.getBodyAsString().split("------------------------------------\n");
        assertThat(splitBody.length, is(2));
        assertThat(
            splitBody[0],
            is(endsWith("creating expectation:" + NEW_LINE +
                "" + NEW_LINE +
                "\t{" + NEW_LINE +
                "\t  \"httpRequest\" : {" + NEW_LINE +
                "\t    \"path\" : \"request_one\"" + NEW_LINE +
                "\t  }," + NEW_LINE +
                "\t  \"times\" : {" + NEW_LINE +
                "\t    \"unlimited\" : true" + NEW_LINE +
                "\t  }," + NEW_LINE +
                "\t  \"timeToLive\" : {" + NEW_LINE +
                "\t    \"unlimited\" : true" + NEW_LINE +
                "\t  }," + NEW_LINE +
                "\t  \"httpResponse\" : {" + NEW_LINE +
                "\t    \"statusCode\" : 200," + NEW_LINE +
                "\t    \"reasonPhrase\" : \"OK\"," + NEW_LINE +
                "\t    \"body\" : \"response_one\"" + NEW_LINE +
                "\t  }" + NEW_LINE +
                "\t}" + NEW_LINE))
        );
        assertThat(
            splitBody[1],
            is(endsWith("retrieving logs that match:" + NEW_LINE +
                NEW_LINE +
                "\t{" + NEW_LINE +
                "\t  \"path\" : \"request_one\"" + NEW_LINE +
                "\t}" + NEW_LINE +
                NEW_LINE)));
    }

    @Test
    public void shouldAddExpectation() {
        // given
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        HttpRequest request = request("/expectation").withMethod("PUT").withBody(
            expectationSerializer.serialize(expectationOne)
        );

        // when
        embeddedChannel.writeInbound(request);

        // then
        assertResponse(201, "");
        assertThat(httpStateHandler.firstMatchingExpectation(request("request_one")), is(expectationOne));
    }

    @Test
    public void shouldRetrieveActiveExpectations() {
        // given
        Expectation expectationOne = new Expectation(request("request_one")).thenRespond(response("response_one"));
        httpStateHandler.add(expectationOne);
        HttpRequest expectationRetrieveExpectationsRequest = request("/retrieve")
            .withMethod("PUT")
            .withQueryStringParameter("type", RetrieveType.ACTIVE_EXPECTATIONS.name())
            .withBody(
                httpRequestSerializer.serialize(request("request_one"))
            );

        // when
        embeddedChannel.writeInbound(expectationRetrieveExpectationsRequest);

        // then
        assertResponse(200, expectationSerializer.serialize(Collections.singletonList(
            expectationOne
        )));
    }

    @Test
    public void shouldProxyRequestsWhenProxying() {
        // given
        HttpRequest request = request("request_one");
        InetSocketAddress remoteAddress = new InetSocketAddress(1080);
        embeddedChannel.attr(LOCAL_HOST_HEADERS).set(ImmutableSet.of(
            "local_address:666",
            "localhost:666",
            "127.0.0.1:666"
        ));
        embeddedChannel.attr(PROXYING).set(true);
        embeddedChannel.attr(REMOTE_SOCKET).set(remoteAddress);

        // when
        embeddedChannel.writeInbound(request);

        // then
        verify(mockActionHandler).processAction(
            eq(request),
            any(NettyResponseWriter.class),
            any(ChannelHandlerContext.class),
            eq(ImmutableSet.of(
                "local_address:666",
                "localhost:666",
                "127.0.0.1:666"
            )),
            eq(true),
            eq(false));
    }

    @Test
    public void shouldProxyRequestsWhenNotProxying() {
        // given
        HttpRequest request = request("request_one");
        InetSocketAddress remoteAddress = new InetSocketAddress(1080);
        embeddedChannel.attr(LOCAL_HOST_HEADERS).set(ImmutableSet.of(
            "local_address:666",
            "localhost:666",
            "127.0.0.1:666"
        ));
        embeddedChannel.attr(PROXYING).set(false);
        embeddedChannel.attr(REMOTE_SOCKET).set(remoteAddress);

        // when
        embeddedChannel.writeInbound(request);

        // then
        verify(mockActionHandler).processAction(
            eq(request),
            any(NettyResponseWriter.class),
            any(ChannelHandlerContext.class),
            eq(ImmutableSet.of(
                "local_address:666",
                "localhost:666",
                "127.0.0.1:666"
            )),
            eq(false),
            eq(false));
    }
}
