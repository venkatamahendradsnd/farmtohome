package com.farmtohome.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path stableUploadDir = Paths.get(System.getProperty("user.home"), "farmtohome", "uploads");
        String stableUploadPath = stableUploadDir.toAbsolutePath().toUri().toString();
        String legacyUploadPath = Paths.get("./uploads").toAbsolutePath().toUri().toString();
        String projectUploadPath = Paths.get("farm2", "farmtohome", "uploads").toAbsolutePath().toUri().toString();
        Path staticDir = Paths.get("src/main/resources/static");
        String staticPath = staticDir.toAbsolutePath().toUri().toString();

        // Fallback to source static files when classpath static resources are stale in local IDE runs.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", staticPath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(stableUploadPath, legacyUploadPath, projectUploadPath);
    }
}
