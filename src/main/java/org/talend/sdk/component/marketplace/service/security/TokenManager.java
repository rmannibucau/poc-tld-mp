package org.talend.sdk.component.marketplace.service.security;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbProperty;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.marketplace.cdi.Tx;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Token;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class TokenManager {
    private static final int EVICTION_ITERATION_DURATION = 250;

    @Inject
    private EntityManager entityManager;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.jwt.iss")
    private String jwtIssuer;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.jwt.algorithm", defaultValue = "RS256")
    private String jwtAlgorithm;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.jwt.typ", defaultValue = "JWT")
    private String jwtTyp;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.jwt.kid", defaultValue = "talend-marketplace")
    private String jwtKid;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.key.private")
    private String jwtPrivateKey;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.eviction.durationSec", defaultValue = "300")
    private Long evictionDelay;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.expirationMs", defaultValue = "1800000")
    private Long expiration;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    @Inject
    private TokenManager self;

    @Inject
    private Jsonb jsonb;

    @Inject
    private RequestContextController contextController;

    @Inject
    private Config config;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread evictionThread;

    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    private String jwtHeader;
    private String jwtJavaAlgorithm;
    private PrivateKey privateKey;

    void onStart(@Observes @Initialized(ApplicationScoped.class) final Object startup) {
        if (!"JWT".equalsIgnoreCase(jwtTyp)) {
            throw new IllegalArgumentException("For now only JWT is supported for token generation");
        }

        final String keyAlgo;
        switch (jwtAlgorithm.toLowerCase(ROOT)) {
            case "rs256":
                keyAlgo = "RSA";
                jwtJavaAlgorithm = "SHA256withRSA";
                break;
            default:
                throw new IllegalArgumentException("For now only RS256 is supported for JWT generation");
        }

        final JwtHeader jwtHeaderModel = new JwtHeader();
        jwtHeaderModel.setAlg(jwtAlgorithm);
        jwtHeaderModel.setKid(jwtKid);
        jwtHeaderModel.setTyp("JWT");
        jwtHeader = encodeJwtSegment(jwtHeaderModel);

        privateKey = toPrivateKey(loadContent(jwtPrivateKey), keyAlgo);

        if (evictionDelay > 0) {
            evictionThread = new Thread(() -> {
                final long iterations = TimeUnit.SECONDS.toMillis(evictionDelay) / EVICTION_ITERATION_DURATION;
                while (running.get()) {
                    eivctionIteration(iterations);
                }
            });
            evictionThread.setName(getClass().getName() + "-eviction-thread");
            evictionThread.start();
        }
    }

    @PreDestroy
    private void destroy() {
        running.set(false);
        ofNullable(evictionThread).ifPresent(t -> {
            try {
                t.join(EVICTION_ITERATION_DURATION * 2);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (t.isAlive()) {
                t.interrupt();
            }
        });

    }

    @Tx
    public void doEviction() {
        final int update = entityManager.createNamedQuery("Token.deleteExpiredTokens")
                                        .setParameter("until", new Date())
                                        .executeUpdate();
        if (update > 0) {
            log.info("Expired {} tokens", update);
        }
    }

    @Tx
    public void removeByAccessToken(final String accessToken) {
        try {
            entityManager.remove(entityManager.createNamedQuery("Token.findByAccessToken", Token.class)
                                              .setParameter("accessToken", accessToken)
                                              .getSingleResult());
        } catch (final NoResultException nre) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Tx
    public Token refreshToken(final String refreshToken) {
        try {
            final Token token = entityManager.createNamedQuery("Token.findByRefreshToken", Token.class)
                                             .setParameter("refreshToken", refreshToken)
                                             .getSingleResult();
            entityManager.remove(token);
            return newToken(token.getAccount());
        } catch (final NoResultException nre) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }

    @Tx
    public Token passwordToken(final String username, final String password) {
        try {
            final Account account = entityManager.createNamedQuery("Account.findByLogin", Account.class)
                                           .setParameter("login", username)
                                           .getSingleResult();
            if (!passwordHashStrategy.compare(account.getPassword(), password)) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            return newToken(account);
        } catch (final NoResultException nre) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }

    private String generateAccessToken(final Account account, final long expirationTime) {
        final JwtPayload jwtPayload = new JwtPayload();
        jwtPayload.setIss(jwtIssuer);
        jwtPayload.setIat(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        jwtPayload.setExp(TimeUnit.MILLISECONDS.toSeconds(expirationTime));
        jwtPayload.setSub(account.getLogin());
        jwtPayload.setCreatedAt(System.nanoTime()); // to ensure in the same seconds we can generate N tokens
        jwtPayload.setFullName(account.getName());
        jwtPayload.setGroups(toGroups(account));
        final String payload = encodeJwtSegment(jwtPayload);
        final String signingString = jwtHeader + '.' + payload;

        try {
            final Signature signature = Signature.getInstance(jwtJavaAlgorithm);
            signature.initSign(privateKey);
            signature.update(signingString.getBytes(StandardCharsets.UTF_8));
            return signingString + '.' + base64Encoder.encodeToString(signature.sign());
        } catch (final NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> toGroups(final Account account) {
        switch (account.getAccountType()) {
            case SUDO:
                return singletonList("**");
            case MACHINE:
                return singletonList("*");
            case USER:
                return singletonList(account.getVendor().getId());
            default:
                return emptyList();
        }
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    private Token newToken(final Account account) {
        final long expirationTime = System.currentTimeMillis() + expiration;
        final Token token = new Token();
        token.setId(UUID.randomUUID().toString());
        token.setAccount(account);
        token.setExpiredAt(new Date(expirationTime));
        token.setAccessToken(generateAccessToken(account, expirationTime));
        token.setRefreshToken(generateRefreshToken());
        entityManager.persist(token);
        entityManager.flush();
        return token;
    }

    private PrivateKey toPrivateKey(final String key, final String algo) {
        try {
            switch (algo) {
                case "RSA": {
                    final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key
                            .replace("-----BEGIN RSA KEY-----", "")
                            .replace("-----END RSA KEY-----", "")
                            .replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                            .replace("-----END RSA PRIVATE KEY-----", "")
                            .replace("\n", "")
                            .trim()));
                    final KeyFactory keyFactory = KeyFactory.getInstance(algo);
                    return keyFactory.generatePrivate(keySpec);
                }
                default:
                    throw new IllegalArgumentException("Invalid key");
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid key decoding");
        }
    }

    private String loadContent(final String value) {
        final File file = new File(value);
        if (file.exists()) {
            try {
                return String.join("\n", Files.readAllLines(file.toPath()));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                                              .getResourceAsStream(value)) {
            if (stream != null) {
                return new BufferedReader(new InputStreamReader(stream)).lines().collect(joining("\n"));
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        return value;
    }

    private String encodeJwtSegment(final Object model) {
        return base64Encoder.encodeToString(jsonb.toJson(model).getBytes(StandardCharsets.UTF_8));
    }

    private void eivctionIteration(final long iterations) {
        contextController.activate();
        try {
            self.doEviction();
        } catch (final RuntimeException re) {
            log.warn(re.getMessage(), re);
        } finally {
            contextController.deactivate();
        }
        for (long i = 0; i < iterations; i++) {
            if (!running.get()) {
                break;
            }
            try {
                Thread.sleep(EVICTION_ITERATION_DURATION);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Data
    private static class JwtHeader {
        private String alg;
        private String typ;
        private String kid;
    }

    @Data
    private static class JwtPayload {
        private long exp;
        private long iat;
        private String iss;
        private String sub;
        private long createdAt;

        @JsonbProperty("full_name")
        private String fullName;

        private Collection<String> groups;
    }
}
