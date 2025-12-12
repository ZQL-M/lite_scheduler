package com.tuba.schedulercore.config;

import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.persistence.impl.DatabaseTaskPersistenceServiceImpl;
import com.tuba.schedulercore.persistence.impl.InMemoryTaskPersistenceServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 持久化配置类
 */
@Configuration
@EnableConfigurationProperties({ ExecutorProperties.class, PersistenceProperties.class })
public class PersistenceConfiguration {

    /**
     * 根据配置选择持久化服务实现
     * 
     * @param persistenceProperties      持久化配置属性
     * @param inMemoryPersistenceService 内存持久化服务实现
     * @param databasePersistenceService 数据库持久化服务实现
     * @return TaskPersistenceService 实现
     */
    @Bean
    @Primary
    public TaskPersistenceService taskPersistenceService(
            PersistenceProperties persistenceProperties,
            InMemoryTaskPersistenceServiceImpl inMemoryPersistenceService,
            DatabaseTaskPersistenceServiceImpl databasePersistenceService) {
        // 根据配置选择持久化实现
        if ("database".equalsIgnoreCase(persistenceProperties.getType())) {
            return databasePersistenceService;
        } else {
            return inMemoryPersistenceService;
        }
    }
}