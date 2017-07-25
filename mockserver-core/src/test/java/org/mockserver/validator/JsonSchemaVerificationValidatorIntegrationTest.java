package org.mockserver.validator;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.character.Character.NEW_LINE;

/**
 * @author jamesdbloom
 */
public class JsonSchemaVerificationValidatorIntegrationTest {

    private JsonSchemaValidator jsonSchemaValidator = new JsonSchemaHttpRequestValidator();

    @Test
    public void shouldValidateValidCompleteRequestWithStringBody() {
        // when
        assertThat(jsonSchemaValidator.isValid("{" + NEW_LINE +
                "    \"method\" : \"someMethod\"," + NEW_LINE +
                "    \"path\" : \"somePath\"," + NEW_LINE +
                "    \"queryStringParameters\" : [ {" + NEW_LINE +
                "      \"name\" : \"queryStringParameterNameOne\"," + NEW_LINE +
                "      \"values\" : [ \"queryStringParameterValueOne_One\", \"queryStringParameterValueOne_Two\" ]" + NEW_LINE +
                "    }, {" + NEW_LINE +
                "      \"name\" : \"queryStringParameterNameTwo\"," + NEW_LINE +
                "      \"values\" : [ \"queryStringParameterValueTwo_One\" ]" + NEW_LINE +
                "    } ]," + NEW_LINE +
                "    \"body\" : {" + NEW_LINE +
                "      \"type\" : \"STRING\"," + NEW_LINE +
                "      \"string\" : \"someBody\"" + NEW_LINE +
                "    }," + NEW_LINE +
                "    \"cookies\" : [ {" + NEW_LINE +
                "      \"name\" : \"someCookieName\"," + NEW_LINE +
                "      \"value\" : \"someCookieValue\"" + NEW_LINE +
                "    } ]," + NEW_LINE +
                "    \"headers\" : [ {" + NEW_LINE +
                "      \"name\" : \"someHeaderName\"," + NEW_LINE +
                "      \"values\" : [ \"someHeaderValue\" ]" + NEW_LINE +
                "    } ]" + NEW_LINE +
                "  }"), is(""));
    }

    @Test
    public void shouldValidateInvalidBodyFields() {
        // when
        assertThat(jsonSchemaValidator.isValid("{" + NEW_LINE +
                        "    \"body\" : {" + NEW_LINE +
                        "      \"type\" : \"STRING\"," + NEW_LINE +
                        "      \"value\" : \"someBody\"" + NEW_LINE +
                        "    }" + NEW_LINE +
                        "  }"),
                is(
                        "1 error:" + NEW_LINE +
                                " - for field \"/body\" a plain string or one of the following example bodies must be specified " + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"BINARY\"," + NEW_LINE +
                                "     \"base64Bytes\": \"\"," + NEW_LINE +
                                "     \"contentType\": \"\"" + NEW_LINE +
                                "   }, " + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"JSON\"," + NEW_LINE +
                                "     \"json\": \"\"," + NEW_LINE +
                                "     \"contentType\": \"\"," + NEW_LINE +
                                "     \"matchType\": \"ONLY_MATCHING_FIELDS\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"JSON_SCHEMA\"," + NEW_LINE +
                                "     \"jsonSchema\": \"\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"PARAMETERS\"," + NEW_LINE +
                                "     \"parameters\": \"TO DO\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"REGEX\"," + NEW_LINE +
                                "     \"regex\": \"\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"STRING\"," + NEW_LINE +
                                "     \"string\": \"\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"XML\"," + NEW_LINE +
                                "     \"xml\": \"\"," + NEW_LINE +
                                "     \"contentType\": \"\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"XML_SCHEMA\"," + NEW_LINE +
                                "     \"xmlSchema\": \"\"" + NEW_LINE +
                                "   }," + NEW_LINE +
                                "   {" + NEW_LINE +
                                "     \"not\": false," + NEW_LINE +
                                "     \"type\": \"XPATH\"," + NEW_LINE +
                                "     \"xpath\": \"\"" + NEW_LINE +
                                "   }"
                ));
    }

    @Test
    public void shouldValidateInvalidExtraField() {
        // when
        assertThat(jsonSchemaValidator.isValid("{" + NEW_LINE +
                        "    \"invalidField\" : {" + NEW_LINE +
                        "      \"type\" : \"STRING\"," + NEW_LINE +
                        "      \"value\" : \"someBody\"" + NEW_LINE +
                        "    }" + NEW_LINE +
                        "  }"),
                is(
                        "1 error:" + NEW_LINE +
                                " - object instance has properties which are not allowed by the schema: [\"invalidField\"]"
                ));
    }

    @Test
    public void shouldValidateMultipleInvalidFieldTypes() {
        // when
        assertThat(jsonSchemaValidator.isValid("{" + NEW_LINE +
                        "    \"method\" : 100," + NEW_LINE +
                        "    \"path\" : false" + NEW_LINE +
                        "  }"),
                is(
                        "2 errors:" + NEW_LINE +
                                " - instance type (integer) does not match any allowed primitive type (allowed: [\"string\"]) for field \"/method\"" + NEW_LINE +
                                " - instance type (boolean) does not match any allowed primitive type (allowed: [\"string\"]) for field \"/path\""
                ));
    }

    @Test
    public void shouldValidateInvalidListItemType() {
        // when
        assertThat(jsonSchemaValidator.isValid("{" + NEW_LINE +
                        "    \"headers\" : [ \"invalidValueOne\", \"invalidValueTwo\" ]" + NEW_LINE +
                        "  }"),
                is(
                        "2 errors:" + NEW_LINE +
                                " - instance type (string) does not match any allowed primitive type (allowed: [\"object\"]) for field \"/headers/0\"" + NEW_LINE +
                                " - instance type (string) does not match any allowed primitive type (allowed: [\"object\"]) for field \"/headers/1\""
                ));
    }

}