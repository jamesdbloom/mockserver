package org.mockserver.mock.action;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockserver.callback.WebSocketClientRegistry;
import org.mockserver.callback.WebSocketRequestCallback;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.model.HttpObjectCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.responsewriter.ResponseWriter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;

/**
 * @author jamesdbloom
 */
public class HttpForwardObjectCallbackActionHandlerTest {

    @Test
    public void shouldHandleHttpRequests() {
        // given
        WebSocketClientRegistry mockWebSocketClientRegistry = mock(WebSocketClientRegistry.class);
        HttpStateHandler mockHttpStateHandler = mock(HttpStateHandler.class);
        HttpObjectCallback httpObjectCallback = new HttpObjectCallback().withClientId("some_clientId");
        HttpRequest request = request().withBody("some_body");
        ResponseWriter mockResponseWriter = mock(ResponseWriter.class);
        when(mockHttpStateHandler.getWebSocketClientRegistry()).thenReturn(mockWebSocketClientRegistry);
        when(mockHttpStateHandler.getMockServerLogger()).thenReturn(new MockServerLogger());

        // when
        new HttpForwardObjectCallbackActionHandler(mockHttpStateHandler, null).handle(mock(ActionHandler.class), httpObjectCallback, request, mockResponseWriter, true);

        // then
        verify(mockWebSocketClientRegistry).registerForwardCallbackHandler(any(String.class), any(WebSocketRequestCallback.class));
        verify(mockWebSocketClientRegistry).sendClientMessage(eq("some_clientId"), any(HttpRequest.class));
    }

    @Test
    public void shouldReturnNotFound() throws ExecutionException, InterruptedException {
        // given
        ActionHandler mockActionHandler = mock(ActionHandler.class);
        HttpStateHandler mockHttpStateHandler = mock(HttpStateHandler.class);
        WebSocketClientRegistry mockWebSocketClientRegistry = mock(WebSocketClientRegistry.class);
        HttpObjectCallback httpObjectCallback = new HttpObjectCallback().withClientId("some_clientId");
        HttpRequest request = request().withBody("some_body");
        ResponseWriter mockResponseWriter = mock(ResponseWriter.class);
        when(mockHttpStateHandler.getWebSocketClientRegistry()).thenReturn(mockWebSocketClientRegistry);
        when(mockHttpStateHandler.getMockServerLogger()).thenReturn(new MockServerLogger());
        when(mockWebSocketClientRegistry.sendClientMessage(eq("some_clientId"), any(HttpRequest.class))).thenReturn(false);

        // when
        new HttpForwardObjectCallbackActionHandler(mockHttpStateHandler, null).handle(mockActionHandler, httpObjectCallback, request, mockResponseWriter, true);

        // then
        verify(mockWebSocketClientRegistry).registerForwardCallbackHandler(any(String.class), any(WebSocketRequestCallback.class));
        verify(mockWebSocketClientRegistry).sendClientMessage(eq("some_clientId"), any(HttpRequest.class));
        ArgumentCaptor<HttpForwardActionResult> httpForwardActionResultArgumentCaptor = ArgumentCaptor.forClass(HttpForwardActionResult.class);
        verify(mockActionHandler).writeForwardActionResponse(httpForwardActionResultArgumentCaptor.capture(), same(mockResponseWriter), same(request), same(httpObjectCallback), eq(true));
        assertThat(httpForwardActionResultArgumentCaptor.getValue().getHttpResponse().get(), is(notFoundResponse()));
    }

    private HttpForwardActionResult notFoundFuture(HttpRequest httpRequest) {
        CompletableFuture<HttpResponse> notFoundFuture = new CompletableFuture<>();
        notFoundFuture.complete(notFoundResponse());
        return new HttpForwardActionResult(httpRequest, notFoundFuture, null);
    }
}
