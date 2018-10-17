package org.talend.sdk.component.marketplace.front.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Data;

@Data
public class TokenModel {
    @JsonbProperty("token_type")
    private String tokenType;

    @JsonbProperty("access_token")
    private String accessToken;

    @JsonbProperty("refresh_token")
    private String refreshToken;

    @JsonbProperty("expires_in")
    private long expiresIn;
}
