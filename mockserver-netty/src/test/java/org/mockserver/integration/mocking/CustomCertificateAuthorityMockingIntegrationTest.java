package org.mockserver.integration.mocking;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.integration.server.AbstractBasicMockingIntegrationTest;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * @author jamesdbloom
 */
public class CustomCertificateAuthorityMockingIntegrationTest extends AbstractBasicMockingIntegrationTest {

    private static int mockServerPort;
    private static String originalCertificateAuthorityCertificate;
    private static String originalCertificateAuthorityPrivateKey;

    @BeforeClass
    public static void startServer() {
        // save original value
        originalCertificateAuthorityCertificate = ConfigurationProperties.certificateAuthorityCertificate();
        originalCertificateAuthorityPrivateKey = ConfigurationProperties.certificateAuthorityPrivateKey();

        // set new certificate authority values
        ConfigurationProperties.certificateAuthorityCertificate("org/mockserver/integration/mocking/ca.pem");
        ConfigurationProperties.certificateAuthorityPrivateKey("org/mockserver/integration/mocking/ca-key.pem");

        mockServerClient = startClientAndServer();
        mockServerPort = ((ClientAndServer) mockServerClient).getLocalPort();
    }

    @AfterClass
    public static void stopServer() {
        stopQuietly(mockServerClient);

        // set back to original value
        ConfigurationProperties.certificateAuthorityCertificate(originalCertificateAuthorityCertificate);
        ConfigurationProperties.certificateAuthorityPrivateKey(originalCertificateAuthorityPrivateKey);
    }

    @Override
    public int getServerPort() {
        return mockServerPort;
    }

}
