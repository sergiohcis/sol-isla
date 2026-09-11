package com.hosannasolutions.solisla.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded product images straight off disk instead of through a controller/DB round
 *  trip — {@code sol-isla.storage.local-path} is the same root {@code LocalFileStorageService}
 *  writes to. Publicly readable: product images have no confidentiality requirement (unlike
 *  SweetHome's tenant/lease documents), so this is intentionally outside the admin-auth surface. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String storageLocation;

    public WebConfig(@Value("${sol-isla.storage.local-path}") String localPath) {
        this.storageLocation = "file:" + Path.of(localPath).toAbsolutePath().normalize() + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**").addResourceLocations(storageLocation);
    }
}
