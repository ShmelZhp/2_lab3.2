package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DemoApplication.class);
    }

    @Bean
    public ServletRegistrationBean<FilesServlet> filesServlet() {
        return new ServletRegistrationBean<>(new FilesServlet(), "/files");
    }

    @Bean
    public ServletRegistrationBean<LoginServlet> loginServlet() {
        return new ServletRegistrationBean<>(new LoginServlet(), "/login");
    }

    @Bean
    public ServletRegistrationBean<RegisterServlet> registerServlet() {
        return new ServletRegistrationBean<>(new RegisterServlet(), "/register");
    }

    @Bean
    public ServletRegistrationBean<LogoutServlet> logoutServlet() {
        return new ServletRegistrationBean<>(new LogoutServlet(), "/logout");
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}