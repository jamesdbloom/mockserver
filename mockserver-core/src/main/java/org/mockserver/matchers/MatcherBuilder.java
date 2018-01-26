package org.mockserver.matchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;

/**
 * @author jamesdbloom
 */
public class MatcherBuilder {

    private final MockServerLogger mockServerLogger;

    public MatcherBuilder(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public HttpRequestMatcher transformsToMatcher(HttpRequest httpRequest) {
        return new HttpRequestMatcher(httpRequest, mockServerLogger);
    }

    public HttpRequestMatcher transformsToMatcher(Expectation expectation) {
        return new HttpRequestMatcher(expectation, mockServerLogger);
    }

}
