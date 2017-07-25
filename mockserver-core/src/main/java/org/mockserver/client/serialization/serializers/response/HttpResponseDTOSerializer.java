package org.mockserver.client.serialization.serializers.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.mockserver.client.serialization.model.*;
import org.mockserver.model.JsonSchemaBody;

import java.io.IOException;

/**
 * @author jamesdbloom
 */
public class HttpResponseDTOSerializer extends StdSerializer<HttpResponseDTO> {

    public HttpResponseDTOSerializer() {
        super(HttpResponseDTO.class);
    }

    @Override
    public void serialize(HttpResponseDTO httpResponseDTO, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        if (httpResponseDTO.getStatusCode() != null) {
            jgen.writeObjectField("statusCode", httpResponseDTO.getStatusCode());
        }
        if (httpResponseDTO.getHeaders() != null && !httpResponseDTO.getHeaders().isEmpty()) {
            jgen.writeObjectField("headers", httpResponseDTO.getHeaders());
        }
        if (httpResponseDTO.getCookies() != null && !httpResponseDTO.getCookies().isEmpty()) {
            jgen.writeObjectField("cookies", httpResponseDTO.getCookies());
        }
        BodyWithContentTypeDTO body = httpResponseDTO.getBody();
        if (body != null) {
            if (body instanceof StringBodyDTO && !((StringBodyDTO) body).getString().isEmpty()) {
                jgen.writeObjectField("body", body);
            } else if (body instanceof JsonBodyDTO && !((JsonBodyDTO) body).getJson().isEmpty()) {
                jgen.writeObjectField("body", ((JsonBodyDTO) body).getJson());
            } else if (body instanceof XmlBodyDTO && !((XmlBodyDTO) body).getXml().isEmpty()) {
                jgen.writeObjectField("body", ((XmlBodyDTO) body).getXml());
            } else if (body instanceof ParameterBodyDTO && !((ParameterBodyDTO) body).getParameters().isEmpty()) {
                jgen.writeObjectField("body", ((ParameterBodyDTO) body).buildObject().toString());
            } else if (body instanceof BinaryBodyDTO) {
                jgen.writeObjectField("body", body);
            }
        }
        if (httpResponseDTO.getDelay() != null) {
            jgen.writeObjectField("delay", httpResponseDTO.getDelay());
        }
        if (httpResponseDTO.getConnectionOptions() != null) {
            jgen.writeObjectField("connectionOptions", httpResponseDTO.getConnectionOptions());
        }
        jgen.writeEndObject();
    }
}
