package org.talend.sdk.component.marketplace.front.model;

import java.util.Collection;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ComponentModel extends BaseModel {
    @View.Schema(length = 1024, required = true, position = 1, reference = VendorModel.class)
    private String vendor;

    @View.Schema(length = 2048, required = true, position = 2)
    private String name;

    @View.Schema(length = 2048, required = true, position = 3)
    private String license;

    @View.Schema(length = 2048, required = true, position = 4)
    private String sources;

    @View.Schema(length = 2048, required = true, position = 5)
    private String bugtracker;

    @View.Schema(length = 2048, required = true, position = 6)
    private String documentation;

    @View.Schema(widget = "textarea", length = 8192, required = true, position = 7)
    private String description;

    @View.Schema(widget = "textarea", length = 8192, required = true, position = 8)
    private String changelog;

    @View.Skip
    private VendorModel completeVendor;

    private Collection<ProductModel> products;

    private Collection<DownloadModel> downloads;

    @Data
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Page extends org.talend.sdk.component.marketplace.front.model.Page<ComponentModel> {
    }
}
