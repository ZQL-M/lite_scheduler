package com.tuba.schedulercore.core.executor;


import com.tuba.schedulercore.model.TaskContext;

public interface TaskExecutor {
    void execute(TaskContext context);
}