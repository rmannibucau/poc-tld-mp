package org.talend.sdk.component.marketplace.front;

import static javax.ws.rs.client.Entity.entity;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.front.model.TokenModel;
import org.talend.sdk.component.marketplace.front.model.TokenRequest;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.security.PasswordHashStrategy;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class TokenResourceTest {
    @Inject
    private Context context;

    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    @Inject
    private WebTarget target;

    @BeforeEach
    @AfterEach
    void cleanTokens() {
        context.executeInTx(() -> entityManager.createQuery("delete from Token t").executeUpdate());
    }

    @Test
    void password() {
        withAccount((a, password) -> assertNotNull(target.path("security/token")
                                                     .request(MediaType.APPLICATION_JSON_TYPE)
                                                     .post(entity(new TokenRequest(a.getLogin(), password, null, "password"), MediaType.APPLICATION_JSON_TYPE),
                      TokenModel.class).getAccessToken()));
    }

    @Test
    void refresh() {
        withAccount((a, password) -> {
            final String refreshToken = target.path("security/token")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(entity(new TokenRequest(a.getLogin(), password, null, "password"), MediaType.APPLICATION_JSON_TYPE), TokenModel.class).getRefreshToken();
            assertNotNull(target.path("security/token")
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(new TokenRequest(null, null, refreshToken, "refresh_token"), MediaType.APPLICATION_JSON_TYPE),
                                        TokenModel.class)
                                .getAccessToken());
            // refresh token has been invalidated
            assertThrows(WebApplicationException.class, () -> target.path("security/token")
                                                                    .request(MediaType.APPLICATION_JSON_TYPE)
                                                                    .post(entity(new Form()
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", refreshToken), MediaType.APPLICATION_JSON_TYPE),
                TokenModel.class));
        });
    }

    @Test
    void invalidGrant() {
        withAccount((a, p) -> assertThrows(WebApplicationException.class, () -> target.path("security/token")
                                                                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                                                                  .post(entity(new TokenRequest(null, null, null, "foo"), MediaType.APPLICATION_JSON_TYPE),
                      TokenModel.class)));
    }

    @Test
    void invalidPassword() {
        withAccount((a, p) -> assertThrows(WebApplicationException.class, () -> target.path("security/token")
                                                                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                                                                  .post(entity(new TokenRequest(a.getLogin(), "wrong", null, "password"), MediaType.APPLICATION_JSON_TYPE),
                      TokenModel.class)));
    }

    @Test
    void invalidRefreshToken() {
        withAccount((a, p) -> assertThrows(WebApplicationException.class, () -> target.path("security/token")
                                                                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                                                                  .post(entity(new TokenRequest(null, null, "wrong", "refresh_token"), MediaType.APPLICATION_JSON_TYPE),
                      TokenModel.class)));
    }

    private void withAccount(final BiConsumer<Account, String> consumer) {
        final Vendor vendor = new Vendor();
        vendor.setName("talend");

        final String password = "S3cr8tP@ssword";
        final Account account = new Account();
        account.setName("Test User");
        account.setLogin("login@talend.com");
        account.setPassword(passwordHashStrategy.hash(password));
        account.setVendor(vendor);
        account.setAccountType(SUDO);

        context.executeInTx(() -> {
            entityManager.persist(vendor);
            entityManager.persist(account);
        });
        try {
            context.execute(() -> consumer.accept(account, password));
        } finally {
            context.executeInTx(() -> {
                entityManager.remove(entityManager.getReference(Vendor.class, vendor.getId()));
                entityManager.remove(entityManager.getReference(Account.class, account.getId()));
            });
        }
    }
}
