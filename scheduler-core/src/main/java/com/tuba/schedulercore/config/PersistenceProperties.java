package com.tuba.schedulercore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 持久化配置属性
 */
@ConfigurationProperties(prefix = "lite-scheduler.persistence")
public class PersistenceProperties {
    /**
     * 持久化类型：database 或 memory
     */
    private String type = "memory";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}