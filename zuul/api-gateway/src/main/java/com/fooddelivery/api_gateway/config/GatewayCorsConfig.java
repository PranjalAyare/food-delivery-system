package com.fooddelivery.api_gateway.config;



import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.CorsFilter;



import java.util.Arrays;



@Configuration

public class GatewayCorsConfig {



@Bean

public CorsFilter corsFilter() {

CorsConfiguration config = new CorsConfiguration();



config.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // ✅ only one allowed origin

config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin"));

config.setAllowCredentials(true); // ✅ required for cookies or Authorization header



UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

source.registerCorsConfiguration("/**", config);



return new CorsFilter(source);

}

}
