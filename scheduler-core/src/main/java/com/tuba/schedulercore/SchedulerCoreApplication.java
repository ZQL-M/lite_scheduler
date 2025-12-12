package com.tuba.schedulercore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tuba.schedulercore.mapper")
public class SchedulerCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerCoreApplication.class, args);
    }

}
