package com.example.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 商品管理系统启动类。
 */
@EnableAsync
@SpringBootApplication
public class ProductAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductAdminApplication.class, args);
    }
}
