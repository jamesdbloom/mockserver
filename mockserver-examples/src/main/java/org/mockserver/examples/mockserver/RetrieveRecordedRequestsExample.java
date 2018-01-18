package org.mockserver.examples.mockserver;

import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;

import static org.mockserver.model.HttpRequest.request;

/**
 * @author jamesdbloom
 */
public class RetrieveRecordedRequestsExample {

    public void retrieveAllRecordedRequests() {
        HttpRequest[] recordedRequests = new MockServerClient("localhost", 1080)
            .retrieveRecordedRequests(
                request()
            );
    }

    public void retrieveRecordedRequestsUsingRequestMatcher() {
        HttpRequest[] recordedRequests = new MockServerClient("localhost", 1080)
            .retrieveRecordedRequests(
                request()
                    .withPath("/some/path")
                    .withMethod("POST")
            );
    }

    public void retrieveRecordedRequestsAsJava() {
        String recordedRequests = new MockServerClient("localhost", 1080)
            .retrieveRecordedRequests(
                request()
                    .withPath("/some/path"),
                Format.JAVA
            );
    }

    public void retrieveRecordedRequestsInJson() {
        String recordedRequests = new MockServerClient("localhost", 1080)
            .retrieveRecordedRequests(
                request()
                    .withPath("/some/path"),
                Format.JSON
            );
    }
}
