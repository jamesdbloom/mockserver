package org.mockserver.cli;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.mockserver.mockserver.MockServerBuilder;
import org.mockserver.proxy.ProxyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;

/**
 * @author jamesdbloom
 */
public class Main {
    public static final String SERVER_PORT_KEY = "serverPort";
    public static final String SERVER_DATABASE_FILE = "databaseFile";
    public static final String PROXY_PORT_KEY = "proxyPort";
    public static final String PROXY_REMOTE_PORT_KEY = "proxyRemotePort";
    public static final String PROXY_REMOTE_HOST_KEY = "proxyRemoteHost";
    public static final String USAGE = "" +
            "   java -jar <path to mockserver-jetty-jar-with-dependencies.jar> [-serverPort <port>] [-proxyPort <port>] [-proxyRemotePort <port>] [-proxyRemoteHost <hostname>]" + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "     valid options are:                                                                " + System.getProperty("line.separator") +
            "        -serverPort <port>           Specifies the HTTP, HTTPS, SOCKS and HTTP         " + System.getProperty("line.separator") +
            "                                     CONNECT port for proxy. Port unification          " + System.getProperty("line.separator") +
            "                                     supports for all protocols on the same port.      " + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "        -databaseFile <filename>     Specifies the database file name, where mock      " + System.getProperty("line.separator") +
            "                                     server saves the expectations when exits.         " + System.getProperty("line.separator") +
            "                                     If you start the server with a database file      " + System.getProperty("line.separator") +
            "                                     you do not have to configure the expectations     " + System.getProperty("line.separator") +
            "                                     every time.                                       " + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "        -proxyPort <port>            Specifies the HTTP and HTTPS port for the         " + System.getProperty("line.separator") +
            "                                     MockServer. Port unification is used to           " + System.getProperty("line.separator") +
            "                                     support HTTP and HTTPS on the same port.          " + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "        -proxyRemotePort <port>      Specifies the port to forward all proxy           " + System.getProperty("line.separator") +
            "                                     requests to (i.e. all requests received on        " + System.getProperty("line.separator") +
            "                                     portPort). This setting is used to enable         " + System.getProperty("line.separator") +
            "                                     the port forwarding mode therefore this           " + System.getProperty("line.separator") +
            "                                     option disables the HTTP, HTTPS, SOCKS and        " + System.getProperty("line.separator") +
            "                                     HTTP CONNECT support.                             " + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "        -proxyRemoteHost <hostname>  Specified the host to forward all proxy           " + System.getProperty("line.separator") +
            "                                     requests to (i.e. all requests received on        " + System.getProperty("line.separator") +
            "                                     portPort). This setting is ignored unless         " + System.getProperty("line.separator") +
            "                                     proxyRemotePort has been specified. If no         " + System.getProperty("line.separator") +
            "                                     value is provided for proxyRemoteHost when        " + System.getProperty("line.separator") +
            "                                     proxyRemotePort has been specified,               " + System.getProperty("line.separator") +
            "                                     proxyRemoteHost will default to \"localhost\".    " + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator") +
            "   i.e. java -jar ./mockserver-jetty-jar-with-dependencies.jar -serverPort 1080 -proxyPort 1090 -proxyRemotePort 80 -proxyRemoteHost www.mock-server.com" + System.getProperty("line.separator") +
            "                                                                                       " + System.getProperty("line.separator");

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @VisibleForTesting
    static ProxyBuilder httpProxyBuilder = new ProxyBuilder();
    @VisibleForTesting
    static MockServerBuilder mockServerBuilder = new MockServerBuilder();
    @VisibleForTesting
    static PrintStream outputPrintStream = System.out;
    @VisibleForTesting
    static Runtime runtime = Runtime.getRuntime();
    private static boolean usagePrinted = false;


    /**
     * Run the MockServer directly providing the parseArguments for the server and httpProxyBuilder as the only input parameters (if not provided the server port defaults to 8080 and the httpProxyBuilder is not started).
     *
     * @param arguments the entries are in pairs:
     *                  - the first  pair is "-serverPort" followed by the server port if not provided the MockServer is not started,
     *                  - the second pair is "-proxyPort"  followed by the httpProxyBuilder  port if not provided the httpProxyBuilder      is not started
     */
    public static void main(String... arguments) {
        usagePrinted = false;

        Map<String, String> parsedArguments = parseArguments(arguments);

        if (logger.isDebugEnabled()) {
            logger.debug(System.getProperty("line.separator") + System.getProperty("line.separator") + "Using command line options: " +
                    Joiner.on(", ").withKeyValueSeparator("=").join(parsedArguments) + System.getProperty("line.separator"));
        }

        if (parsedArguments.size() > 0 && validateArguments(parsedArguments)) {
            if (parsedArguments.containsKey(SERVER_PORT_KEY)) {
                mockServerBuilder.withHTTPPort(Integer.parseInt(parsedArguments.get(SERVER_PORT_KEY))).withDatabase(parsedArguments.get(SERVER_DATABASE_FILE)).build();
            }
            if (parsedArguments.containsKey(PROXY_PORT_KEY)) {
                ProxyBuilder proxyBuilder = httpProxyBuilder.withLocalPort(Integer.parseInt(parsedArguments.get(PROXY_PORT_KEY)));
                if (parsedArguments.containsKey(PROXY_REMOTE_PORT_KEY)) {
                    String remoteHost = parsedArguments.get(PROXY_REMOTE_HOST_KEY);
                    if (Strings.isNullOrEmpty(remoteHost)) {
                        remoteHost = "localhost";
                    }
                    proxyBuilder.withDirect(remoteHost, Integer.parseInt(parsedArguments.get(PROXY_REMOTE_PORT_KEY)));
                }
                proxyBuilder.build();
            }
        } else {
            showUsage();
        }
    }

    private static boolean validateArguments(Map<String, String> parsedArguments) {
        List<String> errorMessages = new ArrayList<String>();
        validatePortArgument(parsedArguments, SERVER_PORT_KEY, errorMessages);
        validatePortArgument(parsedArguments, PROXY_PORT_KEY, errorMessages);
        validatePortArgument(parsedArguments, PROXY_REMOTE_PORT_KEY, errorMessages);
        validateHostnameArgument(parsedArguments, PROXY_REMOTE_HOST_KEY, errorMessages);
        validateFileArgument(parsedArguments, SERVER_DATABASE_FILE, errorMessages);

        if (!errorMessages.isEmpty()) {
            int maxLengthMessage = 0;
            for (String errorMessage : errorMessages) {
               if (errorMessage.length() > maxLengthMessage) {
                   maxLengthMessage = errorMessage.length();
               }
            }
            outputPrintStream.println(System.getProperty("line.separator") + "   " + Strings.padEnd("", maxLengthMessage, '='));
            for (String errorMessage : errorMessages) {
                outputPrintStream.println("   " + errorMessage);
            }
            outputPrintStream.println("   " + Strings.padEnd("", maxLengthMessage, '=') + System.getProperty("line.separator"));
            return false;
        }
        return true;
    }

    private static void validateFileArgument(Map<String, String> parsedArguments, String argumentKey, List<String> errorMessages) {
        return;
    }

    private static void validatePortArgument(Map<String, String> parsedArguments, String argumentKey, List<String> errorMessages) {
        if (parsedArguments.containsKey(argumentKey) && !parsedArguments.get(argumentKey).matches("^\\d+$")) {
            errorMessages.add(argumentKey + " value \"" + parsedArguments.get(argumentKey) + "\" is invalid, please specify a port i.e. \"1080\"");
        }
    }

    private static void validateHostnameArgument(Map<String, String> parsedArguments, String argumentKey, List<String> errorMessages) {
        String validIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
        String validHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        if (parsedArguments.containsKey(argumentKey) && !(parsedArguments.get(argumentKey).matches(validIpAddressRegex) || parsedArguments.get(argumentKey).matches(validHostnameRegex))) {
            errorMessages.add(argumentKey + " value \"" + parsedArguments.get(argumentKey) + "\" is invalid, please specify a host name i.e. \"localhost\" or \"127.0.0.1\"");
        }
    }

    private static Map<String, String> parseArguments(String... arguments) {
        Map<String, String> parsedArguments = new HashMap<String, String>();
        Iterator<String> argumentsIterator = Arrays.asList(arguments).iterator();
        while (argumentsIterator.hasNext()) {
            String argumentName = argumentsIterator.next();
            if (argumentsIterator.hasNext()) {
                String argumentValue = argumentsIterator.next();
                if (!parseArgument(parsedArguments, SERVER_PORT_KEY, argumentName, argumentValue)
                        && !parseArgument(parsedArguments, PROXY_PORT_KEY, argumentName, argumentValue)
                        && !parseArgument(parsedArguments, PROXY_REMOTE_PORT_KEY, argumentName, argumentValue)
                        && !("-" + PROXY_REMOTE_HOST_KEY).equalsIgnoreCase(argumentName)
                        && !parseArgument(parsedArguments, SERVER_DATABASE_FILE, argumentName, argumentValue)) {
                    showUsage();
                    break;
                }
                if (("-" + PROXY_REMOTE_HOST_KEY).equalsIgnoreCase(argumentName)) {
                    parsedArguments.put(PROXY_REMOTE_HOST_KEY, argumentValue);
                }
            } else {
                showUsage();
                break;
            }
        }
        return parsedArguments;
    }

    private static boolean parseArgument(Map<String, String> parsedArguments, final String key, final String argumentName, final String argumentValue) {
        if (argumentName.equals("-" + key)) {
            parsedArguments.put(key, argumentValue);
            return true;
        }
        return false;
    }

    private static void showUsage() {
        if (!usagePrinted) {
            outputPrintStream.print(USAGE);
            runtime.exit(1);
            usagePrinted = true;
        }
    }

}
