package org.talend.sdk.component.marketplace.front.model;

import java.util.Date;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;

@Data
public abstract class BaseModel {
    @View.Skip
    private String id;

    @View.Skip
    private Date created;

    @View.Skip
    private Date updated;

    @View.Schema(type = "hidden", readOnly = true)
    private long version;
}
