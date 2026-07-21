package org.cosmicide.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MainProtocolTest {

    private static String escaped(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Object parse(String value) throws Exception {
        return invokeNested("Json", "parse", new Class<?>[]{String.class}, value);
    }

    private static String stringify(Object value) throws Exception {
        return (String) invokeNested("Json", "stringify", new Class<?>[]{Object.class}, value);
    }

    private static Object serialize(Object value, Type type) throws Exception {
        return invokeNested(
                "ModelSerializer",
                "serialize",
                new Class<?>[]{Object.class, Type.class},
                value,
                type
        );
    }

    private static Object invokeNested(
            String nestedClass,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Class<?> type = Class.forName(Main.class.getName() + "$" + nestedClass);
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw error;
        }
    }

    private static void assertParseFails(String input, String expectedMessage) throws Exception {
        try {
            parse(input);
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage(), error.getMessage().contains(expectedMessage));
            return;
        }
        throw new AssertionError("Expected JSON parsing to fail: " + input);
    }

    @Test
    public void providerHandlesInvalidInputPingBindingRejectionAndShutdown() throws Exception {
        File projectDir = Files.createTempDirectory("cosmic-tooling-project").toFile();
        try {
            Process process = startProvider(projectDir.getAbsolutePath());
            String requests = String.join("\n",
                    "not-json",
                    "[]",
                    "{\"id\":1,\"method\":\"ping\"}",
                    "{\"id\":2,\"method\":\"gradle/tasks\",\"params\":{\"projectDir\":\"/other\"}}",
                    "{\"id\":3,\"method\":\"shutdown\"}",
                    ""
            );
            process.getOutputStream().write(requests.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            assertTrue("provider did not stop", process.waitFor(15, TimeUnit.SECONDS));
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            assertEquals(stderr, 0, process.exitValue());
            assertTrue(stdout.contains("\"code\":\"InvalidJson\""));
            assertTrue(stdout.contains("\"code\":\"InvalidRequest\""));
            assertTrue(stdout.contains("\"name\":\"Cosmic Gradle Tooling Provider\""));
            assertTrue(stdout.contains("\"projectDir\":\"" + escaped(projectDir.getCanonicalPath()) + "\""));
            assertTrue(stdout.contains("Project binding is fixed at server startup"));
            assertTrue(stdout.contains("\"id\":3,\"ok\":true,\"result\":{\"shuttingDown\":true}"));
        } finally {
            projectDir.delete();
        }
    }

    @Test
    public void providerReportsMalformedStartupArgumentsAsProtocolError() throws Exception {
        Process process = startProvider("--project-dir");
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(stdout.contains("\"code\":\"ServerError\""));
        assertTrue(stdout.contains("Missing value for --project-dir"));
    }

    @Test
    public void jsonCodecRoundTripsNestedProtocolValuesAndEscapes() throws Exception {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("message", "line 1\n\"quoted\"\\path");
        value.put("number", 42L);
        value.put("fraction", 1.25d);
        value.put("enabled", true);
        value.put("items", Arrays.asList("one", null, 3L));

        String encoded = stringify(value);
        Object decoded = parse(encoded);

        assertEquals(value, decoded);
        assertTrue(encoded.contains("\\n"));
        assertTrue(encoded.contains("\\\"quoted\\\""));
        assertTrue(encoded.contains("\\\\path"));
    }

    @Test
    public void jsonCodecRejectsTrailingInputInvalidEscapesAndMalformedNumbers() throws Exception {
        assertParseFails("{} trailing", "Unexpected trailing JSON");
        assertParseFails("\"\\x\"", "Invalid escape sequence");
        assertParseFails("1.", "Invalid number");
        assertParseFails("1e+", "Invalid number");
        assertParseFails("{\"key\":}", "Unexpected character");
        assertParseFails("\"unterminated", "Unterminated string");
    }

    @Test
    public void modelSerializerHandlesCommonProtocolTypesAndCycles() throws Exception {
        SerializableModel model = new SerializableModel();
        model.child = model;

        @SuppressWarnings("unchecked")
        Map<String, Object> serialized = (Map<String, Object>) serialize(model, SerializableModel.class);

        assertEquals("model", serialized.get("name"));
        assertEquals("value", serialized.get("optional"));
        assertEquals(Arrays.asList(1, 2, 3), serialized.get("values"));
        assertEquals(new File("relative").getAbsolutePath(), serialized.get("file"));
        assertNull(serialized.get("child"));
        assertFalse(serialized.containsKey("class"));
    }

    private Process startProvider(String... args) throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
        List<String> command = new java.util.ArrayList<>();
        command.add(java);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        command.addAll(Arrays.asList(args));
        return new ProcessBuilder(command).start();
    }

    public static final class SerializableModel {
        private SerializableModel child;

        public String getName() {
            return "model";
        }

        public Optional<String> getOptional() {
            return Optional.of("value");
        }

        public List<Integer> getValues() {
            return Arrays.asList(1, 2, 3);
        }

        public File getFile() {
            return new File("relative");
        }

        public SerializableModel getChild() {
            return child;
        }
    }
}
