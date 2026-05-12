package com.tea.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * TeaRoomManagementApplication 类，承担系统中的基础业务职责。
 */
@SpringBootApplication
@EnableScheduling
public class TeaRoomManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeaRoomManagementApplication.class, args);
    }
}
