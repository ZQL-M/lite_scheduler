package com.tuba.schedulercore.core.task;

import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SimpleTubaJobFactory实现，使用反射来创建任务实例
 */
@Component
public class SimpleTubaJobFactory implements TubaJobFactory {

    private static final Logger log = LoggerFactory.getLogger(SimpleTubaJobFactory.class);

    @Override
    public Object newTask(TaskDefinition definition) {
        if (definition == null || definition.getJobClass() == null) {
            log.warn("Task definition or job class is null, cannot create task instance");
            return null;
        }

        try {
            // 使用反射创建任务实例
            Class<?> jobClass = Class.forName(definition.getJobClass());
            Object taskInstance = jobClass.newInstance();
            log.debug("Created task instance for job class: {}", definition.getJobClass());
            return taskInstance;
        } catch (ClassNotFoundException e) {
            log.error("Job class not found: {}", definition.getJobClass(), e);
            throw new RuntimeException("Job class not found: " + definition.getJobClass(), e);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Failed to create task instance for job class: {}", definition.getJobClass(), e);
            throw new RuntimeException("Failed to create task instance for job class: " + definition.getJobClass(), e);
        }
    }
}
