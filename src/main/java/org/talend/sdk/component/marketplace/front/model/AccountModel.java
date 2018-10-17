package org.talend.sdk.component.marketplace.front.model;

import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AccountModel extends BaseModel {
    @View.Schema(length = 255, required = true, position = 1)
    private String name;

    @View.Schema(length = 255, required = true, position = 2)
    private String login;

    @View.Schema(length = 255, required = true, position = 3)
    private String password;

    @View.Schema(required = true, reference = VendorModel.class, position = 4)
    private String vendor;

    @View.Schema(position = 5)
    private boolean sudo; // admin user, "With great power comes great responsibility"

    @View.Schema(position = 6)
    private boolean machine; // enable access to the "API" but no write permission
}
