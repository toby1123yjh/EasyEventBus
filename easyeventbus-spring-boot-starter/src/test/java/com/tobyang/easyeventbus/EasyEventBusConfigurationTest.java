package com.tobyang.easyeventbus;

import com.tobyang.easyeventbus.guava.AsyncEventBus;
import com.tobyang.easyeventbus.guava.EventBus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 * EasyEventBusConfigurationTest - Test cases for the EasyEventBusConfiguration class
 * 
 * This class tests the auto-configuration of EasyEventBus in a Spring Boot application.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EasyEventBusConfigurationTest.class)
@EnableAutoConfiguration
@TestPropertySource(properties = {
    "easyeventbus.enable=true",
    "easyeventbus.identifier=testEventBus"
})
public class EasyEventBusConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void testEventBusAutoconfiguration() {
        // Verify that the EventBus bean is created
        EventBus eventBus = context.getBean(EventBus.class);
        assertNotNull("EventBus bean should be created", eventBus);
        assertEquals("EventBus identifier should match configuration", "testEventBus", eventBus.identifier());

        // Verify it's a synchronous EventBus (not AsyncEventBus)
        assertFalse("Should be synchronous EventBus", eventBus instanceof AsyncEventBus);
    }

    /**
     * Test class for async EventBus configuration
     */
    @RunWith(SpringRunner.class)
    @SpringBootTest(classes = AsyncEventBusConfigurationTest.class)
    @EnableAutoConfiguration
    @TestPropertySource(properties = {
        "easyeventbus.enable=true",
        "easyeventbus.identifier=asyncTestEventBus",
        "easyeventbus.asyncEnabled=true",
        "easyeventbus.asyncThreadPoolSize=5"
    })
    public static class AsyncEventBusConfigurationTest {

        @Autowired
        private ApplicationContext context;

        @Test
        public void testAsyncEventBusAutoconfiguration() {
            // Verify that the EventBus bean is created
            EventBus eventBus = context.getBean(EventBus.class);
            assertNotNull("EventBus bean should be created", eventBus);
            assertEquals("EventBus identifier should match configuration", "asyncTestEventBus", eventBus.identifier());

            // Verify it's an AsyncEventBus
            assertTrue("Should be AsyncEventBus", eventBus instanceof AsyncEventBus);
        }
    }
}