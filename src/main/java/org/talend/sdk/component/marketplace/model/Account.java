package org.talend.sdk.component.marketplace.model;

import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;

import org.talend.sdk.component.marketplace.front.model.AccountModel;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@View(AccountModel.class)
@NamedQueries({
        @NamedQuery(name = "Account.findByLogin", query = "select a from Account a where LOWER(a.login)=LOWER(:login)"),
        @NamedQuery(name = "Account.count", query = "select count(c) from Account c"),
        @NamedQuery(name = "Account.findAll", query = "select c from Account c order by c.name")
})
public class Account extends AuditedEntity {
    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String password;

    @Enumerated(STRING)
    private AccountType accountType;

    @ManyToOne(optional = false)
    private Vendor vendor;

    @PrePersist
    public void ensureType() {
        if (accountType == null) {
            setAccountType(AccountType.USER);
        }
    }

    public enum AccountType {
        SUDO,
        MACHINE,
        USER
    }
}
