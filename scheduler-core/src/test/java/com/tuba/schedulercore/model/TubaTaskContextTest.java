package com.tuba.schedulercore.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskContext 测试类
 * 验证 putAttribute 和 getAttribute 方法
 */
class TubaTaskContextTest {

    @Test
    void testPutAndGetAttribute() {
        TaskDefinition def = new TaskDefinition();
        def.setId("test-task");
        TaskContext context = new TaskContext(def);

        // 测试基本 put/get
        context.putAttribute("key1", "value1");
        assertEquals("value1", context.getAttribute("key1"),
                "Should get the value that was put");

        // 测试覆盖
        context.putAttribute("key1", "value2");
        assertEquals("value2", context.getAttribute("key1"),
                "Should get the updated value");

        // 测试不存在的 key
        assertNull(context.getAttribute("nonexistent"),
                "Should return null for nonexistent key");
    }

    @Test
    void testGetAttributeWithType() {
        TaskDefinition def = new TaskDefinition();
        def.setId("test-task");
        TaskContext context = new TaskContext(def);

        // 测试 String 类型
        context.putAttribute("str", "hello");
        String str = context.getAttribute("str", String.class);
        assertEquals("hello", str, "Should get String value with type");

        // 测试 Integer 类型
        context.putAttribute("int", 42);
        Integer intVal = context.getAttribute("int", Integer.class);
        assertEquals(42, intVal, "Should get Integer value with type");

        // 测试类型不匹配
        String wrongType = context.getAttribute("int", String.class);
        assertNull(wrongType, "Should return null for type mismatch");

        // 测试不存在的 key
        String missing = context.getAttribute("missing", String.class);
        assertNull(missing, "Should return null for missing key");
    }

    @Test
    void testAttributesMap() {
        TaskDefinition def = new TaskDefinition();
        def.setId("test-task");
        TaskContext context = new TaskContext(def);

        context.putAttribute("key1", "value1");
        context.putAttribute("key2", 100);

        // 验证可以通过 getAttributes() 访问
        assertTrue(context.getAttributes().containsKey("key1"),
                "Attributes map should contain key1");
        assertTrue(context.getAttributes().containsKey("key2"),
                "Attributes map should contain key2");
        assertEquals(2, context.getAttributes().size(),
                "Attributes map should have 2 entries");
    }

    @Test
    void testStartAndEndTime() {
        TaskDefinition def = new TaskDefinition();
        def.setId("test-task");
        TaskContext context = new TaskContext(def);

        Instant start = Instant.now();
        context.setStartTime(start);
        assertEquals(start, context.getStartTime(), "Should set and get start time");

        Instant end = Instant.now();
        context.setEndTime(end);
        assertEquals(end, context.getEndTime(), "Should set and get end time");
    }

    @Test
    void testError() {
        TaskDefinition def = new TaskDefinition();
        def.setId("test-task");
        TaskContext context = new TaskContext(def);

        RuntimeException error = new RuntimeException("Test error");
        context.setError(error);
        assertEquals(error, context.getError(), "Should set and get error");
    }
}
