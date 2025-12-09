package com.tuba.schedulercore.executor;


import com.tuba.schedulercore.model.TaskContext;

public interface TaskExecutor {
    void execute(TaskContext context);
}