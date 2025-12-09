package com.tuba.schedulercore.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskDefinition 测试类
 * 验证 enabled 字段和 equals/hashCode 方法
 */
class TaskDefinitionTest {

    @Test
    void testEnabledField() {
        TaskDefinition def = new TaskDefinition();
        // 默认应该启用
        assertTrue(def.isEnabled(), "TaskDefinition should be enabled by default");

        def.setEnabled(false);
        assertFalse(def.isEnabled(), "TaskDefinition should be disabled after setEnabled(false)");

        def.setEnabled(true);
        assertTrue(def.isEnabled(), "TaskDefinition should be enabled after setEnabled(true)");
    }

    @Test
    void testEqualsAndHashCode() {
        TaskDefinition def1 = new TaskDefinition();
        def1.setId("task-1");
        def1.setName("Test Task 1");

        TaskDefinition def2 = new TaskDefinition();
        def2.setId("task-1"); // 相同的 ID
        def2.setName("Test Task 2"); // 不同的名称

        TaskDefinition def3 = new TaskDefinition();
        def3.setId("task-2"); // 不同的 ID
        def3.setName("Test Task 1"); // 相同的名称

        // equals 应该基于 id
        assertEquals(def1, def2, "Two TaskDefinitions with same id should be equal");
        assertNotEquals(def1, def3, "Two TaskDefinitions with different id should not be equal");

        // hashCode 应该基于 id
        assertEquals(def1.hashCode(), def2.hashCode(),
                "Two TaskDefinitions with same id should have same hashCode");
        assertNotEquals(def1.hashCode(), def3.hashCode(),
                "Two TaskDefinitions with different id should have different hashCode");

        // 测试 Set 行为
        Set<TaskDefinition> taskSet = new HashSet<>();
        taskSet.add(def1);
        assertTrue(taskSet.contains(def2), "Set should contain TaskDefinition with same id");
        assertFalse(taskSet.contains(def3), "Set should not contain TaskDefinition with different id");
    }

    @Test
    void testEqualsWithNull() {
        TaskDefinition def = new TaskDefinition();
        def.setId("task-1");

        assertNotEquals(def, null, "TaskDefinition should not be equal to null");
    }

    @Test
    void testEqualsWithSameInstance() {
        TaskDefinition def = new TaskDefinition();
        def.setId("task-1");

        assertEquals(def, def, "TaskDefinition should be equal to itself");
    }
}
