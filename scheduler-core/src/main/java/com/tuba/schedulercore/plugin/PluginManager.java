package com.tuba.schedulercore.plugin;

import java.util.List;

import com.tuba.schedulercore.model.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 插件管理器，负责调用所有注册的插件
 */
@Component
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private final List<TaskPlugin> plugins;

    @Autowired
    public PluginManager(List<TaskPlugin> plugins) {
        this.plugins = plugins;
        log.debug("PluginManager initialized with {} plugins", plugins.size());
    }

    /**
     * 执行所有插件的 before 方法
     * @param ctx 任务上下文
     */
    public void before(TaskContext ctx) {
        for (TaskPlugin p : plugins) {
            try {
                p.before(ctx);
            } catch (Throwable t) {
                // 记录日志但不中断流程
                log.error("Plugin {} failed in before() for task {}", 
                    p.getClass().getName(), 
                    ctx.getDefinition() != null ? ctx.getDefinition().getId() : "unknown", 
                    t);
            }
        }
    }

    /**
     * 执行所有插件的 after 方法
     * @param ctx 任务上下文
     */
    public void after(TaskContext ctx) {
        for (TaskPlugin p : plugins) {
            try {
                p.after(ctx);
            } catch (Throwable t) {
                // 记录日志但不中断流程
                log.error("Plugin {} failed in after() for task {}", 
                    p.getClass().getName(), 
                    ctx.getDefinition() != null ? ctx.getDefinition().getId() : "unknown", 
                    t);
            }
        }
    }

    /**
     * 执行所有插件的 onError 方法
     * @param ctx 任务上下文
     * @param e 异常
     */
    public void onError(TaskContext ctx, Throwable e) {
        for (TaskPlugin p : plugins) {
            try {
                p.onError(ctx, e);
            } catch (Throwable t) {
                // 记录日志但不中断流程
                log.error("Plugin {} failed in onError() for task {}", 
                    p.getClass().getName(), 
                    ctx.getDefinition() != null ? ctx.getDefinition().getId() : "unknown", 
                    t);
            }
        }
    }
}