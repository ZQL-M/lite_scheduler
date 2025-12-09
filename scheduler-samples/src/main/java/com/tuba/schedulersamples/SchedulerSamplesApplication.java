package com.tuba.schedulersamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tuba"})
public class SchedulerSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerSamplesApplication.class, args);
    }

}
