package org.mockserver.matchers;

import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.HttpRequest;
import org.mockserver.validator.jsonschema.JsonSchemaValidator;

import static org.mockserver.character.Character.NEW_LINE;

/**
 * See http://json-schema.org/
 *
 * @author jamesdbloom
 */
public class JsonSchemaMatcher extends BodyMatcher<String> {
    private final MockServerLogger mockServerLogger;
    private String schema;
    private JsonSchemaValidator jsonSchemaValidator;

    public JsonSchemaMatcher(MockServerLogger mockServerLogger, String schema) {
        this.mockServerLogger = mockServerLogger;
        this.schema = schema;
        jsonSchemaValidator = new JsonSchemaValidator(mockServerLogger, schema);
    }

    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return new String[]{"logger", "jsonSchemaValidator"};
    }

    public boolean matches(HttpRequest context, String matched) {
        boolean result = false;

        try {
            String validation = jsonSchemaValidator.isValid(matched);

            result = validation.isEmpty();

            if (!result) {
                mockServerLogger.trace(context, "Failed to match JSON: {}" + NEW_LINE + "with schema: {}" + NEW_LINE + "because: {}", matched, this.schema, validation);
            }
        } catch (Exception e) {
            mockServerLogger.trace(context, "Failed to match JSON: {}" + NEW_LINE + "with schema: {}" + NEW_LINE + "because: {}", matched, this.schema, e.getMessage());
        }

        return reverseResultIfNot(result);
    }

}
