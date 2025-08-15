/*
 * Copyright (C) 2024 tobyang
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tobyang.easyeventbus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * EasyEventBus配置属性测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EasyEventBusPropertiesTest.TestConfiguration.class)
@TestPropertySource(properties = {
    "easyeventbus.enable=true",
    "easyeventbus.identifier=test-bus",
    "easyeventbus.async-enabled=true",
    "easyeventbus.async-thread-pool-size=8",
    "easyeventbus.delayed.enabled=true",
    "easyeventbus.delayed.core-pool-size=4",
    "easyeventbus.delayed.thread-name-prefix=TestDelayed-"
})
public class EasyEventBusPropertiesTest {

    @Autowired
    private EasyEventBusProperties properties;

    @Test
    public void testBasicProperties() {
        assertTrue("应该启用EasyEventBus", properties.isEnable());
        assertEquals("标识符应该正确", "test-bus", properties.getIdentifier());
        assertTrue("应该启用异步处理", properties.isAsyncEnabled());
        assertEquals("异步线程池大小应该正确", 8, properties.getAsyncThreadPoolSize());
    }

    @Test
    public void testDelayedEventProperties() {
        EasyEventBusProperties.DelayedEvent delayed = properties.getDelayed();
        assertNotNull("延迟事件配置不应该为null", delayed);
        
        assertTrue("应该启用延迟事件", delayed.isEnabled());
        assertEquals("核心线程池大小应该正确", 4, delayed.getCorePoolSize());
        assertEquals("线程名称前缀应该正确", "TestDelayed-", delayed.getThreadNamePrefix());
    }

    @Test
    public void testDefaultDelayedEventProperties() {
        EasyEventBusProperties defaultProperties = new EasyEventBusProperties();
        EasyEventBusProperties.DelayedEvent delayed = defaultProperties.getDelayed();
        
        assertNotNull("默认延迟事件配置不应该为null", delayed);
        assertTrue("默认应该启用延迟事件", delayed.isEnabled());
        assertEquals("默认核心线程池大小", 2, delayed.getCorePoolSize());
        assertEquals("默认线程名称前缀", "DelayedEvent-", delayed.getThreadNamePrefix());
    }

    @EnableConfigurationProperties(EasyEventBusProperties.class)
    static class TestConfiguration {
        // 测试配置类
    }
}
