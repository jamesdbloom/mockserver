package org.mockserver.mock;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.mockserver.client.serialization.Base64Converter;
import org.mockserver.client.serialization.ExpectationSerializer;
import org.mockserver.matchers.HttpRequestMatcher;
import org.mockserver.matchers.MatcherBuilder;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jamesdbloom
 */
public class MockServerMatcher extends ObjectWithReflectiveEqualsHashCodeToString {

    protected final List<Expectation> expectations;
    private Logger requestLogger = LoggerFactory.getLogger("REQUEST");

    public MockServerMatcher() {
        this(new ArrayList<Expectation>());
    }

    public MockServerMatcher(List<Expectation> expectations) {
        this.expectations = Collections.synchronizedList(expectations);
    }

    public Expectation when(HttpRequest httpRequest) {
        return when(httpRequest, Times.unlimited(), TimeToLive.unlimited());
    }

    public Expectation when(final HttpRequest httpRequest, Times times, TimeToLive timeToLive) {
        Expectation expectation;
        if (times.isUnlimited()) {
            Collection<Expectation> existingExpectationsWithMatchingRequest = new ArrayList<Expectation>();
            for (Expectation potentialExpectation : new ArrayList<Expectation>(this.expectations)) {
                if (potentialExpectation.contains(httpRequest)) {
                    existingExpectationsWithMatchingRequest.add(potentialExpectation);
                }
            }
            if (!existingExpectationsWithMatchingRequest.isEmpty()) {
                for (Expectation existingExpectation : existingExpectationsWithMatchingRequest) {
                    existingExpectation.setNotUnlimitedResponses();
                }
                 expectation = new Expectation(httpRequest, times, timeToLive);
            } else {
                expectation = new Expectation(httpRequest, Times.unlimited(), timeToLive);
            }
        } else {
            expectation = new Expectation(httpRequest, times, timeToLive);
        }
        this.expectations.add(expectation);
        return expectation;
    }

    public Action handle(HttpRequest httpRequest) {
        for (Expectation expectation : new ArrayList<Expectation>(this.expectations)) {
            if (expectation.matches(httpRequest)) {
                expectation.decrementRemainingMatches();
                if (!expectation.hasRemainingMatches()) {
                    if (this.expectations.contains(expectation)) {
                        this.expectations.remove(expectation);
                    }
                }
                return expectation.getAction(true);
            } else if (!expectation.isStillAlive()) {
                if (this.expectations.contains(expectation)) {
                    this.expectations.remove(expectation);
                }
            }
        }
        return null;
    }

    public void clear(HttpRequest httpRequest) {
        if (httpRequest != null) {
            HttpRequestMatcher httpRequestMatcher = new MatcherBuilder().transformsToMatcher(httpRequest);
            for (Expectation expectation : new ArrayList<Expectation>(this.expectations)) {
                if (httpRequestMatcher.matches(expectation.getHttpRequest(), true)) {
                    if (this.expectations.contains(expectation)) {
                        this.expectations.remove(expectation);
                    }
                }
            }
        } else {
            reset();
        }
    }

    public void reset() {
        this.expectations.clear();
    }

    public void dumpToLog(HttpRequest httpRequest) {
        if (httpRequest != null) {
            ExpectationSerializer expectationSerializer = new ExpectationSerializer();
            for (Expectation expectation : new ArrayList<Expectation>(this.expectations)) {
                if (expectation.matches(httpRequest)) {
                    requestLogger.warn(cleanBase64Response(expectationSerializer.serialize(expectation)));
                }
            }
        } else {
            ExpectationSerializer expectationSerializer = new ExpectationSerializer();
            for (Expectation expectation : new ArrayList<Expectation>(this.expectations)) {
                requestLogger.warn(cleanBase64Response(expectationSerializer.serialize(expectation)));
            }
        }
    }

    @VisibleForTesting
    String cleanBase64Response(String serializedExpectation) {
        Pattern base64ResponseBodyPattern = Pattern.compile("[\\s\\S]*\\\"httpResponse\\\"\\s*\\:\\s*\\{[\\s\\S]*\\\"body\\\"\\s*\\:\\s*\\\"(([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==))\\\"[\\s\\S]*");
        Matcher matcher = base64ResponseBodyPattern.matcher(serializedExpectation);
        if (matcher.find()) {
            return serializedExpectation.replace(matcher.group(1), new String(Base64Converter.base64StringToBytes(matcher.group(1))));
        } else {
            return serializedExpectation;
        }
    }

    public List<Expectation> getExpectations() {
        return new ArrayList<Expectation>(this.expectations);
    }

}
