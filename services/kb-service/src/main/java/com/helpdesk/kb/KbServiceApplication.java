package com.helpdesk.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbServiceApplication.class, args);
    }
}
