package org.talend.sdk.component.marketplace.front.model;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VendorModel extends BaseModel {
    @View.Schema(length = 2048, required = true, position = 1)
    private String name;
}
