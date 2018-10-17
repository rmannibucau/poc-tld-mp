package org.talend.sdk.component.marketplace.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tokens", indexes = {
        @Index(name = "tokens_accessToken", columnList = "accessToken"),
        @Index(name = "tokens_refreshToken", columnList = "refreshToken")
})
@NamedQueries({
        @NamedQuery(name = "Token.findByRefreshToken", query = "select t from Token t where t.refreshToken = :refreshToken"),
        @NamedQuery(name = "Token.findByAccessToken", query = "select t from Token t where t.refreshToken = :accessToken"),
        @NamedQuery(name = "Token.deleteExpiredTokens", query = "delete from Token t where t.expiredAt < :until")
})
public class Token {
    @Id
    private String id;

    @Column(unique = true, length = 7168) // big cause information holder
    private String accessToken;

    @Column(unique = true, length = 2048) // smaller cause just a workflow value
    private String refreshToken;

    @Temporal(TIMESTAMP)
    private Date expiredAt;

    @ManyToOne(optional = false)
    private Account account;

    @Version
    private long version;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return Token.class.isInstance(obj) && (super.equals(obj) || id == Token.class.cast(obj).getId());
    }
}
