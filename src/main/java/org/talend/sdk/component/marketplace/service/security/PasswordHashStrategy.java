package org.talend.sdk.component.marketplace.service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PasswordHashStrategy {
    private static final String SEP = "$$";
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    @Inject
    @ConfigProperty(name = "talend.marketplace.password.strategy.hash.algorithm", defaultValue = "SHA-512")
    private String algorithm;

    @Inject
    @ConfigProperty(name = "talend.marketplace.password.strategy.hash.iterations", defaultValue = "8")
    private Integer iterations;

    public String hash(final String value) {
        return doHash(algorithm, iterations, value);
    }

    public boolean compare(final String hashedValue, final String clearValue) {
        if (hashedValue == null || clearValue == null) {
            return Objects.equals(hashedValue, clearValue);
        }
        final String[] data = hashedValue.split(Pattern.quote(SEP));
        if (data.length < 3) {
            return false; // corrupted DB?
        }
        return doHash(data[0], Integer.parseInt(data[1]), clearValue).equals(hashedValue);
    }

    private String doHash(final String algorithm, final int iterations, final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] output = value.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < iterations; i++) {
                output = digest.digest(output);
                digest.reset();
            }
            return algorithm + SEP + iterations + SEP + toHex(output);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new IllegalArgumentException(nsae);
        }
    }

    private String toHex(final byte[] data) {
        return IntStream.range(0, data.length)
                     .map(idx -> data[idx])
                     .mapToObj(b -> new char[]{ HEX_CHARS[(b >> 4) & 0xF], HEX_CHARS[(b & 0xF)] })
                     .collect(StringBuilder::new, (b, a) -> b.append(a[0]).append(a[1]), StringBuilder::append)
                     .toString();
    }
}
