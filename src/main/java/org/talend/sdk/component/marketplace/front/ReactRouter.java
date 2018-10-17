package org.talend.sdk.component.marketplace.front;

import java.io.IOException;

import javax.enterprise.context.Dependent;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Dependent
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class ReactRouter implements Filter {
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(request);
        final String uri = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
        if (isIncluded(uri)) {
            chain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
                @Override
                public String getServletPath() {
                    return "/index.html";
                }
            }, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isIncluded(final String uri) {
        return !uri.startsWith("/api") &&
                !uri.startsWith("/main-") &&
                !uri.startsWith("/settings.json") &&
                !uri.startsWith("/openapi.json");
    }
}
