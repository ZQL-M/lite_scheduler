package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.annotation.ScheduledTask;
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

    @Autowired
    public TaskRegistrar(TaskRegistry taskRegistry, Scheduler scheduler) {
        this.taskRegistry = taskRegistry;
        this.scheduler = scheduler;
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
                    ScheduledTask ann = AnnotationUtils.findAnnotation(m, ScheduledTask.class);
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
    private void registerMethod(Object bean, Method method, ScheduledTask ann, Class<?> targetClass) {
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
            def.setPersistent(false); // 注解任务默认非持久化
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
    private void parseTimeConfiguration(ScheduledTask ann, TaskDefinition def) {
        // 优先级：cron > fixedRate/fixedDelay > seconds/minutes/hours/days

        // 1. 检查 cron 表达式
        String cron = ann.cron().trim();
        if (StringUtils.hasText(cron)) {
            def.setCron(cron);
            log.debug("Task {} uses cron expression: {}", def.getName(), cron);
            return;
        }

        // 2. 检查 fixedRate
        long fixedRate = ann.fixedRate();
        if (fixedRate > 0) {
            def.setFixedRate(fixedRate);
            log.debug("Task {} uses fixed rate: {}ms", def.getName(), fixedRate);
            return;
        }

        // 3. 检查 fixedDelay
        long fixedDelay = ann.fixedDelay();
        if (fixedDelay > 0) {
            def.setFixedDelay(fixedDelay);
            log.debug("Task {} uses fixed delay: {}ms", def.getName(), fixedDelay);
            return;
        }

        // 4. 检查简化时间配置（seconds/minutes/hours/days）
        String seconds = ann.seconds().trim();
        String minutes = ann.minutes().trim();
        String hours = ann.hours().trim();
        String days = ann.days().trim();

        // 生成 cron 表达式
        String generatedCron = generateCronExpression(seconds, minutes, hours, days);
        if (StringUtils.hasText(generatedCron)) {
            def.setCron(generatedCron);
            log.debug("Task {} uses generated cron expression: {}", def.getName(), generatedCron);
            return;
        }

        log.error("Task {} has no valid time configuration", def.getName());
    }

    /**
     * 根据简化的时间配置生成 Cron 表达式
     * 
     * @param seconds 秒级配置
     * @param minutes 分钟级配置
     * @param hours   小时级配置
     * @param days    天级配置
     * @return Cron 表达式
     */
    private String generateCronExpression(String seconds, String minutes, String hours, String days) {
        // 默认值：每秒执行
        String sec = StringUtils.hasText(seconds) ? seconds : "*";
        String min = StringUtils.hasText(minutes) ? minutes : "*";
        String hour = StringUtils.hasText(hours) ? hours : "*";
        String day = StringUtils.hasText(days) ? days : "*";

        // 如果是简单的数字，表示间隔执行
        if (isSimpleNumber(seconds)) {
            // 每 N 秒执行一次
            sec = "*/" + seconds;
        }
        if (isSimpleNumber(minutes)) {
            // 每 N 分钟执行一次
            min = "*/" + minutes;
        }
        if (isSimpleNumber(hours)) {
            // 每 N 小时执行一次
            hour = "*/" + hours;
        }
        if (isSimpleNumber(days)) {
            // 每 N 天执行一次
            day = "*/" + days;
        }

        // 生成 Cron 表达式：秒 分 时 日 月 周 年（年可选）
        return String.format("%s %s %s %s * ?", sec, min, hour, day);
    }

    /**
     * 判断字符串是否是简单数字
     * 
     * @param str 字符串
     * @return 是否是简单数字
     */
    private boolean isSimpleNumber(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }
        // 检查是否只包含数字
        return str.matches("^\\d+$");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}