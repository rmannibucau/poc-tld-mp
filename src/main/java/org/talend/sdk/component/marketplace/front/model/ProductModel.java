package org.talend.sdk.component.marketplace.front.model;

import java.util.Collection;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ProductModel extends BaseModel {
    @View.Schema(length = 2048, required = true, position = 1)
    private String name;

    @View.Schema(length = 8192, required = true, position = 2)
    private String description;

    private Collection<String> versions;

    @Data
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Page extends org.talend.sdk.component.marketplace.front.model.Page<ProductModel> {
    }
}
