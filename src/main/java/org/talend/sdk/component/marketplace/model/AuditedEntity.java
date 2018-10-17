package org.talend.sdk.component.marketplace.model;

import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.Version;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// todo: surely add a state = {ACTIVE,INACTIVE}
@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class AuditedEntity {
    @Id
    private String id;

    @Column(length = 2048, nullable = false, unique = true)
    private String name;

    @Temporal(TIMESTAMP)
    private Date created;

    @Temporal(TIMESTAMP)
    private Date updated;

    @Version
    private long version;

    @PrePersist
    public void onPersist() {
        if (id == null) {
            initId();
        }
        created = updated = new Date();
    }

    @PreUpdate
    public void onUpdate() {
        updated = new Date();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return getClass().isInstance(obj) && ((id == null && super.equals(obj)) || (id != null && id.equals(AuditedEntity.class.cast(obj).getId())));
    }

    public void initId() {
        if (id != null) {
            throw new IllegalStateException("id is already set, you can't change it");
        }
        id = UUID.randomUUID().toString().replace("-", "");
    }
}
