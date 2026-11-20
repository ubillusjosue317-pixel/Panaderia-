package com.utp.panaderia.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Con el perfil "https" la app escucha en https://localhost:8443 y, ademas, deja abierto el puerto
// http://localhost:8080 SOLO para redirigir a https (Spring Security hace la redireccion 8080 -> 8443).
// Asi quien escriba la direccion vieja con http termina siempre en una conexion cifrada.
@Configuration
@Profile("https")
public class HttpsConfig {

    @Value("${app.http.puerto-redireccion:8080}")
    private int puertoHttp;

    @Value("${server.port:8443}")
    private int puertoHttps;

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> redirigirHttpAHttps() {
        return fabrica -> {
            Connector http = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            http.setScheme("http");
            http.setSecure(false);
            http.setPort(puertoHttp);
            http.setRedirectPort(puertoHttps);
            fabrica.addAdditionalTomcatConnectors(http);
        };
    }
}
