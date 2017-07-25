package org.mockserver.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Joiner;
import joptsimple.internal.Strings;
import org.mockserver.client.serialization.ObjectMapperFactory;
import org.mockserver.file.FileReader;
import org.mockserver.model.ObjectWithReflectiveEqualsHashCodeToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jamesdbloom
 */
public class JsonSchemaValidator extends ObjectWithReflectiveEqualsHashCodeToString implements Validator<String> {

    private final String schema;
    public Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    public JsonSchemaValidator(String schema) {
        if (schema.trim().endsWith(".json")) {
            this.schema = FileReader.readFileFromClassPathOrPath(schema);
        } else if (schema.trim().endsWith("}")) {
            this.schema = schema;
        } else {
            throw new IllegalArgumentException("Schema must either be a path reference to a *.json file or a json string");
        }
    }

    @Override
    public String isValid(String json) {
        List<String> validationErrors = new ArrayList<String>();
        try {
            final ProcessingReport validate = JsonSchemaFactory
                    .byDefault()
                    .getValidator()
                    .validate(objectMapper.readTree(schema), objectMapper.readTree(json), true);

            System.out.println("json = " + json);
            System.out.println("validate = " + validate);
            if (validate.isSuccess()) {
                return "";
            } else {
                for (ProcessingMessage processingMessage : validate) {
                    System.out.println("processingMessage.getMessage() = " + processingMessage.getMessage());
                    if (String.valueOf(processingMessage.asJson().get("keyword")).equals("\"oneOf\"")) {
                        StringBuilder oneOfErrorMessage = new StringBuilder("oneOf of the following must be specified ");
                        for (JsonNode jsonNode : processingMessage.asJson().get("reports")) {
                            if (jsonNode.get(0) != null && jsonNode.get(0).get("required") != null && jsonNode.get(0).get("required").get(0) != null) {
                                oneOfErrorMessage.append(String.valueOf(jsonNode.get(0).get("required").get(0))).append(" ");
                            }
                        }
                        validationErrors.add(oneOfErrorMessage.toString());
                    } else {
                        String fieldPointer = "";
                        if (processingMessage.asJson().get("instance") != null && processingMessage.asJson().get("instance").get("pointer") != null) {
                            fieldPointer = String.valueOf(processingMessage.asJson().get("instance").get("pointer")).replaceAll("\"", "");
                        }
                        validationErrors.add(processingMessage.getMessage() + (fieldPointer.isEmpty() ? "" : " for field \"" + fieldPointer + "\""));
                    }
                }
                return validationErrors.size() + " error" + (validationErrors.size() > 1 ? "s" : "") + ":\n - " + Joiner.on("\n - ").join(validationErrors);
            }
        } catch (Exception e) {
            logger.info("Exception validating JSON", e);
            return e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
