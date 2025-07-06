package com.fooddelivery.order_service;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Autowired
    private HttpServletRequest request;

    @Override
    public void apply(RequestTemplate template) {
        // Grab the Authorization header from the incoming request to order-service
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && !authHeader.isEmpty()) {
            // Forward it to restaurant-service call
            template.header("Authorization", authHeader);
        }
    }
}
