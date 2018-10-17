package org.talend.sdk.component.marketplace.front.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    private String username;

    private String password;

    @JsonbProperty("refresh_token")
    private String refreshToken;

    @JsonbProperty("grant_type")
    private String grantType;
}
