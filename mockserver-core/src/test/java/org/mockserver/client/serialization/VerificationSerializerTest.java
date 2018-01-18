package org.mockserver.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockserver.client.serialization.model.HttpRequestDTO;
import org.mockserver.client.serialization.model.VerificationDTO;
import org.mockserver.client.serialization.model.VerificationTimesDTO;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.Cookie;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;
import org.mockserver.verify.Verification;
import org.mockserver.validator.jsonschema.JsonSchemaVerificationValidator;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.Verification.verification;

public class VerificationSerializerTest {

    private final HttpRequest request = request().withMethod("GET")
            .withPath("somepath")
            .withBody(new StringBody("somebody"))
            .withHeaders(new Header("headerName", "headerValue"))
            .withCookies(new Cookie("cookieName", "cookieValue"));
    private final VerificationTimes times = VerificationTimes.atLeast(2);
    private final Verification fullVerification =
            verification()
                    .withRequest(request)
                    .withTimes(times);
    private final VerificationDTO fullVerificationDTO =
            new VerificationDTO()
                    .setHttpRequest(new HttpRequestDTO(request))
                    .setTimes(new VerificationTimesDTO(times));
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ObjectWriter objectWriter;
    @Mock
    private JsonSchemaVerificationValidator verificationValidator;
    @InjectMocks
    private VerificationSerializer verificationSerializer;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setupTestFixture() {
        verificationSerializer = spy(new VerificationSerializer(new MockServerLogger()));

        initMocks(this);
    }

    @Test
    public void deserialize() throws IOException {
        // given
        when(verificationValidator.isValid(eq("requestBytes"))).thenReturn("");
        when(objectMapper.readValue(eq("requestBytes"), same(VerificationDTO.class))).thenReturn(fullVerificationDTO);

        // when
        Verification verification = verificationSerializer.deserialize("requestBytes");

        // then
        assertEquals(fullVerification, verification);
    }

    @Test
    public void serialize() throws IOException {
        // given
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);

        // when
        verificationSerializer.serialize(fullVerification);

        // then
        verify(objectMapper).writerWithDefaultPrettyPrinter();
        verify(objectWriter).writeValueAsString(fullVerificationDTO);
    }

    @Test
    public void serializeHandlesException() throws IOException {
        // given
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Exception while serializing verification to JSON with value {" + NEW_LINE +
                "  \"httpRequest\" : { }," + NEW_LINE +
                "  \"times\" : {" + NEW_LINE +
                "    \"count\" : 1," + NEW_LINE +
                "    \"exact\" : false" + NEW_LINE +
                "  }" + NEW_LINE +
                "}");
        // and
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any(VerificationDTO.class))).thenThrow(new RuntimeException("TEST EXCEPTION"));

        // when
        verificationSerializer.serialize(new Verification());
    }

}
