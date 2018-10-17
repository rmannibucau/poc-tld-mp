package org.talend.sdk.component.marketplace.cdi;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.talend.sdk.component.marketplace.configuration.MutableLocalConfigSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityAutoConfigurer implements Extension {
    private final Config config = ConfigProvider.getConfig();

    void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        final MutableLocalConfigSource source = StreamSupport.stream(config.getConfigSources().spliterator(), false)
                                                             .filter(MutableLocalConfigSource.class::isInstance)
                                                             .map(MutableLocalConfigSource.class::cast)
                                                             .findFirst()
                                                             .orElseThrow(() -> new IllegalArgumentException("No MutableLocalConfigSource found"));

        setIssuer(source);

        final boolean defaultValue = "development".equalsIgnoreCase(
                config.getOptionalValue("talend.marketplace.environment", String.class).orElse("production"));
        if (config.getOptionalValue("talend.marketplace.oauth2.keys.generate", Boolean.class).orElse(defaultValue)) {
            final boolean logKeys = config.getOptionalValue("talend.marketplace.oauth2.keys.log", Boolean.class).orElse(defaultValue);
            try {
                final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(512);
                final KeyPair keyPair = generator.generateKeyPair();

                final PrivateKey privateKey = keyPair.getPrivate();
                final String key = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(privateKey.getEncoded()) + "\n-----END PRIVATE KEY-----";
                source.getProperties().put("talend.marketplace.oauth2.token.key.private", key);
                if (logKeys) {
                    log.info("Setting up the private key for tests\n{}", key);
                }

                final PublicKey publicKey = keyPair.getPublic();
                final String pubKey = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "\n-----END PUBLIC KEY-----";
                source.getProperties().put("geronimo.jwt-auth.mp.jwt.verify.publickey", pubKey);
                if (logKeys) {
                    log.info("Setting up the public key for tests\n{}", key);
                }
            } catch (final NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void setIssuer(final MutableLocalConfigSource source) {
        final String mpVerifierIssuerKey = "geronimo.jwt-auth.mp.jwt.verify.issuer"; // bug (prefix) in g-jwt-auth 1.0.0
        config.getOptionalValue("talend.marketplace.oauth2.token.jwt.iss", String.class)
              .map(val -> source.getProperties().put(mpVerifierIssuerKey, val))
              .orElseGet(() -> {
                final String defaultValue = "http://marketplace.talend.com/oauth2/";
                Stream.of("talend.marketplace.oauth2.token.jwt.iss", mpVerifierIssuerKey).forEach(key ->
                        source.getProperties().put(key, defaultValue));
                return defaultValue;
            });
    }
}
