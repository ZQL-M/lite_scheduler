package com.tuba.schedulersamples;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.registry.TaskRegistry;
import com.tuba.schedulercore.scheduler.Scheduler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 简单任务控制器
 * 注意：此类已被禁用，功能已被TaskManagementController替代
 */
// @RestController
// @RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRegistry registry;
    private final Scheduler scheduler;

    public TaskController(TaskRegistry registry, Scheduler scheduler) {
        this.registry = registry;
        this.scheduler = scheduler;
    }

    // @GetMapping
    public List<Object> list() {
        return registry.getAll().stream().map(d -> new Object() {
            public String id = d.getId();
            public String name = d.getName();
            public boolean async = d.isAsync();
            public String bean = d.getBean().getClass().getName();
            public String method = d.getMethod().getName();
        }).collect(Collectors.toList());
    }

    // @PostMapping("/{id}/trigger")
    public String trigger(@PathVariable String id) {
        TaskDefinition def = registry.get(id);
        if (def == null) return "task not found: " + id;
        // immediate trigger - create context and execute directly
        scheduler.schedule(def); // 注意：schedule 会再次注册调度。这里我们直接调用 executor 更干净（示例）
        return "scheduled (or triggered): " + id;
    }
}