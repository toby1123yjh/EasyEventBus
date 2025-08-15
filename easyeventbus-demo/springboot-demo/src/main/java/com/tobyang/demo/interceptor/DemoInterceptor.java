package com.tobyang.demo.interceptor;

import com.tobyang.easyeventbus.guava.EventInterceptor;
import com.tobyang.easyeventbus.guava.InterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 演示拦截器 - 记录事件处理过程
 */
@Component
public class DemoInterceptor implements EventInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DemoInterceptor.class);

    @Override
    public void beforeProcessing(Object event, InterceptorContext context) {
        logger.info("🎯 拦截器 - 开始处理: {}", event.getClass().getSimpleName());
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    @Override
    public void afterProcessingSuccess(Object event, InterceptorContext context) {
        Long startTime = context.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : context.getDuration();
        
        logger.info("🎉 拦截器 - 处理成功: {}, 耗时: {}ms, 重试: {}次, 跳过: {}", 
            event.getClass().getSimpleName(), 
            duration,
            context.getRetryCount(),
            context.isSkipped());
    }

    @Override
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        Long startTime = context.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : context.getDuration();
        
        logger.warn("💥 拦截器 - 处理失败: {}, 耗时: {}ms, 重试: {}次, 异常: {}", 
            event.getClass().getSimpleName(), 
            duration,
            context.getRetryCount(),
            exception.getMessage());
    }

    @Override
    public int getOrder() {
        return 1; // 优先执行
    }
}
