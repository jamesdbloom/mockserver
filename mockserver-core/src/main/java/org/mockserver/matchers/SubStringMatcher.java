package org.mockserver.matchers;

import org.apache.commons.lang3.StringUtils;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.NottableString;
import org.slf4j.event.Level;

import static org.mockserver.model.NottableString.string;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.TRACE;

/**
 * @author jamesdbloom
 */
public class SubStringMatcher extends BodyMatcher<NottableString> {
    private static final String[] excludedFields = {"mockserverLogger"};
    private final MockServerLogger mockServerLogger;
    private final NottableString matcher;

    SubStringMatcher(MockServerLogger mockServerLogger, NottableString matcher) {
        this.mockServerLogger = mockServerLogger;
        this.matcher = matcher;
    }

    public static boolean matches(String matcher, String matched, boolean ignoreCase) {
        if (StringUtils.isEmpty(matcher)) {
            return true;
        } else if (matched != null) {
            if (StringUtils.contains(matched, matcher)) {
                return true;
            }
            // case insensitive comparison is mainly to improve matching in web containers like Tomcat that convert header names to lower case
            if (ignoreCase) {
                return StringUtils.containsIgnoreCase(matched, matcher);
            }
        }

        return false;
    }

    public boolean matches(final HttpRequest context, String matched) {
        return matches(context, string(matched));
    }

    public boolean matches(final HttpRequest context, NottableString matched) {
        boolean result = false;

        if (matches(matcher.getValue(), matched.getValue(), false)) {
            result = true;
        }

        if (!result) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setType(LogEntry.LogMessageType.DEBUG)
                    .setLogLevel(DEBUG)
                    .setHttpRequest(context)
                    .setMessageFormat("Failed to perform substring match {} with {}")
                    .setArguments(matched, this.matcher)
            );
        }

        return matched.isNot() == (matcher.isNot() == (not != result));
    }

    @Override
    public String[] fieldsExcludedFromEqualsAndHashCode() {
        return excludedFields;
    }
}
