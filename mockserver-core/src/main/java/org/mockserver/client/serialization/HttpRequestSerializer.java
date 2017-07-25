package org.mockserver.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import joptsimple.internal.Strings;
import org.mockserver.client.serialization.model.HttpRequestDTO;
import org.mockserver.model.HttpRequest;
import org.mockserver.validator.JsonSchemaHttpRequestValidator;
import org.mockserver.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockserver.character.Character.NEW_LINE;

/**
 * @author jamesdbloom
 */
public class HttpRequestSerializer implements Serializer<HttpRequest> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();
    private JsonArraySerializer jsonArraySerializer = new JsonArraySerializer();
    private JsonSchemaHttpRequestValidator httpRequestValidator = new JsonSchemaHttpRequestValidator();

    public String serialize(HttpRequest httpRequest) {
        try {
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new HttpRequestDTO(httpRequest));
        } catch (Exception e) {
            logger.error(String.format("Exception while serializing httpRequest to JSON with value %s", httpRequest), e);
            throw new RuntimeException(String.format("Exception while serializing httpRequest to JSON with value %s", httpRequest), e);
        }
    }

    public String serialize(List<HttpRequest> httpRequests) {
        return serialize(httpRequests.toArray(new HttpRequest[httpRequests.size()]));
    }

    public String serialize(HttpRequest... httpRequests) {
        try {
            if (httpRequests != null && httpRequests.length > 0) {
                HttpRequestDTO[] httpRequestDTOs = new HttpRequestDTO[httpRequests.length];
                for (int i = 0; i < httpRequests.length; i++) {
                    httpRequestDTOs[i] = new HttpRequestDTO(httpRequests[i]);
                }
                return objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(httpRequestDTOs);
            }
            return "";
        } catch (Exception e) {
            logger.error("Exception while serializing HttpRequest to JSON with value " + Arrays.asList(httpRequests), e);
            throw new RuntimeException("Exception while serializing HttpRequest to JSON with value " + Arrays.asList(httpRequests), e);
        }
    }

    public HttpRequest deserialize(String jsonHttpRequest) {
        if (Strings.isNullOrEmpty(jsonHttpRequest)) {
            throw new IllegalArgumentException("1 error:\n - a request is required but value was \"" + String.valueOf(jsonHttpRequest) + "\"");
        } else {
            String validationErrors = httpRequestValidator.isValid(jsonHttpRequest);
            if (validationErrors.isEmpty()) {
                HttpRequest httpRequest = null;
                try {
                    HttpRequestDTO httpRequestDTO = objectMapper.readValue(jsonHttpRequest, HttpRequestDTO.class);
                    if (httpRequestDTO != null) {
                        httpRequest = httpRequestDTO.buildObject();
                    }
                } catch (Exception e) {
                    logger.error("Exception while parsing [" + jsonHttpRequest + "] for HttpRequest", e);
                    throw new RuntimeException("Exception while parsing [" + jsonHttpRequest + "] for HttpRequest", e);
                }
                return httpRequest;
            } else {
                logger.info("Validation failed:" + NEW_LINE + jsonHttpRequest + NEW_LINE + "-- HttpRequest:" + NEW_LINE + jsonHttpRequest + NEW_LINE + "-- Schema:" + NEW_LINE + httpRequestValidator.getSchema());
                throw new IllegalArgumentException(validationErrors);
            }
        }
    }

    @Override
    public Class<HttpRequest> supportsType() {
        return HttpRequest.class;
    }

    public HttpRequest[] deserializeArray(String jsonHttpRequests) {
        List<HttpRequest> httpRequests = new ArrayList<HttpRequest>();
        if (Strings.isNullOrEmpty(jsonHttpRequests)) {
            throw new IllegalArgumentException("1 error:\n - a request or request array is required but value was \"" + String.valueOf(jsonHttpRequests) + "\"");
        } else {
            List<String> jsonRequestList = jsonArraySerializer.returnJSONObjects(jsonHttpRequests);
            if (jsonRequestList.isEmpty()) {
                throw new IllegalArgumentException("1 error:\n - a request or array of request is required");
            } else {
                List<String> validationErrorsList = new ArrayList<String>();
                for (String jsonExpecation : jsonRequestList) {
                    try {
                        httpRequests.add(deserialize(jsonExpecation));
                    } catch (IllegalArgumentException iae) {
                        validationErrorsList.add(iae.getMessage());
                    }

                }
                if (!validationErrorsList.isEmpty()) {
                    throw new IllegalArgumentException((validationErrorsList.size() > 1 ? "[" : "") + Joiner.on(",\n").join(validationErrorsList) + (validationErrorsList.size() > 1 ? "]" : ""));
                }
            }
        }
        return httpRequests.toArray(new HttpRequest[httpRequests.size()]);
    }

}
