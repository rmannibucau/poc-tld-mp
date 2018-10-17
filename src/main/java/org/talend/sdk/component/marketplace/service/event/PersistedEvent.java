package org.talend.sdk.component.marketplace.service.event;

import org.talend.sdk.component.marketplace.model.AuditedEntity;

import lombok.Data;

@Data
public class PersistedEvent<T extends AuditedEntity> {
    private final T entity;
}
