package org.talend.sdk.component.marketplace.service.security;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.MACHINE;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;
import static org.talend.sdk.component.marketplace.model.Account.AccountType.USER;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.bind.Jsonb;
import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Token;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class TokenManagerTest {

    @Inject
    private TokenManager manager;

    @Inject
    private Context context;

    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    @Inject
    private Jsonb jsonb;

    @BeforeEach
    @AfterEach
    void cleanTokens() {
        context.executeInTx(() -> entityManager.createQuery("delete from Token t").executeUpdate());
    }

    @Test
    void loginSudo() {
        withAccount(SUDO, (account, password) -> assertTokenGeneration(account, password, "**"));
    }

    @Test
    void loginMachine() {
        withAccount(MACHINE, (account, password) -> assertTokenGeneration(account, password, "*"));
    }

    @Test
    void loginValid() {
        withAccount(USER, (account, password) -> assertTokenGeneration(account, password, account.getVendor().getId()));
    }

    @Test
    void loginInvalid() {
        withAccount(USER, (account, password) -> assertThrows(WebApplicationException.class,
                () -> manager.passwordToken(account.getLogin(), password + "wrong")));
    }

    private void assertTokenGeneration(final Account account, final String password, final String group) {
        final Token token = manager.passwordToken(account.getLogin(), password);
        assertNotNull(token);
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertNotNull(token.getExpiredAt());
        assertEquals(account.getId(), token.getAccount().getId());
        final JsonObject payload = jsonb.fromJson(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(token.getAccessToken().split("\\.")[1])),
                JsonObject.class);
        assertEquals(
                singletonList(group),
                payload.getJsonArray("groups").stream()
                       .map(JsonString.class::cast).map(JsonString::getString)
                       .collect(toList()));
    }

    private void withAccount(final Account.AccountType type, final BiConsumer<Account, String> consumer) {
        final Vendor vendor = new Vendor();
        vendor.setName("talend");

        final String password = "S3cr8tP@ssword";
        final Account account = new Account();
        account.setName("Test User");
        account.setLogin("login@talend.com");
        account.setPassword(passwordHashStrategy.hash(password));
        account.setVendor(vendor);
        account.setAccountType(type); // to test we have the vendor id in groups

        context.executeInTx(() -> {
            entityManager.persist(vendor);
            entityManager.persist(account);
            entityManager.flush();
        });
        try {
            context.execute(() -> consumer.accept(account, password));
        } finally {
            context.executeInTx(() -> {
                entityManager.remove(entityManager.getReference(Vendor.class, vendor.getId()));
                entityManager.remove(entityManager.getReference(Account.class, account.getId()));
                entityManager.flush();
            });
        }
    }
}
