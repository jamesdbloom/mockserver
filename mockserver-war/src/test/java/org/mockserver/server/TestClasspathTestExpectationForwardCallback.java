package org.mockserver.server;

import org.mockserver.mock.action.ExpectationForwardCallback;
import org.mockserver.model.HttpRequest;

import java.util.ArrayList;
import java.util.List;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author jamesdbloom
 */
public class TestClasspathTestExpectationForwardCallback implements ExpectationForwardCallback {

    public static List<HttpRequest> httpRequests = new ArrayList<HttpRequest>();
    public static HttpRequest httpRequestToReturn = request();

    @Override
    public HttpRequest handle(HttpRequest httpRequest) {
        httpRequests.add(httpRequest);
        return httpRequestToReturn;
    }
}
