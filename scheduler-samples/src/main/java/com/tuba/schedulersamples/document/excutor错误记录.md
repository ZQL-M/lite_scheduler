2026-03-06 16:11:54.632 ERROR 14200 --- [ task-scanner-1] c.t.s.core.scanner.TaskScanner           : 扫描任务时发生异常

org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): com.tuba.schedulercore.mapper.TaskTriggerMapper.selectByTaskId
	at org.apache.ibatis.binding.MapperMethod$SqlCommand.<init>(MapperMethod.java:235) ~[mybatis-3.5.10.jar:3.5.10]
	at com.baomidou.mybatisplus.core.override.MybatisMapperMethod.<init>(MybatisMapperMethod.java:50) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.lambda$cachedInvoker$0(MybatisMapperProxy.java:111) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1708) ~[na:na]
	at com.baomidou.mybatisplus.core.toolkit.CollectionUtils.computeIfAbsent(CollectionUtils.java:115) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.cachedInvoker(MybatisMapperProxy.java:98) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.invoke(MybatisMapperProxy.java:89) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at jdk.proxy2/jdk.proxy2.$Proxy55.selectByTaskId(Unknown Source) ~[na:na]
	at com.tuba.schedulercore.service.impl.TaskTriggerServiceImpl.findByTaskId(TaskTriggerServiceImpl.java:32) ~[classes/:na]
	at com.tuba.schedulercore.core.scheduler.ThreadPoolScheduler.schedule(ThreadPoolScheduler.java:102) ~[classes/:na]
	at com.tuba.schedulercore.core.scanner.TaskScanner.scanTasks(TaskScanner.java:92) ~[classes/:na]
	at org.springframework.scheduling.support.DelegatingErrorHandlingRunnable.run(DelegatingErrorHandlingRunnable.java:54) ~[spring-context-5.3.23.jar:5.3.23]
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) ~[na:na]
	at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java:305) ~[na:na]
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:840) ~[na:na]

2026-03-06 16:11:55.078  INFO 14200 --- [           main] c.t.s.excutor.TaskTestDemo               : --- 测试 2: 任务执行超时控制 ---
2026-03-06 16:11:55.079  INFO 14200 --- [           main] c.t.s.s.impl.TaskSchedulerServiceImpl    : 任务持久化配置: taskName=Timeout Control Test, persistence.type=database, isDatabase=true, persistent=true
2026-03-06 16:11:55.079  INFO 14200 --- [           main] c.t.s.s.impl.TaskSchedulerServiceImpl    : 开始持久化任务到数据库: taskId=test-timeout-control, taskName=Timeout Control Test
2026-03-06 16:11:55.084 ERROR 14200 --- [           main] c.t.s.excutor.TaskTestDemo               : ❌ 超时控制测试失败

org.springframework.jdbc.BadSqlGrammarException:
### Error updating database.  Cause: java.sql.SQLSyntaxErrorException: Unknown column 'timeout' in 'field list'
### The error may exist in com/tuba/schedulercore/mapper/TaskDefinitionMapper.java (best guess)
### The error may involve com.tuba.schedulercore.mapper.TaskDefinitionMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO task_definition  ( id, name, group_name, job_class,  async, enabled, persistent, deleted,   timeout, retry_count, retry_interval )  VALUES  ( ?, ?, ?, ?,  ?, ?, ?, ?,   ?, ?, ? )
### Cause: java.sql.SQLSyntaxErrorException: Unknown column 'timeout' in 'field list'
; bad SQL grammar []; nested exception is java.sql.SQLSyntaxErrorException: Unknown column 'timeout' in 'field list'
	at org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator.doTranslate(SQLErrorCodeSQLExceptionTranslator.java:239) ~[spring-jdbc-5.3.23.jar:5.3.23]
	at org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator.translate(AbstractFallbackSQLExceptionTranslator.java:70) ~[spring-jdbc-5.3.23.jar:5.3.23]
	at org.mybatis.spring.MyBatisExceptionTranslator.translateExceptionIfPossible(MyBatisExceptionTranslator.java:91) ~[mybatis-spring-2.0.7.jar:2.0.7]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:441) ~[mybatis-spring-2.0.7.jar:2.0.7]
	at jdk.proxy2/jdk.proxy2.$Proxy52.insert(Unknown Source) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate.insert(SqlSessionTemplate.java:272) ~[mybatis-spring-2.0.7.jar:2.0.7]
	at com.baomidou.mybatisplus.core.override.MybatisMapperMethod.execute(MybatisMapperMethod.java:59) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy$PlainMethodInvoker.invoke(MybatisMapperProxy.java:148) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.invoke(MybatisMapperProxy.java:89) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at jdk.proxy2/jdk.proxy2.$Proxy54.insert(Unknown Source) ~[na:na]
	at com.tuba.schedulercore.core.persistence.impl.DatabaseTaskPersistenceServiceImpl.save(DatabaseTaskPersistenceServiceImpl.java:23) ~[classes/:na]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl.persistTask(TaskSchedulerServiceImpl.java:511) ~[classes/:na]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl.registerTaskCore(TaskSchedulerServiceImpl.java:443) ~[classes/:na]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl.registerTask(TaskSchedulerServiceImpl.java:111) ~[classes/:na]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl.registerTask(TaskSchedulerServiceImpl.java:658) ~[classes/:na]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl$$FastClassBySpringCGLIB$$5f7ffddf.invoke(<generated>) ~[classes/:na]
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218) ~[spring-core-5.3.23.jar:5.3.23]
	at org.springframework.aop.framework.CglibAopProxy.invokeMethod(CglibAopProxy.java:386) ~[spring-aop-5.3.23.jar:5.3.23]
	at org.springframework.aop.framework.CglibAopProxy.access$000(CglibAopProxy.java:85) ~[spring-aop-5.3.23.jar:5.3.23]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:704) ~[spring-aop-5.3.23.jar:5.3.23]
	at com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl$$EnhancerBySpringCGLIB$$af12c530.registerTask(<generated>) ~[classes/:na]
	at com.tuba.schedulersamples.excutor.TaskTestDemo.testTimeoutControl(TaskTestDemo.java:109) ~[classes/:na]
	at com.tuba.schedulersamples.excutor.TaskTestDemo.run(TaskTestDemo.java:53) ~[classes/:na]
	at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:773) ~[spring-boot-2.6.13.jar:2.6.13]
	at org.springframework.boot.SpringApplication.callRunners(SpringApplication.java:763) ~[spring-boot-2.6.13.jar:2.6.13]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:314) ~[spring-boot-2.6.13.jar:2.6.13]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1317) ~[spring-boot-2.6.13.jar:2.6.13]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1306) ~[spring-boot-2.6.13.jar:2.6.13]
	at com.tuba.schedulersamples.SchedulerSamplesApplication.main(SchedulerSamplesApplication.java:10) ~[classes/:na]
Caused by: java.sql.SQLSyntaxErrorException: Unknown column 'timeout' in 'field list'
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:120) ~[mysql-connector-j-8.0.31.jar:8.0.31]
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:122) ~[mysql-connector-j-8.0.31.jar:8.0.31]
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeInternal(ClientPreparedStatement.java:916) ~[mysql-connector-j-8.0.31.jar:8.0.31]
	at com.mysql.cj.jdbc.ClientPreparedStatement.execute(ClientPreparedStatement.java:354) ~[mysql-connector-j-8.0.31.jar:8.0.31]
	at com.zaxxer.hikari.pool.ProxyPreparedStatement.execute(ProxyPreparedStatement.java:44) ~[HikariCP-4.0.3.jar:na]
	at com.zaxxer.hikari.pool.HikariProxyPreparedStatement.execute(HikariProxyPreparedStatement.java) ~[HikariCP-4.0.3.jar:na]
	at org.apache.ibatis.executor.statement.PreparedStatementHandler.update(PreparedStatementHandler.java:47) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.executor.statement.RoutingStatementHandler.update(RoutingStatementHandler.java:74) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.executor.SimpleExecutor.doUpdate(SimpleExecutor.java:50) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.executor.BaseExecutor.update(BaseExecutor.java:117) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.executor.CachingExecutor.update(CachingExecutor.java:76) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.update(DefaultSqlSession.java:194) ~[mybatis-3.5.10.jar:3.5.10]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.insert(DefaultSqlSession.java:181) ~[mybatis-3.5.10.jar:3.5.10]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:569) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:427) ~[mybatis-spring-2.0.7.jar:2.0.7]
	... 25 common frames omitted

2026-03-06 16:11:59.614 ERROR 14200 --- [ task-scanner-1] c.t.s.core.scanner.TaskScanner           : 扫描任务时发生异常

org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): com.tuba.schedulercore.mapper.TaskTriggerMapper.selectByTaskId
	at org.apache.ibatis.binding.MapperMethod$SqlCommand.<init>(MapperMethod.java:235) ~[mybatis-3.5.10.jar:3.5.10]
	at com.baomidou.mybatisplus.core.override.MybatisMapperMethod.<init>(MybatisMapperMethod.java:50) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.lambda$cachedInvoker$0(MybatisMapperProxy.java:111) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1708) ~[na:na]
	at com.baomidou.mybatisplus.core.toolkit.CollectionUtils.computeIfAbsent(CollectionUtils.java:115) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.cachedInvoker(MybatisMapperProxy.java:98) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.invoke(MybatisMapperProxy.java:89) ~[mybatis-plus-core-3.5.3.1.jar:3.5.3.1]
	at jdk.proxy2/jdk.proxy2.$Proxy55.selectByTaskId(Unknown Source) ~[na:na]
	at com.tuba.schedulercore.service.impl.TaskTriggerServiceImpl.findByTaskId(TaskTriggerServiceImpl.java:32) ~[classes/:na]
	at com.tuba.schedulercore.core.scheduler.ThreadPoolScheduler.schedule(ThreadPoolScheduler.java:102) ~[classes/:na]
	at com.tuba.schedulercore.core.scanner.TaskScanner.scanTasks(TaskScanner.java:92) ~[classes/:na]
	at org.springframework.scheduling.support.DelegatingErrorHandlingRunnable.run(DelegatingErrorHandlingRunnable.java:54) ~[spring-context-5.3.23.jar:5.3.23]
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) ~[na:na]
	at java.base/java.util.concurrent.FutureTask.runAndReset(FutureTask.java:305) ~[na:na]
	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:305) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) ~[na:na]
	at java.base/java.lang.Thread.run(Thread.java:840) ~[na:na]

2026-03-06 16:12:00.098  INFO 14200 --- [           main] c.t.s.excutor.TaskTestDemo               : --- 测试 3: 任务失败重试机制 ---
2026-03-06 16:12:00.099  INFO 14200 --- [           main] c.t.s.s.impl.TaskSchedulerServiceImpl    : 任务持久化配置: taskName=Retry Mechanism Test, persistence.type=database, isDatabase=true, persistent=true
2026-03-06 16:12:00.099  INFO 14200 --- [           main] c.t.s.s.impl.TaskSchedulerServiceImpl    : 开始持久化任务到数据库: taskId=test-retry-mechanism, taskName=Retry Mechanism Test
2026-03-06 16:12:00.102 ERROR 14200 --- [           main] c.t.s.excutor.TaskTestDemo               : ❌ 重试机制测试失败

org.springframework.jdbc.BadSqlGrammarException:
### Error updating database.  Cause: java.sql.SQLSyntaxErrorException: Unknown column 'timeout' in 'field list'