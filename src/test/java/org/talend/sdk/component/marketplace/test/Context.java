package org.talend.sdk.component.marketplace.test;

import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Token;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.security.PasswordHashStrategy;
import org.talend.sdk.component.marketplace.service.security.TokenManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class Context {

    @Inject
    private RequestContextController requestContextController;

    @Inject
    private EntityManager entityManager;

    @Inject
    private TokenManager tokenManager;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    public <T> T withToken(final Vendor vendor, final Function<String, T> factory) {
        return withAccount(vendor, (account, password) -> withToken(account, password, factory));
    }

    public <T> T withToken(final Function<String, T> factory) {
        return withAccount(newVendor(), (account, password) -> withToken(account, password, factory));
    }

    public <T> T withToken(final Account account, final String password,
                           final Function<String, T> factory) {
        final AtomicReference<Token> token = new AtomicReference<>();
        execute(() -> token.set(tokenManager.passwordToken(account.getLogin(), password)));
        try {
            return factory.apply("Bearer " + token.get().getAccessToken());
        } finally {
            executeInTx(() -> entityManager.remove(entityManager.getReference(Token.class, token.get().getId())));
        }
    }

    public void execute(final Runnable runnable) {
        requestContextController.activate();
        try {
            runnable.run();
        } finally {
            requestContextController.deactivate();
        }
    }

    public void executeInTx(final Runnable runnable) {
        execute(() -> {
            final EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            try {
                runnable.run();
                entityManager.flush();
                tx.commit();
            } catch (final RuntimeException ex) {
                tx.rollback();
                throw ex;
            }
        });
    }

    public <T> T withAccount(final Vendor vendor, final BiFunction<Account, String, T> factory) {
        final String password = "P@ssw0rd";
        final Account account = new Account();
        account.setName("tester");
        account.setLogin("tester");
        account.setAccountType(SUDO);
        account.setPassword(passwordHashStrategy.hash(password));
        final boolean volatileVendor = vendor.getId() == null;
        executeInTx(() -> {
            account.setVendor(vendor);

            if (volatileVendor) {
                entityManager.persist(vendor);
            }
            entityManager.persist(account);
        });
        try {
            return factory.apply(account, password);
        } finally {
            executeInTx(() -> {
                if (volatileVendor) {
                    entityManager.remove(entityManager.getReference(Vendor.class, account.getVendor().getId()));
                }
                entityManager.remove(entityManager.getReference(Account.class, account.getId()));
            });
        }
    }

    private Vendor newVendor() {
        final Vendor vendor = new Vendor();
        vendor.setName("Test");
        return vendor;
    }
}
