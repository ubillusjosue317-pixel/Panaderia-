package com.utp.panaderia.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Las fotos que sube el administrador se guardan en una carpeta fuera de src (por defecto ./uploads)
// y se muestran en /uploads/...
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir}")
    private String carpetaUploads;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Paths.get(carpetaUploads).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(ubicacion);
    }
}
