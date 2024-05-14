package org.chaz.portal.settings.filter;

import org.chaz.portal.settings.service.SettingService;
import org.commons.constants.Constants;
import org.commons.entity.settings.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Component
public class SettingFilter implements Filter {

    @Autowired
    private SettingService settingService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String url = servletRequest.getRequestURL().toString();

        if (url.endsWith(".css") || url.endsWith(".js") || url.endsWith(".png") ||
                url.endsWith(".jpg")) {
            filterChain.doFilter(request, response);
            return;
        }

        List<Setting> generalSettings = settingService.getGeneralSettings();

        generalSettings.forEach(setting -> {
            request.setAttribute(setting.getKey(), setting.getValue());
        });

        request.setAttribute("S3_BASE_URI", Constants.S3_BASE_URI);

        filterChain.doFilter(request, response);
    }
}
