# EasyEventBus

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x%2F3.x-green.svg)](https://spring.io/projects/spring-boot)

> 🚀 **基于Google Guava EventBus的事件总线，提供完整的失败处理、重试机制和监控能力** 

## ✨ 核心特性

### 🎯 三阶段事件处理模式
- **幂等性检查** (`@Idempotent`) - 防止重复处理
- **智能重试** (`@FailRetry`) - 自动重试失败的事件
- **失败处理** (`@FailSubscribe`) - 优雅处理最终失败

### 🔧 企业级功能
- **延迟事件** - 支持延迟发布事件到未来时间点
- **全局拦截器** - 提供事件处理的监控和审计能力
- **Spring Boot集成** - 零配置自动装配
- **性能监控** - 内置性能统计和监控

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.tobyang</groupId>
    <artifactId>easyeventbus-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基础配置

```yaml
easyeventbus:
  enable: true
  async-enabled: true
  async-thread-pool-size: 10
  delayed:
    enabled: true
    core-pool-size: 2
```

### 事件监听器

```java
@Component
public class OrderEventListener {

    // 阶段1：幂等性检查
    @Idempotent
    public boolean checkOrderNotProcessed(OrderCreatedEvent event) {
        return !orderRepository.exists(event.getOrderId());
    }

    // 阶段2：业务处理（支持重试）
    @Subscribe
    @FailRetry(retries = 3, interval = 5, timeUnit = TimeUnit.SECONDS)
    public void handleOrderCreated(OrderCreatedEvent event) {
        orderService.processOrder(event);
    }

    // 阶段3：失败处理
    @FailSubscribe
    public void handleOrderFailure(OrderCreatedEvent event, FailureContext context) {
        alertService.sendAlert("订单处理失败", context);
        deadLetterQueue.send(event);
    }
}
```

## 🎨 高级特性

### 全局拦截器 & 失败上下文

```java
@Component
public class EventMonitorInterceptor implements EventInterceptor {
    @Override
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        metricsCollector.recordFailure(event.getClass(), context.getDuration());
    }
}

@FailSubscribe
public void handleFailure(PaymentEvent event, FailureContext context) {
    switch (context.getFailureType()) {
        case RETRY_EXHAUSTED -> deadLetterQueue.send(event, context);
        case PROCESSING_EXCEPTION -> errorLogger.log(event, context.getFailureCause());
        case SYSTEM_EXCEPTION -> alertService.sendAlert(context);
    }
}
```

## 🏗️ 核心架构

### 三阶段处理模式
```
事件发布 → 幂等性检查 → 业务处理 → 失败处理
  post()   @Idempotent   @Subscribe  @FailSubscribe
                           ↓
                      自动重试(@FailRetry)
```

### 解决的关键问题
| 问题 | 原始Guava实现 | EasyEventBus |
|------|-----------|-------------|
| **可靠性** | 失败后无法恢复   | 智能重试 + 失败处理 |
| **重复处理** | 缺乏幂等性保证   | 内置幂等性检查 |
| **可观测性** | 缺乏监控能力    | 全局拦截器 + 性能统计 |
| **异步复杂性** | 手动管理线程池   | 开箱即用 + 延迟事件 |


## 📚 示例项目

完整示例：`easyeventbus-demo/springboot-demo`

## 📄 许可证

Apache License 2.0

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给个 Star 支持！**

[🐛 Issues](../../issues) · [💡 Discussions](../../discussions) · [📖 Wiki](../../wiki)

</div>
