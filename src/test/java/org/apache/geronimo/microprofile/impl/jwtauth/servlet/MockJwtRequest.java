package org.apache.geronimo.microprofile.impl.jwtauth.servlet;

import static java.util.Collections.singleton;

import java.lang.reflect.Proxy;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.geronimo.microprofile.impl.jwtauth.jwt.JwtParser;
import org.eclipse.microprofile.jwt.JsonWebToken;

// simple mock to not require a request to test secured services, no more needed in geronimo-jwt-auth 1.0.1
public class MockJwtRequest extends JwtRequest {

    public MockJwtRequest() {
        super(new JwtParser(), "Authorization", "Bearer ",
                HttpServletRequest.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { HttpServletRequest.class }, (p, m, a) -> null)));
    }

    @Override
    public JsonWebToken getToken() {
        return new JsonWebToken() {
            @Override
            public String getName() {
                return "mock";
            }

            @Override
            public Set<String> getClaimNames() {
                return singleton("groups");
            }

            @Override
            public <T> T getClaim(final String claimName) {
                return (T) singleton("**");
            }
        };
    }
}
