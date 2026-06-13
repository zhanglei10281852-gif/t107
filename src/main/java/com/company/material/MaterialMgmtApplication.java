package com.company.material;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MaterialMgmtApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaterialMgmtApplication.class, args);
    }
}
