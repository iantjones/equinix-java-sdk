package api.equinix.javasdk.fabric;

import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structural tests verifying that the dryRun feature exists on builder classes.
 * Uses reflection to confirm the dryRun field and method are present
 * on {@link ConnectionOperator.ConnectionBuilder} and
 * {@link ServiceTokenOperator.ServiceTokenBuilder}.
 */
class DryRunTest {

    @Test
    void connectionBuilder_hasDryRunField() {
        Class<?>[] innerClasses = ConnectionOperator.class.getDeclaredClasses();
        Class<?> connectionBuilderClass = Arrays.stream(innerClasses)
                .filter(c -> c.getSimpleName().equals("ConnectionBuilder"))
                .findFirst()
                .orElse(null);

        assertNotNull(connectionBuilderClass,
                "ConnectionOperator should have an inner class named ConnectionBuilder");

        boolean hasDryRunField = false;
        for (Field field : connectionBuilderClass.getDeclaredFields()) {
            if (field.getName().equals("dryRun") && field.getType() == boolean.class) {
                hasDryRunField = true;
                break;
            }
        }
        assertTrue(hasDryRunField,
                "ConnectionBuilder should have a 'dryRun' boolean field");
    }

    @Test
    void connectionBuilder_hasDryRunMethod() {
        Class<?>[] innerClasses = ConnectionOperator.class.getDeclaredClasses();
        Class<?> connectionBuilderClass = Arrays.stream(innerClasses)
                .filter(c -> c.getSimpleName().equals("ConnectionBuilder"))
                .findFirst()
                .orElse(null);

        assertNotNull(connectionBuilderClass);

        boolean hasDryRunMethod = false;
        for (Method method : connectionBuilderClass.getDeclaredMethods()) {
            if (method.getName().equals("dryRun") && method.getParameterCount() == 0) {
                hasDryRunMethod = true;
                // Verify the method returns the builder type for fluent chaining
                assertEquals(connectionBuilderClass, method.getReturnType(),
                        "dryRun() should return the ConnectionBuilder for fluent chaining");
                break;
            }
        }
        assertTrue(hasDryRunMethod,
                "ConnectionBuilder should have a 'dryRun()' method");
    }

    @Test
    void serviceTokenBuilder_hasDryRunField() {
        Class<?>[] innerClasses = ServiceTokenOperator.class.getDeclaredClasses();
        Class<?> serviceTokenBuilderClass = Arrays.stream(innerClasses)
                .filter(c -> c.getSimpleName().equals("ServiceTokenBuilder"))
                .findFirst()
                .orElse(null);

        assertNotNull(serviceTokenBuilderClass,
                "ServiceTokenOperator should have an inner class named ServiceTokenBuilder");

        boolean hasDryRunField = false;
        for (Field field : serviceTokenBuilderClass.getDeclaredFields()) {
            if (field.getName().equals("dryRun") && field.getType() == boolean.class) {
                hasDryRunField = true;
                break;
            }
        }
        assertTrue(hasDryRunField,
                "ServiceTokenBuilder should have a 'dryRun' boolean field");
    }

    @Test
    void serviceTokenBuilder_hasDryRunMethod() {
        Class<?>[] innerClasses = ServiceTokenOperator.class.getDeclaredClasses();
        Class<?> serviceTokenBuilderClass = Arrays.stream(innerClasses)
                .filter(c -> c.getSimpleName().equals("ServiceTokenBuilder"))
                .findFirst()
                .orElse(null);

        assertNotNull(serviceTokenBuilderClass);

        boolean hasDryRunMethod = false;
        for (Method method : serviceTokenBuilderClass.getDeclaredMethods()) {
            if (method.getName().equals("dryRun") && method.getParameterCount() == 0) {
                hasDryRunMethod = true;
                assertEquals(serviceTokenBuilderClass, method.getReturnType(),
                        "dryRun() should return the ServiceTokenBuilder for fluent chaining");
                break;
            }
        }
        assertTrue(hasDryRunMethod,
                "ServiceTokenBuilder should have a 'dryRun()' method");
    }
}
