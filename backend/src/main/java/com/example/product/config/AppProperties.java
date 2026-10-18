package com.example.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用自定义配置（对应 application.yml 里的 app.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Search search = new Search();

    @Data
    public static class Jwt {
        /** HS256 密钥，长度需 >= 32 字节 */
        private String secret = "product-admin-jwt-secret-key-2024-please-change-in-production";
        /** token 有效期（秒） */
        private long expireSeconds = 7200L;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Data
    public static class Search {
        private String productIndex = "product_index";
        private int pageSize = 10;
        /** 建索引分词器；未安装 IK 插件时自动回退 standard */
        private String analyzer = "ik_max_word";
        /** 查询分词器 */
        private String searchAnalyzer = "ik_smart";
    }
}
