package org.mockserver.client.proxy;

import com.google.common.base.Charsets;
import org.apache.velocity.runtime.directive.contrib.For;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.netty.NettyHttpClient;
import org.mockserver.client.netty.SocketConnectionException;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.client.serialization.HttpRequestSerializer;
import org.mockserver.client.serialization.VerificationSequenceSerializer;
import org.mockserver.client.serialization.VerificationSerializer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.*;
import org.mockserver.verify.Verification;
import org.mockserver.verify.VerificationSequence;
import org.mockserver.verify.VerificationTimes;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.Verification.verification;
import static org.mockserver.verify.VerificationTimes.atLeast;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author jamesdbloom
 */
public class ProxyClientTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private NettyHttpClient mockHttpClient;
    @Mock
    private ExpectationSerializer mockExpectationSerializer;
    @Mock
    private HttpRequestSerializer mockHttpRequestSerializer;
    @Mock
    private VerificationSerializer mockVerificationSerializer;
    @Mock
    private VerificationSequenceSerializer mockVerificationSequenceSerializer;
    @InjectMocks
    private ProxyClient proxyClient;

    @Before
    public void setupTestFixture() {
        proxyClient = spy(new ProxyClient("localhost", 1090));

        initMocks(this);
    }

    @Test
    public void shouldHandleNullHostnameExceptions() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("Host can not be null or empty"));

        // when
        new ProxyClient(null, 1090);
    }

    @Test
    public void shouldHandleNullContextPathExceptions() {
        // given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("ContextPath can not be null"));

        // when
        new ProxyClient("localhost", 1090, null);
    }

    @Test
    public void shouldSendStopRequest() {
        // when
        proxyClient.stop();

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/stop"),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldBeCloseable() throws Exception {
        // when
        proxyClient.close();

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/stop"),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldQueryRunningStatus() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withStatusCode(HttpStatusCode.OK_200.code()));

        // when
        boolean running = proxyClient.isRunning();

        // then
        assertTrue(running);
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/status"),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldQueryRunningStatusWhenSocketConnectionException() throws Exception {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenThrow(SocketConnectionException.class);

        // when
        boolean running = proxyClient.isRunning();

        // then
        assertFalse(running);
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/status"),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldSendResetRequest() {
        // when
        proxyClient.reset();

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/reset"),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldSendClearRequest() {
        // given
        HttpRequest someRequestMatcher = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));
        when(mockHttpRequestSerializer.serialize(someRequestMatcher)).thenReturn(someRequestMatcher.toString());

        // when
        proxyClient.clear(someRequestMatcher);

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/clear")
                        .withBody(someRequestMatcher.toString(), Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldSendClearRequestWithType() {
        // given
        HttpRequest someRequestMatcher = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));
        when(mockHttpRequestSerializer.serialize(someRequestMatcher)).thenReturn(someRequestMatcher.toString());

        // when
        proxyClient.clear(someRequestMatcher, ClearType.LOG);

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/clear")
                        .withQueryStringParameter("type", "log")
                        .withBody(someRequestMatcher.toString(), Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldSendClearRequestForNullRequest() throws Exception {
        // when
        proxyClient
                .clear(null);

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/clear")
                        .withBody("", Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldRetrieveRequests() {
        // given - a request
        HttpRequest someRequestMatcher = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));
        when(mockHttpRequestSerializer.serialize(someRequestMatcher)).thenReturn(someRequestMatcher.toString());

        // and - a client
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("body"));

        // and - a response
        HttpRequest[] httpRequests = {};
        when(mockHttpRequestSerializer.deserializeArray("body")).thenReturn(httpRequests);

        // when
        assertSame(httpRequests, proxyClient.retrieveRecordedRequests(someRequestMatcher));

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/retrieve")
                        .withQueryStringParameter("type", RetrieveType.REQUESTS.name())
                        .withQueryStringParameter("format", Format.JSON.name())
                        .withBody(someRequestMatcher.toString(), Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS);
        verify(mockHttpRequestSerializer).deserializeArray("body");
    }

    @Test
    public void shouldRetrieveRequestsWithNullRequest() {
        // given
        HttpRequest[] httpRequests = {};
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("body"));
        when(mockHttpRequestSerializer.deserializeArray("body")).thenReturn(httpRequests);

        // when
        assertSame(httpRequests, proxyClient.retrieveRecordedRequests(null));

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/retrieve")
                        .withQueryStringParameter("type", RetrieveType.REQUESTS.name())
                        .withQueryStringParameter("format", Format.JSON.name())
                        .withBody("", Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
        verify(mockHttpRequestSerializer).deserializeArray("body");
    }

    @Test
    public void shouldRetrieveRecordedExpectations() throws UnsupportedEncodingException {
        // given - a request
        HttpRequest someRequestMatcher = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));
        when(mockHttpRequestSerializer.serialize(someRequestMatcher)).thenReturn(someRequestMatcher.toString());

        // and - a client
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("body"));

        // and - an expectation
        Expectation[] expectations = {};
        when(mockExpectationSerializer.deserializeArray("body")).thenReturn(expectations);

        // when
        assertSame(expectations, proxyClient.retrieveRecordedExpectations(someRequestMatcher));

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/retrieve")
                        .withQueryStringParameter("type", RetrieveType.RECORDED_EXPECTATIONS.name())
                        .withQueryStringParameter("format", Format.JSON.name())
                        .withBody(someRequestMatcher.toString(), Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
        verify(mockExpectationSerializer).deserializeArray("body");
    }

    @Test
    public void shouldRetrieveExpectationsWithNullRequest() {
        // given
        Expectation[] expectations = {};
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("body"));
        when(mockExpectationSerializer.deserializeArray("body")).thenReturn(expectations);

        // when
        assertSame(expectations, proxyClient.retrieveRecordedExpectations(null));

        // then
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/retrieve")
                        .withQueryStringParameter("type", RetrieveType.RECORDED_EXPECTATIONS.name())
                        .withQueryStringParameter("format", Format.JSON.name())
                        .withBody("", Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
        verify(mockExpectationSerializer).deserializeArray("body");
    }

    @Test
    public void shouldVerifyDoesNotMatchSingleRequestNoVerificationTimes() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("Request not found at least once expected:<foo> but was:<bar>"));
        when(mockVerificationSequenceSerializer.serialize(any(VerificationSequence.class))).thenReturn("verification_json");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));

        try {
            proxyClient.verify(httpRequest);

            // then
            fail();
        } catch (AssertionError ae) {
            verify(mockVerificationSequenceSerializer).serialize(new VerificationSequence().withRequests(httpRequest));
            verify(mockHttpClient).sendRequest(
                    request()
                            .withHeader(HOST.toString(), "localhost:" + 1090)
                            .withMethod("PUT")
                            .withPath("/verifySequence")
                            .withBody("verification_json", Charsets.UTF_8),
                20000,
                TimeUnit.MILLISECONDS
            );
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesNotMatchMultipleRequestsNoVerificationTimes() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("Request not found at least once expected:<foo> but was:<bar>"));
        when(mockVerificationSequenceSerializer.serialize(any(VerificationSequence.class))).thenReturn("verification_json");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));

        try {
            proxyClient.verify(httpRequest, httpRequest);

            // then
            fail();
        } catch (AssertionError ae) {
            verify(mockVerificationSequenceSerializer).serialize(new VerificationSequence().withRequests(httpRequest, httpRequest));
            verify(mockHttpClient).sendRequest(
                    request()
                            .withHeader(HOST.toString(), "localhost:" + 1090)
                            .withMethod("PUT")
                            .withPath("/verifySequence")
                            .withBody("verification_json", Charsets.UTF_8),
                20000,
                TimeUnit.MILLISECONDS
            );
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldVerifyDoesMatchSingleRequestNoVerificationTimes() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody(""));
        when(mockVerificationSequenceSerializer.serialize(any(VerificationSequence.class))).thenReturn("verification_json");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));

        try {
            proxyClient.verify(httpRequest);

            // then
        } catch (AssertionError ae) {
            fail();
        }

        // then
        verify(mockVerificationSequenceSerializer).serialize(new VerificationSequence().withRequests(httpRequest));
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/verifySequence")
                        .withBody("verification_json", Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldVerifyDoesMatchSingleRequestOnce() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody(""));
        when(mockVerificationSerializer.serialize(any(Verification.class))).thenReturn("verification_json");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));

        try {
            proxyClient.verify(httpRequest, once());

            // then
        } catch (AssertionError ae) {
            fail();
        }

        // then
        verify(mockVerificationSerializer).serialize(verification().withRequest(httpRequest).withTimes(once()));
        verify(mockHttpClient).sendRequest(
                request()
                        .withHeader(HOST.toString(), "localhost:" + 1090)
                        .withMethod("PUT")
                        .withPath("/verify")
                        .withBody("verification_json", Charsets.UTF_8),
            20000,
            TimeUnit.MILLISECONDS
        );
    }

    @Test
    public void shouldVerifyDoesNotMatchSingleRequest() {
        // given
        when(mockHttpClient.sendRequest(any(HttpRequest.class), anyLong(), any(TimeUnit.class))).thenReturn(response().withBody("Request not found at least once expected:<foo> but was:<bar>"));
        when(mockVerificationSerializer.serialize(any(Verification.class))).thenReturn("verification_json");
        HttpRequest httpRequest = new HttpRequest()
                .withPath("/some_path")
                .withBody(new StringBody("some_request_body"));

        try {
            proxyClient.verify(httpRequest, atLeast(1));

            // then
            fail();
        } catch (AssertionError ae) {
            verify(mockVerificationSerializer).serialize(verification().withRequest(httpRequest).withTimes(atLeast(1)));
            verify(mockHttpClient).sendRequest(
                    request()
                            .withHeader(HOST.toString(), "localhost:" + 1090)
                            .withMethod("PUT")
                            .withPath("/verify")
                            .withBody("verification_json", Charsets.UTF_8),
                20000,
                TimeUnit.MILLISECONDS
            );
            assertThat(ae.getMessage(), is("Request not found at least once expected:<foo> but was:<bar>"));
        }
    }

    @Test
    public void shouldHandleNullHttpRequest() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest, VerificationTimes) requires a non null HttpRequest object"));

        // when
        proxyClient.verify(null, VerificationTimes.exactly(2));
    }

    @Test
    public void shouldHandleNullVerificationTimes() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest, VerificationTimes) requires a non null VerificationTimes object"));

        // when
        proxyClient.verify(request(), null);
    }

    @Test
    public void shouldHandleNullHttpRequestSequence() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest...) requires a non null non empty array of HttpRequest objects"));

        // when
        proxyClient.verify((HttpRequest) null);
    }

    @Test
    public void shouldHandleEmptyHttpRequestSequence() {
        // then
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(containsString("verify(HttpRequest...) requires a non null non empty array of HttpRequest objects"));

        // when
        proxyClient.verify();
    }
}
