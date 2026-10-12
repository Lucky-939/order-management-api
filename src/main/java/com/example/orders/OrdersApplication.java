package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Marks the main class that bootstraps and auto-configures the Spring Boot application
@SpringBootApplication
public class OrdersApplication {

    // Main entry point for the JVM to launch the Spring Boot application
    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
