package com.tuba.schedulercore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 执行器配置类
 * 启用 ExecutorProperties 配置属性绑定
 */
@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorConfiguration {
    // @EnableConfigurationProperties 会自动创建 ExecutorProperties Bean
}

