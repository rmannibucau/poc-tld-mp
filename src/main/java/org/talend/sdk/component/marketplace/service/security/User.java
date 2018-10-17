package org.talend.sdk.component.marketplace.service.security;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

import lombok.Data;

@Data
@RequestScoped
public class User {
    @Inject
    @Claim(standard = Claims.groups)
    private Set<String> groups;
}
