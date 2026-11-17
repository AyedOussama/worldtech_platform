package fr.entreprise.maintenance.testinfra;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fr.entreprise.maintenance.features.identity.application.GoogleIdentityService;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/** Local signed OIDC double, never Google credentials or a replacement for staging validation. */
public final class LocalOidcProvider implements AutoCloseable {
    private record Grant(String jwt, String challenge) {
        @Override public String toString() { return "Grant[redacted]"; }
    }
    private final HttpServer server;
    private final RSAKey key;
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    private final String client = "local-oidc-fixture";
    public LocalOidcProvider() {
        try {
            key = new RSAKeyGenerator(2048).keyID(OpaqueTokens.generate()).generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/jwks", exchange -> {
                byte[] content = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, content.length); exchange.getResponseBody().write(content); exchange.close();
            });
            server.createContext("/token", exchange -> {
                Map<String, String> form = parameters(new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8));
                Grant grant = grants.remove(form.get("code"));
                if (grant == null || !GoogleIdentityService.challenge(form.getOrDefault("code_verifier", "")).equals(grant.challenge())) {
                    byte[] error = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, error.length); exchange.getResponseBody().write(error); exchange.close(); return;
                }
                byte[] content = ("{\"access_token\":\"" + OpaqueTokens.generate() + "\",\"token_type\":\"Bearer\",\"expires_in\":300,\"id_token\":\""
                        + grant.jwt() + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, content.length); exchange.getResponseBody().write(content); exchange.close();
            });
            server.start();
        } catch (Exception failure) { throw new IllegalStateException("OIDC fixture initialization failed."); }
    }
    public ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("google").clientId(client).clientSecret(OpaqueTokens.generate())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/google").scope("openid", "email").authorizationUri(origin() + "/authorize")
                .tokenUri(origin() + "/token").jwkSetUri(origin() + "/jwks").issuerUri("https://accounts.google.com").userNameAttributeName("sub").build();
    }
    public String code(String authorizationUrl, String subject, String email, String fault) throws Exception {
        Map<String, String> request = parameters(URI.create(authorizationUrl).getRawQuery());
        if (!"S256".equals(request.get("code_challenge_method")) || request.get("nonce") == null || request.get("state") == null) {
            throw new AssertionError("OIDC nonce/state/PKCE missing.");
        }
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(fault.equals("issuer") ? "https://wrong.example.invalid" : "https://accounts.google.com")
                .audience(fault.equals("audience") ? "wrong-client" : client).subject(subject).issueTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(fault.equals("expired") ? now.minusSeconds(300) : now.plusSeconds(300)))
                .claim("email", email).claim("email_verified", true).claim("nonce", fault.equals("nonce") ? OpaqueTokens.generate() : request.get("nonce")).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(fault.equals("signature") ? new RSAKeyGenerator(2048).generate() : key));
        String code = OpaqueTokens.generate(); grants.put(code, new Grant(jwt.serialize(), request.get("code_challenge"))); return code;
    }
    public static Map<String, String> parameters(String query) {
        var result = new HashMap<String, String>();
        if (query != null) { for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        } }
        return result;
    }
    private String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); grants.clear(); }
}
