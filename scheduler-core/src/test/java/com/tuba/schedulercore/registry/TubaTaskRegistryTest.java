package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.core.registry.TaskRegistry;
import com.tuba.schedulercore.model.TaskDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskRegistry 测试类
 * 验证 enable/disable 方法
 */
class TubaTaskRegistryTest {

    private TaskRegistry registry;
    private TaskDefinition task1;
    private TaskDefinition task2;

    @BeforeEach
    void setUp() {
        registry = new TaskRegistry();
        
        task1 = new TaskDefinition();
        task1.setId("task-1");
        task1.setName("Task 1");
        task1.setEnabled(true);
        
        task2 = new TaskDefinition();
        task2.setId("task-2");
        task2.setName("Task 2");
        task2.setEnabled(true);
        
        registry.register(task1);
        registry.register(task2);
    }

    @Test
    void testEnable() {
        // 先禁用
        task1.setEnabled(false);
        assertFalse(task1.isEnabled(), "Task should be disabled");
        
        // 启用
        registry.enable("task-1");
        assertTrue(task1.isEnabled(), "Task should be enabled after enable()");
        
        // 启用不存在的任务（应该不抛异常）
        assertDoesNotThrow(() -> registry.enable("nonexistent"), 
            "Enabling nonexistent task should not throw exception");
    }

    @Test
    void testDisable() {
        // 先启用
        assertTrue(task1.isEnabled(), "Task should be enabled initially");
        
        // 禁用
        registry.disable("task-1");
        assertFalse(task1.isEnabled(), "Task should be disabled after disable()");
        
        // 禁用不存在的任务（应该不抛异常）
        assertDoesNotThrow(() -> registry.disable("nonexistent"), 
            "Disabling nonexistent task should not throw exception");
    }

    @Test
    void testEnableDisableMultipleTasks() {
        registry.disable("task-1");
        registry.disable("task-2");
        
        assertFalse(task1.isEnabled(), "Task 1 should be disabled");
        assertFalse(task2.isEnabled(), "Task 2 should be disabled");
        
        registry.enable("task-1");
        assertTrue(task1.isEnabled(), "Task 1 should be enabled");
        assertFalse(task2.isEnabled(), "Task 2 should still be disabled");
    }

    @Test
    void testGetAndContains() {
        TaskDefinition retrieved = registry.get("task-1");
        assertNotNull(retrieved, "Should retrieve registered task");
        assertEquals("task-1", retrieved.getId(), "Retrieved task should have correct id");
        
        assertTrue(registry.contains("task-1"), "Should contain registered task");
        assertFalse(registry.contains("nonexistent"), "Should not contain unregistered task");
    }

    @Test
    void testUnregister() {
        assertTrue(registry.contains("task-1"), "Task should be registered");
        
        registry.unregister("task-1");
        assertFalse(registry.contains("task-1"), "Task should be unregistered");
        assertNull(registry.get("task-1"), "Unregistered task should return null");
    }
}

