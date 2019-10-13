package com.template.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
public class Starter {
    /**
     * Starts our Spring Boot application.
     */
    @Autowired
    NodeRPCConnection rpcConnection;
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(NodeRPCConnection rpcConnection){
        ObjectMapper mapper= JacksonSupport.createDefaultMapper(rpcConnection.proxy);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;

    }
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Starter.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebEnvironment(true);
        app.run(args);
    }
}