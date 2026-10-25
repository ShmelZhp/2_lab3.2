package com.example.demo;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {
    @Bean
    public ServletRegistrationBean<FilesServlet> filesServlet() {
        return new ServletRegistrationBean<>(new FilesServlet(), "/files");
    }
}