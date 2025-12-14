package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.annotation.TubaTask;
import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 启动时扫描 Spring 容器中带 @ScheduledTask 的方法并注册到调度器。
 * 支持 Spring AOP 代理，使用 AopUtils 获取原始类。
 */
@Component
public class TaskRegistrar implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(TaskRegistrar.class);

    private ApplicationContext applicationContext;
    private final TaskRegistry taskRegistry;
    private final Scheduler scheduler;
    private final com.tuba.schedulercore.config.PersistenceProperties persistenceProperties;

    @Autowired
    public TaskRegistrar(TaskRegistry taskRegistry, Scheduler scheduler,
            com.tuba.schedulercore.config.PersistenceProperties persistenceProperties) {
        this.taskRegistry = taskRegistry;
        this.scheduler = scheduler;
        this.persistenceProperties = persistenceProperties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        // 2. 扫描 Spring 容器中的任务
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int registeredCount = 0;
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                // 使用 AopUtils 获取原始类，处理 Spring AOP 代理问题
                Class<?> targetClass = AopUtils.getTargetClass(bean);
                Method[] methods = targetClass.getDeclaredMethods();
                for (Method m : methods) {
                    TubaTask ann = AnnotationUtils.findAnnotation(m, TubaTask.class);
                    if (ann != null) {
                        registerMethod(bean, m, ann, targetClass);
                        registeredCount++;
                    }
                }
            } catch (Exception e) {
                // 记录日志但继续扫描其他 bean
                log.warn("Failed to scan bean: {}, error: {}", beanName, e.getMessage(), e);
            }
        }
        log.info("Lite-Scheduler: Loaded {} tasks from persistence, registered {} new tasks",
                registeredCount);
        // start scheduler if needed
        scheduler.start();
    }

    /**
     * 注册任务方法
     * 
     * @param bean        Spring bean 实例（可能是代理）
     * @param method      方法对象（从原始类获取）
     * @param ann         注解
     * @param targetClass 原始类（非代理类）
     */
    private void registerMethod(Object bean, Method method, TubaTask ann, Class<?> targetClass) {
        if (bean == null || method == null || ann == null || targetClass == null) {
            log.warn("Cannot register method with null parameters: bean={}, method={}, ann={}, targetClass={}",
                    bean, method, ann, targetClass);
            return;
        }
        try {
            // 验证方法签名：无参或单参数 TaskContext
            int paramCount = method.getParameterCount();
            if (paramCount > 1) {
                log.warn("Skipping method {}: unsupported signature (must have 0 or 1 parameter)", method.getName());
                return;
            }
            if (paramCount == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (!com.tuba.schedulercore.model.TaskContext.class.isAssignableFrom(paramType)) {
                    log.warn("Skipping method {}: parameter must be TaskContext, but got {}",
                            method.getName(), paramType.getName());
                    return;
                }
            }

            String id = UUID.randomUUID().toString();
            String name = StringUtils.hasText(ann.name()) ? ann.name() : method.getName();
            TaskDefinition def = new TaskDefinition();
            def.setId(id);
            def.setName(name);
            def.setGroup(ann.group());
            def.setAsync(ann.async());
            def.setBean(bean);
            // 使用原始类的方法，确保能正确反射调用
            def.setMethod(targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes()));
            def.setBeanName(targetClass.getSimpleName());
            def.setMethodName(method.getName());
            def.setDescription(ann.description());
            def.setEnabled(true); // 默认启用
            // 根据配置自动设置持久化属性，覆盖注解默认值
            boolean isDatabasePersistence = "database".equalsIgnoreCase(persistenceProperties.getType());
            def.setPersistent(isDatabasePersistence || ann.persistent()); // 数据库模式下默认为true，否则使用注解值
            def.setRepeatCount(ann.repeatCount());

            // 解析时间配置
            parseTimeConfiguration(ann, def);

            // register in registry and schedule
            taskRegistry.register(def);
            scheduler.schedule(def);
        } catch (NoSuchMethodException e) {
            log.error("Failed to register method {}: {}", method.getName(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to register method {}: {}", method.getName(), e.getMessage(), e);
        }
    }

    /**
     * 解析时间配置，将注解中的时间属性转换为任务定义的时间配置
     * 
     * @param ann 注解实例
     * @param def 任务定义
     */
    private void parseTimeConfiguration(TubaTask ann, TaskDefinition def) {
        // 优先级：cron > interval+type

        // 1. 检查 cron 表达式
        String cron = ann.cron().trim();
        if (StringUtils.hasText(cron)) {
            def.setCron(cron);
            log.debug("Task {} uses cron expression: {}", def.getName(), cron);
            return;
        }

        // 2. 检查 interval
        long interval = ann.interval();
        if (interval > 0) {
            // 根据时间单位转换为毫秒
            long millisInterval = convertToMillis(interval, ann.type());
            def.setFixedRate(millisInterval);
            log.debug("Task {} uses interval: {} {} ({}ms)", def.getName(), interval, ann.type().name(),
                    millisInterval);
            return;
        }

        log.error("Task {} has no valid time configuration", def.getName());
    }

    /**
     * 将时间间隔转换为毫秒
     * 
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 毫秒数
     */
    private long convertToMillis(long interval, TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return interval * 1000;
            case MINUTES:
                return interval * 1000 * 60;
            case HOURS:
                return interval * 1000 * 60 * 60;
            case DAYS:
                return interval * 1000 * 60 * 60 * 24;
            case MILLISECONDS:
            default:
                return interval;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}