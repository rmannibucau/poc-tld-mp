package org.talend.sdk.component.marketplace.front.model;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DownloadModel extends BaseModel {
    @View.Schema(type = "file", required = true, position = 1)
    private String file;

    @View.Schema(required = true, reference = ComponentModel.class, position = 2, title = "Component")
    private String componentId;

    @View.Schema(length = 255, required = true, position = 3, title = "Version")
    private String componentVersion;
}
