# Gradle tooling bridge

## Purpose

Gradle's Tooling API cannot run on ART. Cosmic IDE exposes familiar Tooling API interfaces in the
app while running the real implementation in a selected glibc JDK process.

```text
Android app                                  JDK process
-----------                                  -----------
RemoteGradleConnector                        GradleToolingServer
RemoteProjectConnection   newline JSON       real ProjectConnection
model/build/test adapters  <------------->   Gradle daemon(s)
```

This is an explicit data bridge, not general Java remoting. Only represented operations and
read-only model data can cross it.

## Process ownership

`ToolingServerManager` owns at most one provider process, bound to one absolute project path.
Switching projects stops it; later work starts another. The provider is launched through
`ProcessExecutor`, so it and its Gradle descendants receive the selected JDK and canonical glibc
environment.

The app sends a `ping` after startup. Shutdown closes pending work and streams, asks the provider
to stop, and then destroys the process. Provider stderr is diagnostic output; stdout is reserved
for protocol messages.

## Protocol

The transport is UTF-8 with one JSON object per line.

```json
{"id":"req-1","method":"gradle/tasks","params":{"opId":"op-1"}}
{"id":"req-1","ok":true,"result":{}}
{"event":"gradle/output","opId":"op-1","stream":"stdout","text":"..."}
```

Request ids correlate exactly one response. Operation ids correlate the output, progress, input,
and cancellation events belonging to longer work. They serve different lifetimes and must not be
interchanged.

Writes from request workers and output-flush threads share one lock so concurrent events cannot
break line framing. Provider libraries must never write logs to stdout.

## Supported behavior

Named requests cover:

- environment, project, task, and arbitrary available model queries;
- build and test launch;
- changed-path notification;
- interactive input and cooperative cancellation;
- project connection close, provider shutdown, and health checking.

New-project creation binds a provider to the empty target directory and runs Gradle's built-in
`init` task through `BuildLauncher` with non-interactive language and package options. The temporary
provider is stopped after generation; project files are not assembled by the Android process.

An unknown `gradle/<name>` request is treated as a request to run task `<name>`. This is convenient
for arbitrary tasks, but it also means a misspelled protocol method can become a Gradle invocation.

Common operation settings—arguments, JVM arguments, system properties, environment overrides,
Java home, color, and detailed failures—are serialized. Streams, listeners, and cancellation
tokens stay local and are bridged by operation id.

Provider output is flushed by line, with a short partial-line flush for prompts. Progress events
are intentionally small snapshots rather than Gradle's full descriptor hierarchy. Interactive
input is requested only when Gradle attempts to read; cancellation closes that input and signals
Gradle's cancellation token.

## Model snapshots

Gradle model implementation objects are not serialized. The provider walks getters declared by
Tooling API interfaces and converts supported values to JSON. The app reconstructs interface values
as dynamic proxies backed by that JSON.

These values are snapshots:

- getters decode stored data but other methods are unsupported;
- they have no remote identity or mutation path;
- cycles, excessive depth, unsupported values, and failing getters become `null`;
- absent primitive values decode to Java defaults;
- `DomainObjectSet` uses a small immutable local implementation.

Code using a remote model must not assume it is a live Gradle object.

## Deliberate limits

- Arbitrary `BuildAction` bytecode cannot cross the protocol. Add a named data operation instead.
- Real test descriptors and the full typed progress hierarchy are not reconstructed.
- Progress listener operation-type filtering is not preserved.
- Model interfaces must exist in both the app and provider classpaths.
- Distribution URI selection is recorded by the app but is not passed to provider startup.
- Compatibility is represented by a protocol integer, with no feature negotiation.

The current app also adds per-operation listeners and cancellation polling without an explicit
removal/completion handle. Fix those lifecycle leaks before treating one provider process as an
unbounded session.

## Packaged provider artifact

`:feature:tooling` builds a dependency-inclusive JAR and copies it to
`app/src/main/assets/gradle-tooling.jar`. At runtime the asset is refreshed into app-private storage
before launch.

A provider source change is incomplete until the asset has been rebuilt and copied. Building an
app against changed client code while packaging an old provider creates protocol drift that may
look like a runtime bug.

## Adding an operation

1. Define a small JSON request/result contract and any events.
2. Validate parameters before allocating operation state.
3. Use an operation id when output, progress, input, or cancellation applies.
4. Remove cancellation and input state in `finally`.
5. Keep provider stdout protocol-only.
6. Add the app adapter and preserve old behavior or update compatibility handling.
7. Rebuild the packaged provider JAR.
8. Test success, typed failure, cancellation, concurrent output, and shutdown.

Do not send serialized implementation objects or executable bytecode.

## Failure clues

| Symptom                           | Check first                                                          |
|-----------------------------------|----------------------------------------------------------------------|
| Provider does not answer `ping`   | JDK/glibc launch, stale/missing asset, or stdout contamination       |
| JSON parsing fails                | non-protocol output on stdout or broken newline framing              |
| Output appears only at completion | partial-output flushing and operation-id routing                     |
| Interactive build hangs           | `gradle/inputRequested` handling and matching operation id           |
| Model getter returns null/default | unsupported getter/value, cycle/depth truncation, or decode mismatch |
| Project switch uses the old build | fixed-project server ownership and shutdown                          |
