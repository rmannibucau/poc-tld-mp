package org.talend.sdk.component.marketplace.front;

import static java.util.Optional.ofNullable;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.marketplace.front.model.TokenModel;
import org.talend.sdk.component.marketplace.front.model.TokenRequest;
import org.talend.sdk.component.marketplace.model.Token;
import org.talend.sdk.component.marketplace.service.security.TokenManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("security")
@ApplicationScoped
public class TokenResource {
    @Inject
    private TokenManager manager;

    @Inject
    @ConfigProperty(name = "talend.marketplace.oauth2.token.expirationApproximationResponseTimeMs", defaultValue = "200")
    private Long expirationApprox;

    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TokenModel token(final TokenRequest request) {
        switch (ofNullable(request.getGrantType()).orElse("password")) {
            case "password":
                return toModel(manager.passwordToken(request.getUsername(), request.getPassword()));
            case "refresh_token":
                return toModel(manager.refreshToken(request.getRefreshToken()));
            default:
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @Path("logout")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void logout(@FormParam("access_token") final String accessToken) {
        manager.removeByAccessToken(accessToken);
    }

    private TokenModel toModel(final Token token) {
        final TokenModel model = new TokenModel();
        model.setTokenType("bearer");
        model.setAccessToken(token.getAccessToken());
        model.setRefreshToken(token.getRefreshToken());
        model.setExpiresIn(TimeUnit.MILLISECONDS.toSeconds(Math.max(0, token.getExpiredAt().getTime() - System.currentTimeMillis() - expirationApprox)));
        return model;
    }
}
