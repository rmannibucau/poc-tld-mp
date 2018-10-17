package org.talend.sdk.component.marketplace.front;

import javax.enterprise.context.Dependent;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

@Dependent
@ApplicationPath("api")
@LoginConfig(authMethod = "MP-JWT")
public class MarketplaceApplication extends Application {
}
