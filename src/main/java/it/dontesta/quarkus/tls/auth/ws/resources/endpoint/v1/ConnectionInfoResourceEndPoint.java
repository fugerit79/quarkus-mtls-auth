package it.dontesta.quarkus.tls.auth.ws.resources.endpoint.v1;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import it.dontesta.quarkus.tls.auth.ws.utils.CertificateUtil;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

/**
 * JAX-RS Resource class that provides information about the client connection.
 *
 * <p><a href="https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.html#resource">JAX-RS Resource</a>
 *
 * @author Antonio Musarra
 */
@Path("/v1/connection-info")
public class ConnectionInfoResourceEndPoint {

  /**
   * Returns information about the client connection.
   *
   * @param routingContext The routing context for the request.
   * @return A JSON object containing information about the client connection.
   */
  @Path("/info")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConnectionInfo(@Context RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    Map<String, Object> connectionInfo = new HashMap<>();

    connectionInfo.put("isSecure", request.isSSL());
    connectionInfo.put("httpProtocol", request.version().name());
    connectionInfo.put("userAgent", request.getHeader("User-Agent"));
    connectionInfo.put("httpRequestHeaders", getRequestHeaders(request));

    if (request.isSSL()) {
      SSLSession sslSession = request.sslSession();
      if (sslSession != null) {
        connectionInfo.put("protocol", sslSession.getProtocol());
        connectionInfo.put("cipherSuite", sslSession.getCipherSuite());
        connectionInfo.put("server", getServerInfo(sslSession));
        connectionInfo.put("client", getClientInfo(sslSession));
      }
    }

    connectionInfo.put("clientAddress", request.remoteAddress().toString());
    connectionInfo.put("clientPort", request.remoteAddress().port());

    return Response.ok(connectionInfo).build();
  }

  /**
   * Retrieves the request headers from the HttpServerRequest.
   *
   * @param request The HttpServerRequest object.
   * @return A map containing the request headers.
   */
  private Map<String, String> getRequestHeaders(HttpServerRequest request) {
    Map<String, String> requestHeaders = new HashMap<>();
    request.headers().forEach(header -> requestHeaders.put(header.getKey(), header.getValue()));
    return requestHeaders;
  }

  /**
   * Returns information about the server certificate.
   *
   * @param sslSession The SSL session.
   * @return A map containing information about the server certificate.
   */
  private Map<String, Object> getServerInfo(SSLSession sslSession) {
    Map<String, Object> serverInfo = new HashMap<>();
    try {
      Certificate[] serverCerts = sslSession.getLocalCertificates();
      if (serverCerts != null && serverCerts.length > 0) {
        X509Certificate serverCert = (X509Certificate) serverCerts[0];
        populateCertInfo(serverInfo, serverCert);
      }
    } catch (CertificateParsingException e) {
      serverInfo.put("error",
          "Unable to retrieve subjectAlternativeNames: %s".formatted(e.getMessage()));
    }
    return serverInfo;
  }

  /**
   * Returns information about the client certificate.
   *
   * @param sslSession The SSL session.
   * @return A map containing information about the client certificate.
   */
  private Map<String, Object> getClientInfo(SSLSession sslSession) {
    Map<String, Object> clientInfo = new HashMap<>();
    try {
      Certificate[] clientCerts = sslSession.getPeerCertificates();
      if (clientCerts != null && clientCerts.length > 0) {
        X509Certificate clientCert = (X509Certificate) clientCerts[0];
        populateCertInfo(clientInfo, clientCert);
      }
    } catch (SSLPeerUnverifiedException | CertificateParsingException e) {
      clientInfo.put("error",
          "Unable to retrieve SSL peer certificates: %s".formatted(e.getMessage()));
    }
    return clientInfo;
  }

  /**
   * Populates the certificate information into the provided map.
   *
   * @param certInfo The map to populate with certificate information.
   * @param cert     The X509Certificate object.
   * @throws CertificateParsingException If an error occurs while parsing the certificate.
   */
  private void populateCertInfo(Map<String, Object> certInfo, X509Certificate cert)
      throws CertificateParsingException {
    certInfo.put("certSubject", cert.getSubjectX500Principal().getName());
    certInfo.put("certIssuer", cert.getIssuerX500Principal().getName());
    certInfo.put("certSerialNumber", cert.getSerialNumber().toString());
    certInfo.put("notBefore", cert.getNotBefore().toString());
    certInfo.put("notAfter", cert.getNotAfter().toString());
    certInfo.put("keyAlgorithm", cert.getPublicKey().getAlgorithm());
    certInfo.put("keySize", CertificateUtil.getKeySize(cert));
    certInfo.put("subjectAlternativeNames", cert.getSubjectAlternativeNames());
    certInfo.put("certPEM", CertificateUtil.convertToBase64(cert));
  }
}