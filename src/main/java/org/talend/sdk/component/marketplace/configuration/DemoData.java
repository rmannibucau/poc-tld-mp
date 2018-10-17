package org.talend.sdk.component.marketplace.configuration;

import static org.talend.sdk.component.marketplace.model.Account.AccountType.SUDO;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.marketplace.cdi.Tx;
import org.talend.sdk.component.marketplace.model.Account;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.security.PasswordHashStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DemoData {
    @Inject
    private EntityManager entityManager;

    @Inject
    private PasswordHashStrategy passwordHashStrategy;

    @Inject
    @ConfigProperty(name = "talend.marketplace.environment", defaultValue = "production")
    private String environment;

    @Inject
    @ConfigProperty(name = "talend.marketplace.demo.user.default.login", defaultValue = "demo@talend.com")
    private String defaultUserLogin;

    @Inject
    @ConfigProperty(name = "talend.marketplace.demo.user.default.password", defaultValue = "P@ssw0rd")
    private String defaultUserPassword;

    @Tx
    void onStart(@Observes @Initialized(ApplicationScoped.class) final ServletContext context) {
        if (!"development".equals(environment)) {
            return;
        }

        final Vendor vendor = createVendor();
        createAccount(vendor);
        IntStream.range(0, 15).forEach(idx -> createComponent(vendor, idx));
        entityManager.flush();

        log.info("\n\nDEMO DATA:\n- Admin user: \"{}\" / \"{}\"\n\n", defaultUserLogin, defaultUserPassword);
    }

    private void createComponent(final Vendor vendor, final int idx) {
        final Component component = new Component();
        component.setName("A Component #" + (idx + 1));
        component.setDescription(IntStream.range(0, (int) (Math.random() * 200))
                                          .mapToObj(i -> RandomStringUtils.randomAlphanumeric((int) (Math.random() * 30)))
                                          .collect(Collectors.joining(" ")));
        component.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
        component.setSources("https://github.com/Talend/component-runtime");
        component.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
        component.setChangelog("Things can change");
        component.setDocumentation("https://talend.github.io/component-runtime/");
        component.setVendor(vendor);
        entityManager.persist(component);
    }

    private Vendor createVendor() {
        final Vendor vendor = new Vendor();
        vendor.setName("Talend");
        entityManager.persist(vendor);
        return vendor;
    }

    private void createAccount(final Vendor vendor) {
        final Account account = new Account();
        account.setName("Talend");
        account.setLogin(defaultUserLogin);
        account.setPassword(passwordHashStrategy.hash(defaultUserPassword));
        account.setAccountType(SUDO);
        account.setVendor(vendor);
        entityManager.persist(account);
    }
}
