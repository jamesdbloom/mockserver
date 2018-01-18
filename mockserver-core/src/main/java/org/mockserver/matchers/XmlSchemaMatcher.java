package org.mockserver.matchers;

import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.HttpRequest;
import org.mockserver.validator.xmlschema.XmlSchemaValidator;

import static org.mockserver.character.Character.NEW_LINE;

/**
 * See http://xml-schema.org/
 *
 * @author jamesdbloom
 */
public class XmlSchemaMatcher extends BodyMatcher<String> {
    private final MockServerLogger mockServerLogger;
    private String schema;
    private XmlSchemaValidator xmlSchemaValidator;

    public XmlSchemaMatcher(MockServerLogger mockServerLogger, String schema) {
        this.mockServerLogger = mockServerLogger;
        this.schema = schema;
        xmlSchemaValidator = new XmlSchemaValidator(mockServerLogger, schema);
    }

    protected String[] fieldsExcludedFromEqualsAndHashCode() {
        return new String[]{"logger", "xmlSchemaValidator"};
    }

    public boolean matches(HttpRequest context, String matched) {
        boolean result = false;

        try {
            String validation = xmlSchemaValidator.isValid(matched);

            result = validation.isEmpty();

            if (!result) {
                mockServerLogger.trace(context, "Failed to match XML: {}" + NEW_LINE + "with schema: {}" + NEW_LINE + "because: {}", matched, this.schema, validation);
            }
        } catch (Exception e) {
            mockServerLogger.trace(context, "Failed to match XML: {}" + NEW_LINE + "with schema: {}" + NEW_LINE + "because: {}", matched, this.schema, e.getMessage());
        }

        return reverseResultIfNot(result);
    }

}
