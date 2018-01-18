package org.mockserver.log.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

/**
 * @author jamesdbloom
 */
public class RequestResponseLogEntry extends LogEntry implements ExpectationLogEntry {

    private final HttpResponse httpResponse;

    public RequestResponseLogEntry(HttpRequest httpRequest, HttpResponse httpResponse) {
        super(httpRequest);
        this.httpResponse = httpResponse;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    @JsonIgnore
    public Expectation getExpectation() {
        return new Expectation(getHttpRequest(), Times.once(), TimeToLive.unlimited()).thenRespond(httpResponse);
    }

}
