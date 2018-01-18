package org.mockserver.junit;

import com.google.common.annotations.VisibleForTesting;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.socket.PortFactory;

import java.lang.reflect.Field;

public class MockServerRule implements TestRule {

    private static ClientAndServer perTestSuiteClientAndServer;
    private final Object target;
    private final Integer[] port;
    private final boolean perTestSuite;
    private ClientAndServerFactory clientAndServerFactory;
    private ClientAndServer clientAndServer;

    /**
     * Start the MockServer prior to test execution and stop the MockServer after the tests have completed.
     * This constructor dynamically allocates a free port for the MockServer to use.
     *
     * @param target an instance of the test being executed
     */
    public MockServerRule(Object target) {
        this(target, PortFactory.findFreePort());
    }

    /**
     * Start the MockServer prior to test execution and stop the MockServer after the tests have completed.
     * This constructor dynamically allocates a free port for the MockServer to use.
     *
     * @param target       an instance of the test being executed
     * @param perTestSuite indicates how many instances of MockServer are created
     *                     if true a single MockServer is created per JVM
     *                     if false one instance per test class is created
     */
    public MockServerRule(Object target, boolean perTestSuite) {
        this(target, perTestSuite, PortFactory.findFreePort());
    }

    /**
     * Start the proxy prior to test execution and stop the proxy after the tests have completed.
     * This constructor dynamically create a proxy that accepts HTTP(s) requests on the specified port
     *
     * @param target an instance of the test being executed
     * @param port   the HTTP(S) port for the proxy
     */
    public MockServerRule(Object target, Integer... port) {
        this(target, false, port);
    }

    /**
     * Start the proxy prior to test execution and stop the proxy after the tests have completed.
     * This constructor dynamically create a proxy that accepts HTTP(s) requests on the specified port
     *
     * @param target       an instance of the test being executed
     * @param perTestSuite indicates how many instances of MockServer are created
     * @param port         the HTTP(S) port for the proxy
     */
    public MockServerRule(Object target, boolean perTestSuite, Integer... port) {
        this.port = port;
        this.target = target;
        this.perTestSuite = perTestSuite;
        this.clientAndServerFactory = new ClientAndServerFactory(port);
    }

    public Integer getPort() {
        if (port.length > 0) {
            return port[0];
        } else {
            return null;
        }
    }

    public Integer[] getPorts() {
        return port;
    }

    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (perTestSuite) {
                    if (perTestSuiteClientAndServer == null) {
                        perTestSuiteClientAndServer = clientAndServerFactory.newClientAndServer();
                        Runtime.getRuntime().addShutdownHook(new Thread() {
                            @Override
                            public void run() {
                                perTestSuiteClientAndServer.stop();
                            }
                        });
                    }
                    clientAndServer = perTestSuiteClientAndServer;
                } else {
                    clientAndServer = clientAndServerFactory.newClientAndServer();
                }
                setMockServerClient(target, clientAndServer);
                try {
                    if (!perTestSuite) {
                        clientAndServer.reset();
                    }
                    base.evaluate();
                } finally {
                    if (!perTestSuite) {
                        clientAndServer.stop();
                    }
                }
            }
        };
    }

    private void setMockServerClient(Object target, ClientAndServer clientAndServer) {
        for (Class<?> clazz = target.getClass(); !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(MockServerClient.class)) {
                    field.setAccessible(true);
                    try {
                        field.set(target, clientAndServer);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Error setting MockServerClient field on " + target.getClass().getName(), e);
                    }
                }
            }
        }
    }

    public MockServerClient getClient() {
        return clientAndServer;
    }

    @VisibleForTesting
    class ClientAndServerFactory {
        private final Integer[] port;

        public ClientAndServerFactory(Integer... port) {
            this.port = port;
        }

        public ClientAndServer newClientAndServer() {
            return ClientAndServer.startClientAndServer(port);
        }
    }
}
