package com.fenmo.expensetracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String distPath = Path.of(System.getProperty("user.dir"), "dist").toUri().toString();
        registry.addResourceHandler("/assets/**")
            .addResourceLocations(distPath + "assets/");
        registry.addResourceHandler("/favicon.ico", "/vite.svg")
            .addResourceLocations(distPath);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{spring:[^.]+}").setViewName("forward:/");
        registry.addViewController("/**/{spring:[^.]+}").setViewName("forward:/");
    }
}
