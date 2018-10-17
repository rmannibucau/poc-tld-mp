package org.talend.sdk.component.marketplace.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.talend.sdk.component.marketplace.front.model.DownloadModel;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(callSuper = true)
@Table(name = "downloads")
@View(DownloadModel.class)
public class Download extends AuditedEntity {
    @ManyToOne(optional = false)
    private Component component;

    @Column(nullable = false)
    private String componentVersion;
}
