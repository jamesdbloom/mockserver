package org.mockserver.cli;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.configuration.IntegerStringListParser;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mockserver.MockServer;

import java.io.PrintStream;
import java.util.*;

import static org.mockserver.character.Character.NEW_LINE;
import static org.mockserver.cli.Main.Arguments.*;

/**
 * @author jamesdbloom
 */
public class Main {
    static final String USAGE = "" +
        "   java -jar <path to mockserver-jetty-jar-with-dependencies.jar> -serverPort <port> [-proxyRemotePort <port>] [-proxyRemoteHost <hostname>] [-logLevel <level>] " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE +
        "     valid options are:                                                                                                                                          " + NEW_LINE +
        "        -serverPort <port>           The HTTP, HTTPS, SOCKS and HTTP CONNECT                                                                                     " + NEW_LINE +
        "                                     port for the MockServer for both mocking                                                                                    " + NEW_LINE +
        "                                     and proxying requests.  Port unification                                                                                    " + NEW_LINE +
        "                                     is used to support all protocols for                                                                                        " + NEW_LINE +
        "                                     proxying and mocking on the same port.                                                                                      " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE +
        "        -proxyRemotePort <port>      Optionally enables port forwarding mode.                                                                                    " + NEW_LINE +
        "                                     When specified all requests received will                                                                                   " + NEW_LINE +
        "                                     be forwarded to the specified port, unless                                                                                  " + NEW_LINE +
        "                                     they match an expectation.                                                                                                  " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE +
        "        -proxyRemoteHost <hostname>  Specified the host to forward all proxy                                                                                     " + NEW_LINE +
        "                                     requests to when port forwarding mode has                                                                                   " + NEW_LINE +
        "                                     been enabled using the proxyRemotePort                                                                                      " + NEW_LINE +
        "                                     option.  This setting is ignored unless                                                                                     " + NEW_LINE +
        "                                     proxyRemotePort has been specified. If no                                                                                   " + NEW_LINE +
        "                                     value is provided for proxyRemoteHost when                                                                                  " + NEW_LINE +
        "                                     proxyRemotePort has been specified,                                                                                         " + NEW_LINE +
        "                                     proxyRemoteHost will default to \"localhost\".                                                                              " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE +
        "        -logLevel <level>            Optionally specify log level as TRACE, DEBUG,                                                                               " + NEW_LINE +
        "                                     INFO, WARN, ERROR or OFF. If not specified                                                                                  " + NEW_LINE +
        "                                     default is INFO                                                                                                             " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE +
        "   i.e. java -jar ./mockserver-jetty-jar-with-dependencies.jar -serverPort 1080 -proxyRemotePort 80 -proxyRemoteHost www.mock-server.com -logLevel WARN          " + NEW_LINE +
        "                                                                                                                                                                 " + NEW_LINE;
    private static final MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger(Main.class);
    private static final IntegerStringListParser INTEGER_STRING_LIST_PARSER = new IntegerStringListParser();
    public static PrintStream systemOut = System.out;

    /**
     * Run the MockServer directly providing the arguments as specified below.
     *
     * @param arguments the entries are in pairs:
     *                  - "-serverPort"       followed by the mandatory server local port,
     *                  - "-proxyRemotePort"  followed by the optional proxyRemotePort port that enabled port forwarding mode,
     *                  - "-proxyRemoteHost"  followed by the optional proxyRemoteHost port (ignored unless proxyRemotePort is specified)
     *                  - "-logLevel"         followed by the log level
     */
    public static void main(String... arguments) {
        try {
            Map<String, String> parsedArguments = parseArguments(arguments);

            MOCK_SERVER_LOGGER.debug("Using command line options: {}", Joiner.on(", ").withKeyValueSeparator("=").join(parsedArguments));

            if (parsedArguments.size() > 0 && parsedArguments.containsKey(serverPort.name())) {
                if (parsedArguments.containsKey(logLevel.name())) {
                    ConfigurationProperties.logLevel(parsedArguments.get(logLevel.name()));
                }
                Integer[] localPorts = INTEGER_STRING_LIST_PARSER.toArray(parsedArguments.get(serverPort.name()));
                if (parsedArguments.containsKey(proxyRemotePort.name())) {
                    String remoteHost = parsedArguments.get(proxyRemoteHost.name());
                    if (Strings.isNullOrEmpty(remoteHost)) {
                        remoteHost = "localhost";
                    }
                    new MockServer(remoteHost, Integer.parseInt(parsedArguments.get(proxyRemotePort.name())), localPorts);
                } else {
                    new MockServer(localPorts);
                }
            } else {
                showUsage();
            }
        } catch (IllegalArgumentException iae) {
            showUsage();
        }
    }

    private static Map<String, String> parseArguments(String... arguments) {
        Map<String, String> parsedArguments = new HashMap<>();
        List<String> errorMessages = new ArrayList<>();

        Iterator<String> argumentsIterator = Arrays.asList(arguments).iterator();
        while (argumentsIterator.hasNext()) {
            final String next = argumentsIterator.next();
            String argumentName = StringUtils.substringAfter(next, "-");
            if (argumentsIterator.hasNext()) {
                String argumentValue = argumentsIterator.next();
                if (!Arguments.names().containsIgnoreCase(argumentName)) {
                    showUsage();
                    break;
                } else {
                    String errorMessage = "";
                    switch (Arguments.valueOf(argumentName)) {
                        case serverPort:
                            if (!argumentValue.matches("^\\d+(,\\d+)*$")) {
                                errorMessage = argumentName + " value \"" + argumentValue + "\" is invalid, please specify a comma separated list of ports i.e. \"1080,1081,1082\"";
                            }
                            break;
                        case proxyRemotePort:
                            if (!argumentValue.matches("^\\d+$")) {
                                errorMessage = argumentName + " value \"" + argumentValue + "\" is invalid, please specify a port i.e. \"1080\"";
                            }
                            break;
                        case proxyRemoteHost:
                            String validIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
                            String validHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
                            if (!(argumentValue.matches(validIpAddressRegex) || argumentValue.matches(validHostnameRegex))) {
                                errorMessage = argumentName + " value \"" + argumentValue + "\" is invalid, please specify a host name i.e. \"localhost\" or \"127.0.0.1\"";
                            }
                            break;
                        case logLevel:
                            if (!Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF").contains(argumentValue)) {
                                errorMessage = argumentName + " value \"" + argumentValue + "\" is invalid, please specify one of \"TRACE\", \"DEBUG\", \"INFO\", \"WARN\", \"ERROR\", \"OFF\"";
                            }
                            break;
                    }
                    if (errorMessage.isEmpty()) {
                        parsedArguments.put(argumentName, argumentValue);
                    } else {
                        errorMessages.add(errorMessage);
                    }
                }
            } else {
                showUsage();
                break;
            }
        }

        if (!errorMessages.isEmpty()) {
            printValidationEror(errorMessages);
            throw new IllegalArgumentException(errorMessages.toString());
        }
        return parsedArguments;
    }

    private static void printValidationEror(List<String> errorMessages) {
        int maxLengthMessage = 0;
        for (String errorMessage : errorMessages) {
            if (errorMessage.length() > maxLengthMessage) {
                maxLengthMessage = errorMessage.length();
            }
        }
        systemOut.println(NEW_LINE + "   " + Strings.padEnd("", maxLengthMessage, '='));
        for (String errorMessage : errorMessages) {
            systemOut.println("   " + errorMessage);
        }
        systemOut.println("   " + Strings.padEnd("", maxLengthMessage, '=') + NEW_LINE);
    }

    private static void showUsage() {
        systemOut.print(USAGE);
    }

    public enum Arguments {
        serverPort,
        proxyRemotePort,
        proxyRemoteHost,
        logLevel;

        final static CaseInsensitiveList names = new CaseInsensitiveList();

        static {
            for (Arguments arguments : values()) {
                names.add(arguments.name());
            }
        }

        public static CaseInsensitiveList names() {
            return names;
        }
    }

    static class CaseInsensitiveList extends ArrayList<String> {

        CaseInsensitiveList() {
            super();
        }

        boolean containsIgnoreCase(String matcher) {
            for (String listItem : this) {
                if (listItem.equalsIgnoreCase(matcher)) {
                    return true;
                }
            }
            return false;
        }
    }

}
