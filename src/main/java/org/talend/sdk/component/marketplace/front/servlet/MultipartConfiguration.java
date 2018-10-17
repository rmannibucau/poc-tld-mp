package org.talend.sdk.component.marketplace.front.servlet;

import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class MultipartConfiguration implements ServletContainerInitializer {
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) {
        final Config config = ConfigProvider.getConfig();
        ctx.setAttribute(Config.class.getName(), config);

        if (config.getOptionalValue("talend.marketplace.servlet.upload.addServlet", Boolean.class).orElse(true)) {
            // meecrowave uses a filter which enables us to set the multipart setup through a servlet like that
            final ServletRegistration defaultServlet = ctx.getServletRegistration("default");
            final ServletRegistration.Dynamic jaxrs = ctx.addServlet("jaxrs", defaultServlet.getClassName());
            jaxrs.addMapping("/api/component/download/*");
            jaxrs.setMultipartConfig(new MultipartConfigElement(
                    config.getOptionalValue("talend.marketplace.servlet.upload.location", String.class)
                          .orElse(null), // servlet temp dir by default
                    config.getOptionalValue("talend.marketplace.servlet.upload.maxFileSize", Integer.class)
                          .orElse(-1),
                    config.getOptionalValue("talend.marketplace.servlet.upload.maxRequestSize", Integer.class)
                          .orElse(-1),
                    config.getOptionalValue("talend.marketplace.servlet.upload.fileSizeThreshold", Integer.class)
                          .orElse(0)));
        }
    }
}
