package com.asalkartechworks.daddysodahouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DaddySodaHouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaddySodaHouseApplication.class, args);
    }
}
