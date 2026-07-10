package org.cosmicide.gradle;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.ConfigurableLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Main {

    public static void main(String[] args) {
        ProtocolWriter writer = new ProtocolWriter();

        try (GradleToolingServer server = new GradleToolingServer(
                writer,
                ConnectionKey.fromStartupArgs(args)
        ); BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                Map<String, Object> request;

                try {
                    Object parsed = Json.parse(line);
                    if (!(parsed instanceof Map)) {
                        writer.error(null, "InvalidRequest", "Request must be a JSON object", null);
                        continue;
                    }

                    request = castMap(parsed);
                } catch (Throwable t) {
                    writer.error(null, "InvalidJson", t.getMessage(), t);
                    continue;
                }

                server.handle(request);

                Object method = request.get("method");
                if ("shutdown".equals(method)) {
                    break;
                }
            }
        } catch (Throwable t) {
            writer.error(null, "ServerError", t.getMessage(), t);
        }
    }

    private static Map<String, Object> basicProjectToJson(BasicGradleProject project) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", project.getName());
        map.put("path", project.getPath());
        map.put("buildTreePath", safeBuildTreePath(project, project.getPath()));
        map.put("projectDir", project.getProjectDirectory().getAbsolutePath());

        List<Map<String, Object>> children = new ArrayList<>();
        for (BasicGradleProject child : project.getChildren()) {
            children.add(basicProjectToJson(child));
        }

        map.put("children", children);
        return map;
    }

    private static List<Map<String, Object>> basicProjectsToJson(Collection<? extends BasicGradleProject> projects) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (BasicGradleProject project : projects) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", project.getName());
            map.put("path", project.getPath());
            map.put("buildTreePath", safeBuildTreePath(project, project.getPath()));
            map.put("projectDir", project.getProjectDirectory().getAbsolutePath());
            list.add(map);
        }

        sortByString(list, "path");
        return list;
    }

    private static List<Map<String, Object>> gradleBuildsToJson(Collection<? extends GradleBuild> builds) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (GradleBuild build : builds) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rootProject", basicProjectToJson(build.getRootProject()));
            map.put("projects", basicProjectsToJson(build.getProjects()));
            list.add(map);
        }

        return list;
    }

    private static List<GradleProject> collectProjects(GradleProject root) {
        List<GradleProject> result = new ArrayList<>();
        collectProjectsInto(root, result);
        return result;
    }

    private static void collectProjectsInto(GradleProject project, List<GradleProject> result) {
        result.add(project);

        for (GradleProject child : project.getChildren()) {
            collectProjectsInto(child, result);
        }
    }

    private static Map<String, Object> taskToJson(Task task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", nullToEmpty(task.getName()));
        map.put("path", nullToEmpty(task.getPath()));
        map.put("buildTreePath", safeTaskBuildTreePath(task));
        map.put("group", nullToEmpty(task.getGroup()));
        map.put("description", nullToEmpty(task.getDescription()));
        map.put("displayName", nullToEmpty(task.getDisplayName()));
        map.put("public", task.isPublic());

        try {
            map.put("projectPath", task.getProjectIdentifier().getProjectPath());
            map.put("buildRoot", task.getProjectIdentifier().getBuildIdentifier().getRootDir().getAbsolutePath());
        } catch (Throwable ignored) {
            map.put("projectPath", "");
            map.put("buildRoot", "");
        }

        return map;
    }

    private static Map<String, Object> taskSelectorToJson(TaskSelector selector) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", nullToEmpty(selector.getName()));
        map.put("displayName", nullToEmpty(selector.getDisplayName()));
        map.put("description", nullToEmpty(selector.getDescription()));
        map.put("public", selector.isPublic());

        try {
            map.put("projectPath", selector.getProjectIdentifier().getProjectPath());
            map.put("buildRoot", selector.getProjectIdentifier().getBuildIdentifier().getRootDir().getAbsolutePath());
        } catch (Throwable ignored) {
            map.put("projectPath", "");
            map.put("buildRoot", "");
        }

        return map;
    }

    private static String safeTaskBuildTreePath(Task task) {
        try {
            String value = task.getBuildTreePath();
            return value == null ? task.getPath() : value;
        } catch (UnsupportedMethodException ignored) {
            return task.getPath();
        } catch (Throwable ignored) {
            return task.getPath();
        }
    }

    private static String safeBuildTreePath(Object object, String fallback) {
        try {
            Method method = object.getClass().getMethod("getBuildTreePath");
            Object value = method.invoke(object);
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String safeProgressDisplayName(ProgressEvent event) {
        try {
            return nullToEmpty(event.getDisplayName());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String safeProgressDescriptor(ProgressEvent event) {
        try {
            return nullToEmpty(event.getDescriptor().getDisplayName());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String operationId(Object id, Map<String, Object> params) {
        String explicit = asString(params.get("opId"));
        if (explicit != null && !explicit.isEmpty()) {
            return explicit;
        }

        if (id != null) {
            return String.valueOf(id);
        }

        return UUID.randomUUID().toString();
    }

    private static String taskNameFromMethod(String method) {
        if (method == null || method.isEmpty()) {
            return null;
        }

        if (method.startsWith("gradle/")) {
            return method.substring("gradle/".length());
        }

        return method;
    }

    private static Map<String, Object> params(Map<String, Object> request) {
        Object raw = request.get("params");
        if (raw instanceof Map) {
            return castMap(raw);
        }
        return Collections.emptyMap();
    }

    private static Map<String, Object> obj(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }

        List<?> raw = (List<?>) value;
        List<String> result = new ArrayList<>(raw.size());

        for (Object item : raw) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }

        return result;
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }

        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private static boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        return fallback;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static void sortByString(List<Map<String, Object>> list, String key) {
        list.sort(Comparator.comparing(map -> String.valueOf(map.get(key))));
    }

    private static String safeMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return t.getClass().getName();
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    private static final class GradleToolingServer implements Closeable {
        private final ProtocolWriter writer;
        private final ExecutorService executor;
        private final ConnectionKey project;
        private final GradleConnector connector;
        private final Map<String, CancellationTokenSource> running;
        private final Map<String, InteractiveInput> inputs;
        private final AtomicBoolean closed;
        private ProjectConnection connection;

        GradleToolingServer(ProtocolWriter writer, ConnectionKey project) {
            this.writer = writer;
            this.project = project;
            this.connector = connectorFor(project);
            this.executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "cosmic-gradle-provider-worker");
                thread.setDaemon(false);
                return thread;
            });
            this.running = new ConcurrentHashMap<>();
            this.inputs = new ConcurrentHashMap<>();
            this.closed = new AtomicBoolean(false);
        }

        private static GradleConnector connectorFor(ConnectionKey project) {
            GradleConnector connector = GradleConnector.newConnector()
                    .forProjectDirectory(new File(project.projectDir));

            if (project.gradleUserHome != null) {
                connector.useGradleUserHomeDir(new File(project.gradleUserHome));
            }

            if (project.gradleInstallation != null) {
                connector.useInstallation(new File(project.gradleInstallation));
            } else if (project.gradleVersion != null) {
                connector.useGradleVersion(project.gradleVersion);
            } else {
                connector.useBuildDistribution();
            }

            return connector;
        }

        private static boolean containsConsoleArg(List<String> arguments) {
            for (String arg : arguments) {
                if (arg.equals("--console=plain") || arg.startsWith("--console=")) {
                    return true;
                }
            }
            return false;
        }

        void handle(Map<String, Object> request) {
            Object id = request.get("id");
            String method = asString(request.get("method"));

            if (method == null || method.isEmpty()) {
                writer.error(id, "InvalidRequest", "Missing string field: method", null);
                return;
            }

            if (closed.get() && !"shutdown".equals(method)) {
                writer.error(id, "ProviderClosed", "Provider is already closed", null);
                return;
            }

            if (!"ping".equals(method) && !"shutdown".equals(method) && hasRequestProjectBinding(request)) {
                writer.error(
                        id,
                        "InvalidRequest",
                        "Project binding is fixed at server startup; do not send params.projectDir or params.rootDir",
                        null
                );
                return;
            }

            switch (method) {
                case "ping":
                    ping(id);
                    return;

                case "shutdown":
                    writer.result(id, obj(
                            "shuttingDown", true
                    ));
                    close();
                    return;

                case "gradle/cancel":
                    cancel(request);
                    return;

                case "gradle/input":
                    sendInput(request);
                    return;

                case "gradle/closeProject":
                    closeProject(request);
                    return;

                default:
                    executor.execute(() -> {
                        try {
                            dispatchGradleRequest(request);
                        } catch (Throwable t) {
                            writer.error(id, t.getClass().getName(), safeMessage(t), t);
                        }
                    });
            }
        }

        private void dispatchGradleRequest(Map<String, Object> request) throws IOException {
            String method = asString(request.get("method"));

            switch (method) {
                case "gradle/environment":
                    environment(request);
                    break;
                case "gradle/projects":
                    projects(request);
                    break;
                case "gradle/tasks":
                    tasks(request);
                    break;
                case "gradle/run":
                    runBuild(request);
                    break;
                case "gradle/notifyChanged":
                    notifyChanged(request);
                    break;
                default:
                    runGenericGradleTask(request);
                    break;
            }
        }

        private boolean hasRequestProjectBinding(Map<String, Object> request) {
            Map<String, Object> params = params(request);
            return params.containsKey("projectDir") || params.containsKey("rootDir");
        }

        private void ping(Object id) {
            writer.result(id, obj(
                    "name", "Cosmic Gradle Tooling Provider",
                    "protocol", 1,
                    "projectDir", project.projectDir,
                    "javaVersion", System.getProperty("java.version"),
                    "javaHome", System.getProperty("java.home")
            ));
        }

        private void environment(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);
            String opId = operationId(id, params);

            CancellationTokenSource token = GradleConnector.newCancellationTokenSource();
            running.put(opId, token);

            writer.event("gradle/operationStarted", obj(
                    "opId", opId,
                    "method", "gradle/environment"
            ));

            try {
                ModelBuilder<BuildEnvironment> builder = connection().model(BuildEnvironment.class);
                configure(builder, params, opId, token);

                BuildEnvironment env = builder.get();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("opId", opId);
                result.put("buildRoot", env.getBuildIdentifier().getRootDir().getAbsolutePath());
                result.put("gradleVersion", env.getGradle().getGradleVersion());
                result.put("gradleUserHome", env.getGradle().getGradleUserHome().getAbsolutePath());
                result.put("versionInfo", env.getVersionInfo());

                try {
                    result.put("javaHome", env.getJava().getJavaHome().getAbsolutePath());
                    result.put("jvmArguments", new ArrayList<>(env.getJava().getJvmArguments()));
                } catch (UnsupportedMethodException ignored) {
                    result.put("javaHome", "");
                    result.put("jvmArguments", Collections.emptyList());
                }

                writer.result(id, result);
            } finally {
                running.remove(opId);
                writer.event("gradle/operationFinished", obj(
                        "opId", opId,
                        "method", "gradle/environment"
                ));
            }
        }

        private void projects(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);
            String opId = operationId(id, params);

            CancellationTokenSource token = GradleConnector.newCancellationTokenSource();
            running.put(opId, token);

            writer.event("gradle/operationStarted", obj(
                    "opId", opId,
                    "method", "gradle/projects"
            ));

            try {
                ModelBuilder<GradleBuild> builder = connection().model(GradleBuild.class);
                configure(builder, params, opId, token);

                GradleBuild build = builder.get();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("opId", opId);
                result.put("rootProject", basicProjectToJson(build.getRootProject()));
                result.put("projects", basicProjectsToJson(build.getProjects()));
                result.put("includedBuilds", gradleBuildsToJson(build.getIncludedBuilds()));

                writer.result(id, result);
            } finally {
                running.remove(opId);
                writer.event("gradle/operationFinished", obj(
                        "opId", opId,
                        "method", "gradle/projects"
                ));
            }
        }

        private void tasks(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);
            String opId = operationId(id, params);

            CancellationTokenSource token = GradleConnector.newCancellationTokenSource();
            running.put(opId, token);

            writer.event("gradle/operationStarted", obj(
                    "opId", opId,
                    "method", "gradle/tasks"
            ));

            try {
                ProjectConnection connection = connection();

                ModelBuilder<BuildInvocations> invocationsBuilder =
                        connection.model(BuildInvocations.class);
                configure(invocationsBuilder, params, opId, token);
                BuildInvocations invocations = invocationsBuilder.get();

                ModelBuilder<GradleProject> projectBuilder =
                        connection.model(GradleProject.class);
                configure(projectBuilder, params, opId, token);
                GradleProject rootProject = projectBuilder.get();

                List<Map<String, Object>> projectTasks = new ArrayList<>();
                for (GradleProject project : collectProjects(rootProject)) {
                    for (Task task : project.getTasks()) {
                        projectTasks.add(taskToJson(task));
                    }
                }

                List<Map<String, Object>> invocationTasks = new ArrayList<>();
                for (Task task : invocations.getTasks()) {
                    invocationTasks.add(taskToJson(task));
                }

                List<Map<String, Object>> selectors = new ArrayList<>();
                for (TaskSelector selector : invocations.getTaskSelectors()) {
                    selectors.add(taskSelectorToJson(selector));
                }

                sortByString(projectTasks, "path");
                sortByString(invocationTasks, "path");
                sortByString(selectors, "name");

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("opId", opId);
                result.put("tasks", projectTasks);
                result.put("invocationTasks", invocationTasks);
                result.put("taskSelectors", selectors);

                writer.result(id, result);
            } finally {
                running.remove(opId);
                writer.event("gradle/operationFinished", obj(
                        "opId", opId,
                        "method", "gradle/tasks"
                ));
            }
        }

        private void runBuild(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);
            String opId = operationId(id, params);

            List<String> tasks = new ArrayList<>(stringList(params.get("tasks")));
            String task = asString(params.get("task"));
            if (tasks.isEmpty() && task != null && !task.isEmpty()) {
                tasks.add(task);
            }

            if (tasks.isEmpty()) {
                writer.error(id, "MissingTasks", "gradle/run requires params.tasks or params.task", null);
                return;
            }

            CancellationTokenSource token = GradleConnector.newCancellationTokenSource();
            InteractiveInput input = new InteractiveInput(writer, opId);

            running.put(opId, token);
            inputs.put(opId, input);

            writer.event("gradle/buildStarted", obj(
                    "opId", opId,
                    "tasks", tasks,
                    "interactive", true
            ));

            try {
                BuildLauncher launcher = connection().newBuild();
                configure(launcher, params, opId, token);

                launcher.setStandardInput(input);

                launcher.forTasks(tasks.toArray(new String[0]));
                launcher.run();

                writer.result(id, obj(
                        "opId", opId,
                        "success", true
                ));
            } finally {
                running.remove(opId);
                inputs.remove(opId);
                input.close();

                writer.event("gradle/buildFinished", obj(
                        "opId", opId
                ));
            }
        }

        private void runGenericGradleTask(Map<String, Object> request) {
            Map<String, Object> params = new LinkedHashMap<>(params(request));
            List<String> tasks = new ArrayList<>(stringList(params.get("tasks")));

            String task = asString(params.get("task"));
            if (tasks.isEmpty() && task != null && !task.isEmpty()) {
                tasks.add(task);
            }

            if (tasks.isEmpty()) {
                String derivedTask = taskNameFromMethod(asString(request.get("method")));
                if (derivedTask != null && !derivedTask.isEmpty()) {
                    tasks.add(derivedTask);
                }
            }

            if (tasks.isEmpty()) {
                writer.error(request.get("id"), "MissingTasks", "Request requires params.tasks, params.task, or a task-like method name", null);
                return;
            }

            params.put("tasks", tasks);

            Map<String, Object> buildRequest = new LinkedHashMap<>(request);
            buildRequest.put("params", params);
            runBuild(buildRequest);
        }

        private void sendInput(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);

            String opId = asString(params.get("opId"));
            String text = asString(params.get("text"));

            if (opId == null || opId.isEmpty()) {
                writer.error(id, "MissingOperationId", "gradle/input requires params.opId", null);
                return;
            }

            if (text == null) {
                writer.error(id, "MissingInputText", "gradle/input requires params.text", null);
                return;
            }

            InteractiveInput input = inputs.get(opId);

            if (input == null) {
                writer.error(id, "NoSuchInput", "No interactive input stream exists for opId: " + opId, null);
                return;
            }

            try {
                input.write(text);

                writer.result(id, obj(
                        "opId", opId,
                        "accepted", true,
                        "bytes", text.getBytes(StandardCharsets.UTF_8).length
                ));
            } catch (Throwable t) {
                writer.error(id, t.getClass().getName(), safeMessage(t), t);
            }
        }

        private void notifyChanged(Map<String, Object> request) throws IOException {
            Object id = request.get("id");
            Map<String, Object> params = params(request);

            List<String> rawPaths = stringList(params.get("paths"));
            List<Path> paths = new ArrayList<>(rawPaths.size());

            for (String rawPath : rawPaths) {
                paths.add(new File(rawPath).getCanonicalFile().toPath());
            }

            connection().notifyDaemonsAboutChangedPaths(paths);

            writer.result(id, obj(
                    "notified", paths.size()
            ));
        }

        private void cancel(Map<String, Object> request) {
            Object id = request.get("id");
            Map<String, Object> params = params(request);
            String opId = asString(params.get("opId"));

            if (opId == null || opId.isEmpty()) {
                writer.error(id, "MissingOperationId", "gradle/cancel requires params.opId", null);
                return;
            }

            CancellationTokenSource token = running.remove(opId);
            if (token != null) {
                token.cancel();
            }

            InteractiveInput input = inputs.remove(opId);
            if (input != null) {
                input.close();
            }

            writer.result(id, obj(
                    "opId", opId,
                    "cancelRequested", token != null
            ));
        }

        private void closeProject(Map<String, Object> request) {
            Object id = request.get("id");

            try {
                boolean closed = closeConnection();

                writer.result(id, obj(
                        "closed", closed,
                        "projectDir", project.projectDir
                ));
            } catch (Throwable t) {
                writer.error(id, t.getClass().getName(), safeMessage(t), t);
            }
        }

        private synchronized ProjectConnection connection() {
            if (connection != null) {
                return connection;
            }

            connection = connector.connect();
            return connection;
        }

        private synchronized boolean closeConnection() {
            ProjectConnection existing = connection;
            connection = null;

            if (existing == null) {
                return false;
            }

            existing.close();
            return true;
        }

        private void configure(
                ConfigurableLauncher<?> operation,
                Map<String, Object> params,
                String opId,
                CancellationTokenSource token
        ) {
            List<String> arguments = new ArrayList<>(stringList(params.get("arguments")));

            boolean plainConsole = bool(params.get("plainConsole"), true);
            if (plainConsole && !containsConsoleArg(arguments)) {
                arguments.add("--console=plain");
            }

            if (!arguments.isEmpty()) {
                operation.withArguments(arguments);
            }

            List<String> jvmArguments = stringList(params.get("jvmArguments"));
            if (!jvmArguments.isEmpty()) {
                operation.setJvmArguments(jvmArguments);
            }

            String javaHome = asString(params.get("javaHome"));
            if (javaHome != null && !javaHome.isEmpty()) {
                operation.setJavaHome(new File(javaHome));
            }

            Map<String, String> env = stringMap(params.get("env"));
            if (!env.isEmpty()) {
                Map<String, String> merged = new LinkedHashMap<>(System.getenv());
                merged.putAll(env);
                operation.setEnvironmentVariables(merged);
            }

            Map<String, String> systemProperties = stringMap(params.get("systemProperties"));
            if (!systemProperties.isEmpty()) {
                operation.withSystemProperties(systemProperties);
            }

            operation.setColorOutput(bool(params.get("colorOutput"), false));
            operation.withCancellationToken(token.token());

            operation.setStandardOutput(new GradleEventOutputStream(writer, opId, "stdout"));
            operation.setStandardError(new GradleEventOutputStream(writer, opId, "stderr"));

            operation.addProgressListener((ProgressListener) event -> {
                writer.event("gradle/progress", obj(
                        "opId", opId,
                        "type", event.getClass().getSimpleName(),
                        "displayName", safeProgressDisplayName(event),
                        "descriptor", safeProgressDescriptor(event)
                ));
            });
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            for (CancellationTokenSource token : running.values()) {
                try {
                    token.cancel();
                } catch (Throwable ignored) {
                }
            }
            running.clear();

            try {
                closeConnection();
            } catch (Throwable ignored) {
            }

            for (InteractiveInput input : inputs.values()) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
            inputs.clear();

            executor.shutdownNow();

            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class GradleEventOutputStream extends OutputStream {
        private static final ScheduledExecutorService PARTIAL_OUTPUT_FLUSHER =
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "cosmic-gradle-output-flusher");
                    thread.setDaemon(true);
                    return thread;
                });

        private static final long PARTIAL_FLUSH_DELAY_MS = 80L;

        private final ProtocolWriter writer;
        private final String opId;
        private final String stream;
        private final ByteArrayOutputStream buffer;

        private ScheduledFuture<?> pendingFlush;
        private boolean closed;

        GradleEventOutputStream(ProtocolWriter writer, String opId, String stream) {
            this.writer = writer;
            this.opId = opId;
            this.stream = stream;
            this.buffer = new ByteArrayOutputStream(256);
        }

        @Override
        public synchronized void write(int b) {
            if (closed) {
                return;
            }

            if (b == '\n') {
                buffer.write('\n');
                flushBufferLocked(false);
                return;
            }

            if (b == '\r') {
                buffer.write('\n');
                flushBufferLocked(false);
                return;
            }

            buffer.write(b);
            schedulePartialFlushLocked();
        }

        @Override
        public synchronized void write(byte[] bytes, int off, int len) {
            if (bytes == null) {
                throw new NullPointerException("bytes");
            }

            if (off < 0 || len < 0 || len > bytes.length - off) {
                throw new IndexOutOfBoundsException();
            }

            if (closed || len == 0) {
                return;
            }

            for (int i = off; i < off + len; i++) {
                int b = bytes[i] & 0xff;

                if (b == '\n') {
                    buffer.write('\n');
                    flushBufferLocked(false);
                } else if (b == '\r') {
                    buffer.write('\n');
                    flushBufferLocked(false);
                } else {
                    buffer.write(b);
                }
            }

            if (buffer.size() > 0) {
                schedulePartialFlushLocked();
            }
        }

        @Override
        public synchronized void flush() {
            if (closed) {
                return;
            }

            flushBufferLocked(true);
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }

            flushBufferLocked(false);
            closed = true;
            cancelPendingFlushLocked();
        }

        private void schedulePartialFlushLocked() {
            cancelPendingFlushLocked();

            pendingFlush = PARTIAL_OUTPUT_FLUSHER.schedule(() -> {
                synchronized (GradleEventOutputStream.this) {
                    if (!closed) {
                        flushBufferLocked(true);
                    }
                }
            }, PARTIAL_FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
        }

        private void cancelPendingFlushLocked() {
            if (pendingFlush != null) {
                pendingFlush.cancel(false);
                pendingFlush = null;
            }
        }

        private void flushBufferLocked(boolean partial) {
            cancelPendingFlushLocked();

            if (buffer.size() == 0) {
                return;
            }

            String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            buffer.reset();

            if (text.isEmpty()) {
                return;
            }

            writer.event("gradle/output", obj(
                    "opId", opId,
                    "stream", stream,
                    "text", text,
                    "partial", partial
            ));
        }
    }

    private static final class InteractiveInput extends java.io.InputStream implements Closeable {
        private final ArrayDeque<Byte> queue = new ArrayDeque<>();
        private final ProtocolWriter writer;
        private final String opId;

        private boolean closed;
        private boolean waiting;

        InteractiveInput(ProtocolWriter writer, String opId) {
            this.writer = writer;
            this.opId = opId;
        }

        synchronized void write(String text) {
            if (closed) {
                throw new IllegalStateException("Input stream is already closed");
            }

            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

            for (byte b : bytes) {
                queue.addLast(b);
            }

            waiting = false;
            notifyAll();
        }

        synchronized void finish() {
            closed = true;
            waiting = false;
            notifyAll();
        }

        @Override
        public synchronized int read() throws IOException {
            while (queue.isEmpty() && !closed) {
                if (!waiting) {
                    waiting = true;

                    writer.event("gradle/inputRequested", obj(
                            "opId", opId
                    ));
                }

                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for interactive input", e);
                }
            }

            if (!queue.isEmpty()) {
                return queue.removeFirst() & 0xff;
            }

            return -1;
        }

        @Override
        public synchronized int read(byte[] buffer, int off, int len) throws IOException {
            if (buffer == null) {
                throw new NullPointerException("buffer");
            }

            if (off < 0 || len < 0 || len > buffer.length - off) {
                throw new IndexOutOfBoundsException();
            }

            if (len == 0) {
                return 0;
            }

            int first = read();
            if (first == -1) {
                return -1;
            }

            buffer[off] = (byte) first;

            int count = 1;

            while (count < len) {
                Byte next = queue.pollFirst();
                if (next == null) {
                    break;
                }

                buffer[off + count] = next;
                count++;
            }

            return count;
        }

        @Override
        public synchronized void close() {
            finish();
        }
    }

    private static final class ProtocolWriter {
        private final Object lock = new Object();
        private final OutputStreamWriter writer;

        ProtocolWriter() {
            this.writer = new OutputStreamWriter(
                    new FileOutputStream(FileDescriptor.out),
                    StandardCharsets.UTF_8
            );
        }

        void result(Object id, Map<String, Object> result) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("id", id);
            message.put("ok", true);
            message.put("result", result);
            send(message);
        }

        void error(Object id, String type, String message, Throwable throwable) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", type);
            error.put("type", type);
            error.put("message", message == null ? "" : message);

            if (throwable != null) {
                error.put("stack", stackTrace(throwable));
            }

            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("id", id);
            wrapper.put("ok", false);
            wrapper.put("error", error);

            send(wrapper);
        }

        void event(String event, Map<String, Object> body) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("event", event);
            message.putAll(body);
            send(message);
        }

        private void send(Map<String, Object> message) {
            synchronized (lock) {
                try {
                    writer.write(Json.stringify(message));
                    writer.write('\n');
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("Failed to write protocol message: " + e.getMessage());
                }
            }
        }
    }

    private static final class ConnectionKey {
        final String projectDir;
        final String gradleUserHome;
        final String gradleVersion;
        final String gradleInstallation;

        private ConnectionKey(
                String projectDir,
                String gradleUserHome,
                String gradleVersion,
                String gradleInstallation
        ) {
            this.projectDir = projectDir;
            this.gradleUserHome = emptyToNull(gradleUserHome);
            this.gradleVersion = emptyToNull(gradleVersion);
            this.gradleInstallation = emptyToNull(gradleInstallation);
        }

        static ConnectionKey fromStartupParams(Map<String, Object> params) {
            String projectDir = asString(params.get("projectDir"));
            if (projectDir == null || projectDir.isEmpty()) {
                projectDir = asString(params.get("rootDir"));
            }
            if (projectDir == null || projectDir.isEmpty()) {
                throw new IllegalArgumentException("Missing string field: params.projectDir");
            }

            try {
                String canonicalProjectDir = new File(projectDir).getCanonicalFile().getAbsolutePath();

                String gradleUserHome = asString(params.get("gradleUserHome"));
                if (gradleUserHome != null && !gradleUserHome.isEmpty()) {
                    gradleUserHome = new File(gradleUserHome).getCanonicalFile().getAbsolutePath();
                }

                String gradleInstallation = asString(params.get("gradleInstallation"));
                if (gradleInstallation != null && !gradleInstallation.isEmpty()) {
                    gradleInstallation = new File(gradleInstallation).getCanonicalFile().getAbsolutePath();
                }

                return new ConnectionKey(
                        canonicalProjectDir,
                        gradleUserHome,
                        asString(params.get("gradleVersion")),
                        gradleInstallation
                );
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to canonicalize project path", e);
            }
        }

        static ConnectionKey fromStartupArgs(String[] args) {
            Map<String, Object> params = new LinkedHashMap<>();

            if (args.length == 1 && !args[0].startsWith("--")) {
                params.put("projectDir", args[0]);
                return fromStartupParams(params);
            }

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if ("--project-dir".equals(arg) || "--projectDir".equals(arg)) {
                    params.put("projectDir", requiredArgValue(args, ++i, arg));
                } else if ("--gradle-user-home".equals(arg) || "--gradleUserHome".equals(arg)) {
                    params.put("gradleUserHome", requiredArgValue(args, ++i, arg));
                } else if ("--gradle-version".equals(arg) || "--gradleVersion".equals(arg)) {
                    params.put("gradleVersion", requiredArgValue(args, ++i, arg));
                } else if ("--gradle-installation".equals(arg) || "--gradleInstallation".equals(arg)) {
                    params.put("gradleInstallation", requiredArgValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            return fromStartupParams(params);
        }

        private static String requiredArgValue(String[] args, int index, String name) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + name);
            }

            return args[index];
        }

        private static boolean eq(Object a, Object b) {
            return a == null ? b == null : a.equals(b);
        }

        private static int hash(Object value) {
            return value == null ? 0 : value.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ConnectionKey)) {
                return false;
            }

            ConnectionKey that = (ConnectionKey) other;
            return eq(projectDir, that.projectDir)
                    && eq(gradleUserHome, that.gradleUserHome)
                    && eq(gradleVersion, that.gradleVersion)
                    && eq(gradleInstallation, that.gradleInstallation);
        }

        @Override
        public int hashCode() {
            int result = projectDir.hashCode();
            result = 31 * result + hash(gradleUserHome);
            result = 31 * result + hash(gradleVersion);
            result = 31 * result + hash(gradleInstallation);
            return result;
        }
    }

    private static final class Json {
        static Object parse(String input) {
            Parser parser = new Parser(input);
            Object value = parser.parseValue();
            parser.skipWhitespace();

            if (!parser.isEnd()) {
                throw new IllegalArgumentException("Unexpected trailing JSON at index " + parser.index);
            }

            return value;
        }

        static String stringify(Object value) {
            StringBuilder out = new StringBuilder(256);
            writeValue(out, value);
            return out.toString();
        }

        private static void writeValue(StringBuilder out, Object value) {
            if (value == null) {
                out.append("null");
                return;
            }

            if (value instanceof String) {
                writeString(out, (String) value);
                return;
            }

            if (value instanceof Number || value instanceof Boolean) {
                out.append(value);
                return;
            }

            if (value instanceof Map) {
                writeObject(out, castMap(value));
                return;
            }

            if (value instanceof Iterable) {
                writeArray(out, (Iterable<?>) value);
                return;
            }

            writeString(out, String.valueOf(value));
        }

        private static void writeObject(StringBuilder out, Map<String, Object> map) {
            out.append('{');
            boolean first = true;

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }

                first = false;
                writeString(out, entry.getKey());
                out.append(':');
                writeValue(out, entry.getValue());
            }

            out.append('}');
        }

        private static void writeArray(StringBuilder out, Iterable<?> list) {
            out.append('[');
            boolean first = true;

            for (Object item : list) {
                if (!first) {
                    out.append(',');
                }

                first = false;
                writeValue(out, item);
            }

            out.append(']');
        }

        private static void writeString(StringBuilder out, String value) {
            out.append('"');

            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);

                switch (c) {
                    case '"':
                        out.append("\\\"");
                        break;
                    case '\\':
                        out.append("\\\\");
                        break;
                    case '\b':
                        out.append("\\b");
                        break;
                    case '\f':
                        out.append("\\f");
                        break;
                    case '\n':
                        out.append("\\n");
                        break;
                    case '\r':
                        out.append("\\r");
                        break;
                    case '\t':
                        out.append("\\t");
                        break;
                    default:
                        if (c < 0x20) {
                            out.append("\\u");
                            String hex = Integer.toHexString(c);
                            for (int j = hex.length(); j < 4; j++) {
                                out.append('0');
                            }
                            out.append(hex);
                        } else {
                            out.append(c);
                        }
                        break;
                }
            }

            out.append('"');
        }

        private static final class Parser {
            private final String input;
            private int index;

            Parser(String input) {
                this.input = input;
            }

            boolean isEnd() {
                return index >= input.length();
            }

            void skipWhitespace() {
                while (!isEnd()) {
                    char c = input.charAt(index);
                    if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                        index++;
                    } else {
                        break;
                    }
                }
            }

            Object parseValue() {
                skipWhitespace();

                if (isEnd()) {
                    throw new IllegalArgumentException("Unexpected end of JSON");
                }

                char c = input.charAt(index);

                switch (c) {
                    case '{':
                        return parseObject();
                    case '[':
                        return parseArray();
                    case '"':
                        return parseString();
                    case 't':
                        expect("true");
                        return true;
                    case 'f':
                        expect("false");
                        return false;
                    case 'n':
                        expect("null");
                        return null;
                    default:
                        if (c == '-' || (c >= '0' && c <= '9')) {
                            return parseNumber();
                        }
                        throw new IllegalArgumentException("Unexpected character '" + c + "' at index " + index);
                }
            }

            Map<String, Object> parseObject() {
                expect('{');

                Map<String, Object> map = new LinkedHashMap<>();
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    return map;
                }

                while (true) {
                    skipWhitespace();

                    if (!peek('"')) {
                        throw new IllegalArgumentException("Expected object key at index " + index);
                    }

                    String key = parseString();

                    skipWhitespace();
                    expect(':');

                    Object value = parseValue();
                    map.put(key, value);

                    skipWhitespace();

                    if (peek(',')) {
                        index++;
                        continue;
                    }

                    if (peek('}')) {
                        index++;
                        break;
                    }

                    throw new IllegalArgumentException("Expected ',' or '}' at index " + index);
                }

                return map;
            }

            List<Object> parseArray() {
                expect('[');

                List<Object> list = new ArrayList<>();
                skipWhitespace();

                if (peek(']')) {
                    index++;
                    return list;
                }

                while (true) {
                    list.add(parseValue());
                    skipWhitespace();

                    if (peek(',')) {
                        index++;
                        continue;
                    }

                    if (peek(']')) {
                        index++;
                        break;
                    }

                    throw new IllegalArgumentException("Expected ',' or ']' at index " + index);
                }

                return list;
            }

            String parseString() {
                expect('"');

                StringBuilder out = new StringBuilder();

                while (!isEnd()) {
                    char c = input.charAt(index++);

                    if (c == '"') {
                        return out.toString();
                    }

                    if (c != '\\') {
                        out.append(c);
                        continue;
                    }

                    if (isEnd()) {
                        throw new IllegalArgumentException("Unterminated escape sequence");
                    }

                    char escaped = input.charAt(index++);

                    switch (escaped) {
                        case '"':
                            out.append('"');
                            break;
                        case '\\':
                            out.append('\\');
                            break;
                        case '/':
                            out.append('/');
                            break;
                        case 'b':
                            out.append('\b');
                            break;
                        case 'f':
                            out.append('\f');
                            break;
                        case 'n':
                            out.append('\n');
                            break;
                        case 'r':
                            out.append('\r');
                            break;
                        case 't':
                            out.append('\t');
                            break;
                        case 'u':
                            out.append(parseUnicode());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape sequence \\" + escaped + " at index " + index);
                    }
                }

                throw new IllegalArgumentException("Unterminated string");
            }

            char parseUnicode() {
                if (index + 4 > input.length()) {
                    throw new IllegalArgumentException("Invalid unicode escape at index " + index);
                }

                String hex = input.substring(index, index + 4);
                index += 4;

                try {
                    return (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid unicode escape: " + hex, e);
                }
            }

            Number parseNumber() {
                int start = index;

                if (peek('-')) {
                    index++;
                }

                while (!isEnd() && isDigit(input.charAt(index))) {
                    index++;
                }

                boolean floating = false;

                if (!isEnd() && input.charAt(index) == '.') {
                    floating = true;
                    index++;

                    while (!isEnd() && isDigit(input.charAt(index))) {
                        index++;
                    }
                }

                if (!isEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                    floating = true;
                    index++;

                    if (!isEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                        index++;
                    }

                    while (!isEnd() && isDigit(input.charAt(index))) {
                        index++;
                    }
                }

                String raw = input.substring(start, index);

                try {
                    if (floating) {
                        return Double.parseDouble(raw);
                    }
                    return Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number: " + raw, e);
                }
            }

            boolean isDigit(char c) {
                return c >= '0' && c <= '9';
            }

            boolean peek(char expected) {
                return !isEnd() && input.charAt(index) == expected;
            }

            void expect(char expected) {
                skipWhitespace();

                if (isEnd() || input.charAt(index) != expected) {
                    throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
                }

                index++;
            }

            void expect(String expected) {
                if (!input.startsWith(expected, index)) {
                    throw new IllegalArgumentException("Expected " + expected + " at index " + index);
                }

                index += expected.length();
            }
        }
    }
}
