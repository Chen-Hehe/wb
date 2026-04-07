package com.weibo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Weibo Backend Application
 */
@SpringBootApplication
@MapperScan("com.weibo.mapper")
@EnableScheduling
public class WeiboApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WeiboApplication.class, args);
    }
}
