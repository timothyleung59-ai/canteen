package com.shopping;

import com.shopping.wx.filter.AdminAuthFilter;
import com.shopping.wx.filter.SecurityFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfiguration {

    /**
     * 后台管理鉴权过滤器, 必须比 SecurityFilter 先执行(order 更小),
     * 用于拦截纯管理端接口, 校验 Admin-Token。
     */
    @Bean
    public FilterRegistrationBean adminFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new AdminAuthFilter());
        registration.addUrlPatterns("/*");
        registration.setName("AdminAuthFilter");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean filterRegistration() {

        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new SecurityFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("paramName", "paramValue");
        registration.setName("MyFilter");
        registration.setOrder(1);
        return registration;
    }

}



