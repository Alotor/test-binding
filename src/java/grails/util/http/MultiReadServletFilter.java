package grails.util.http;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

public class MultiReadServletFilter implements Filter {
    private static final Set<String> ALLOWED_METHODS = new HashSet<String>(Arrays.asList(new String[]{ "PUT", "POST", "PATCH" }));
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<String>(Arrays.asList(new String[] { "application/json" }));

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;

            if(shouldWrapRequest(request)) {
                filterChain.doFilter(new MultiReadHttpServletRequest(request), servletResponse);
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean shouldWrapRequest(HttpServletRequest request) {
        return ALLOWED_METHODS.contains(request.getMethod()) && ALLOWED_MIME_TYPES.contains(request.getContentType());
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }
}
