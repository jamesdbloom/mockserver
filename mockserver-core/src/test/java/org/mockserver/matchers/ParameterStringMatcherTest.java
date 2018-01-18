package org.mockserver.matchers;

import org.junit.Test;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.Parameter;
import org.mockserver.model.Parameters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.NottableString.not;

/**
 * @author jamesdbloom
 */
public class ParameterStringMatcherTest {

    @Test
    public void shouldMatchMatchingString() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameter.*", "parameter.*")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldNotMatchMatchingStringWhenNotApplied() {
        // given
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // then - not matcher
        assertFalse(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // and - not parameter
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter(not("parameterTwoName"), not("parameterTwoValue"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // and - not parameter
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter(not("parameterTwoName"), not("parameterTwoValue"))
        ))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldMatchMatchingStringWithNotParameterAndNormalParameter() {
        // not matching parameter
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter(not("parameterTwoName"), not("parameterTwoValue"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // not extra parameter
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter(not("parameterThree"), not("parameterThreeValueOne"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // not only parameter
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterThree"), not("parameterThreeValueOne"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // not all parameters (but matching)
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameter.*"), not(".*"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        // not all parameters (but not matching name)
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameter.*"), not("parameter.*"))
        )).matches(null, "" +
            "notParameterOneName=parameterOneValueOne" +
            "&notParameterOneName=parameterOneValueTwo" +
            "&notParameterTwoName=parameterTwoValue"));

        // not all parameters (but not matching value)
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameter.*"), not("parameter.*"))
        )).matches(null, "" +
            "parameterOneName=notParameterOneValueOne" +
            "&parameterOneName=notParameterOneValueTwo" +
            "&parameterTwoName=notParameterTwoValue"));
    }

    @Test
    public void shouldMatchMatchingStringWithOnlyParameter() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterThree"), not("parameterThreeValueOne"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterThree", "parameterThreeValueOne")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));

        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterOneName"), not("parameterOneValueOne"), not("parameterOneValueTwo"))
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldMatchMatchingStringWithOnlyParameterForEmptyBody() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(),
            new Parameters()
        ).matches(null, "parameterThree=parameterThreeValueOne"));

        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterThree", "parameterThreeValueOne")
        )).matches(null, ""));

        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterThree"), not("parameterThreeValueOne"))
        )).matches(null, ""));
    }

    @Test
    public void shouldNotMatchMatchingStringWithNotParameterAndNormalParameter() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter(not("parameterTwoName"), not("parameterTwoValue")))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldNotMatchMatchingStringWithOnlyNotParameter() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterTwoName"), not("parameterTwoValue")))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldNotMatchMatchingStringWithOnlyNotParameterForBodyWithSingleParameter() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter(not("parameterTwoName"), not("parameterTwoValue")))).matches(null, "" +
            "parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldMatchNullExpectation() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), null).matches(null, "some_value"));
    }

    @Test
    public void shouldNotMatchNullExpectationWhenNotApplied() {
        assertFalse(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), null)).matches(null, "some_value"));
    }

    @Test
    public void shouldMatchEmptyExpectation() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters()).matches(null, "some_value"));
    }

    @Test
    public void shouldNotMatchEmptyExpectationWhenNotApplied() {
        assertFalse(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters())).matches(null, "some_value"));
    }

    @Test
    public void shouldNotMatchIncorrectParameterName() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&INCORRECTParameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldMatchIncorrectParameterNameWhenNotApplied() {
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&INCORRECTParameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldNotMatchIncorrectParameterValue() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=INCORRECTParameterTwoValue"));
    }

    @Test
    public void shouldMatchIncorrectParameterValueWhenNotApplied() {
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&parameterTwoName=INCORRECTParameterTwoValue"));
    }

    @Test
    public void shouldNotMatchIncorrectParameterNameAndValue() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&INCORRECTParameterTwoName=INCORRECTParameterTwoValue"));
    }

    @Test
    public void shouldMatchIncorrectParameterNameAndValueWhenNotApplied() {
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterOneValueOne", "parameterOneValueTwo"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterOneValueOne" +
            "&parameterOneName=parameterOneValueTwo" +
            "&INCORRECTParameterTwoName=INCORRECTParameterTwoValue"));
    }

    @Test
    public void shouldNotMatchNullParameterValue() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterValueOne"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterValueOne" +
            "&parameterTwoName="));
    }

    @Test
    public void shouldMatchNullParameterValueWhenNotApplied() {
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterValueOne"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterValueOne" +
            "&parameterTwoName="));
    }

    @Test
    public void shouldMatchNullParameterValueInExpectation() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterValueOne"),
            new Parameter("parameterTwoName", "")
        )).matches(null, "" +
            "parameterOneName=parameterValueOne" +
            "&parameterTwoName=parameterTwoValue"));
    }

    @Test
    public void shouldNotMatchMissingParameter() {
        assertFalse(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterValueOne"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        )).matches(null, "" +
            "parameterOneName=parameterValueOne"));
    }

    @Test
    public void shouldMatchMissingParameterWhenNotApplied() {
        assertTrue(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters(
            new Parameter("parameterOneName", "parameterValueOne"),
            new Parameter("parameterTwoName", "parameterTwoValue")
        ))).matches(null, "" +
            "parameterOneName=parameterValueOne"));
    }

    @Test
    public void shouldMatchNullTest() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters()).matches(null, null));
    }

    @Test
    public void shouldNotMatchNullTestWhenNotApplied() {
        assertFalse(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters())).matches(null, null));
    }

    @Test
    public void shouldMatchEmptyTest() {
        assertTrue(new ParameterStringMatcher(new MockServerLogger(), new Parameters()).matches(null, ""));
    }

    @Test
    public void shouldNotMatchEmptyTestWhenNotApplied() {
        assertFalse(NotMatcher.not(new ParameterStringMatcher(new MockServerLogger(), new Parameters())).matches(null, ""));
    }
}
