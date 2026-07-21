# Editor and language services

## Purpose

The editor has one routing layer for built-in and plugin-provided language behavior. The UI does
not select Java, Kotlin, Scala, or custom servers itself; it asks registered providers to configure
the current file.

```text
file -> EditorLanguageRouter -> EditorLanguageProvider
                                  -> LspServerProvider -> LSP process
                                  -> non-LSP language support
                                  -> plain-text fallback

format request -> EditorFormatterProvider
```

The contracts live in `:ide-api`. Sora and the process implementation remain app details. See
[Plugin architecture](plugin-architecture.md) for registration and enablement.

## Routing contracts

| Contract                  | Purpose                                                                                                       |
|---------------------------|---------------------------------------------------------------------------------------------------------------|
| `EditorLanguageProvider`  | Configures Sora for a file. Use directly only for behavior that does not fit the standard LSP adapter.        |
| `LspServerProvider`       | Supplies a data-only server definition and connection factory. This is the normal language integration point. |
| `EditorFormatterProvider` | Returns replacement text for a document or range. Formatting is independent of language-provider selection.   |

Providers are evaluated by descending priority. A failed provider is logged and skipped; the
router stops at the first successful provider. The built-in generic LSP provider delegates to the
first matching LSP server provider, and the final provider installs `EmptyLanguage`.

`supports` is called during routing, so it must be fast and side-effect free. Start processes only
from the connection factory.

## LSP boundary

`LspServerDefinition` describes the server identity, supported extension, connection factory,
optional bundled TextMate scope or linked TextMate grammar, initialization data, feature switches,
and timeout. It intentionally does not expose Sora classes.

The connection exposes server stdin, server stdout, startup, close, and liveness. Stderr is not
part of the LSP stream and must be drained separately by the provider. Anything written to stdout
other than `Content-Length`-framed LSP messages corrupts the session.

The app adapter owns the `LspProject`/`LspEditor`, attaches the shared `CodeEditor`, connects on an
IO coroutine, and sends post-initialization configuration. Incoming-frame tracing can include
source text and should be enabled only for diagnostics.

## Built-in servers

| Files                   | Process                                        |
|-------------------------|------------------------------------------------|
| `.java`                 | Eclipse JDT LS through the selected JDK        |
| `.kt`                   | `files/kotlin-lsp/bin/intellij-server --stdio` |
| `.scala`, `.sc`, `.sbt` | `files/scala/bin/metals`                       |

All launches use `ProcessExecutor`, so they inherit the canonical glibc/JDK environment described
in [Process execution and terminal](process-execution-and-terminal.md).

Each built-in provider currently caches one process for the active project. At the same time,
`ExistingProcessLspConnection.close` destroys that process. This makes ownership implicit when
several documents share a server. Any move to durable multi-document sessions should introduce an
explicit project-scoped owner and separate editor detach from process shutdown.

## Custom servers

Custom entries have higher priority than bundled providers and start with:

```text
bash -c <starter code>
```

The working directory is the project root, with `COSMIC_PROJECT_ROOT`, `COSMIC_FILE`, and `BASH_ENV`
set. `BASH_ENV` points to Cosmic's non-interactive Bash environment file. Starter code is executable
configuration with the app's permissions. It must be reviewed before importing it from a project or
third party. Use `exec` for the final server command so closing the connection terminates the real
process rather than only its shell parent.

Each entry may also carry one TextMate grammar link. Accepted sources are direct HTTP(S) URLs,
Android `content://` document URIs, `file://` URIs, and absolute paths. JSON, XML/plist, and YAML
grammars are detected from their content; the grammar's own `scopeName` is used, so users do not
enter it separately. The source is limited to 5 MB and is loaded on the IO dispatcher.

HTTPS sources use a URL-keyed cache under `cacheDir/textmate-grammar-cache`. A valid cached grammar
is reused for seven days. On expiry, the app downloads a candidate and replaces the cache atomically
only after TextMate accepts it. Network errors or invalid refreshed content fall back to the last
valid stale copy. Changing the link selects a different cache key immediately. Local files and
document URIs are read directly so edits are visible the next time the language is configured.

The configuration store permits at most one enabled custom server per normalized file extension.
Enabling or saving an entry disables other custom entries for that extension. Legacy preference
data containing duplicates is normalized when read. The generic LSP router still selects only the
highest-priority matching provider across built-in and plugin providers.

## Editor session behavior

`EditorScreen` reuses one `CodeEditor` as tabs change. `EditorViewModel` caches each open document
and writes active edits to disk immediately; this is an autosave model, not a long-lived dirty
buffer model.

Because the editor instance outlives individual files, every switch must replace language,
grammar, formatter, and LSP state. A provider must not retain the editor as though it belonged to
one document permanently.

Bundled TextMate language metadata comes from `assets/textmate/languages.json`. A server may request
a packaged grammar scope or provide a linked grammar. If neither is present, or a linked grammar
cannot be loaded and has no valid cache, its wrapper uses plain highlighting while the LSP is still
allowed to connect.

## Formatting status

The formatter router tries enabled providers by priority and applies the first successful result.
Provider results are text values; providers should not mutate or retain the editor.

The built-in Java and Kotlin formatter registrations currently return the input unchanged because
their formatter calls are disabled. Their presence in the registry does not mean formatting is
implemented.

## Adding language support

For a stdio server:

1. Implement `LspServerProvider` with a stable id and a cheap extension check.
2. Return a definition that starts the process lazily through `ProcessExecutor`.
3. Keep stdout protocol-only and drain stderr continuously.
4. Close streams and the process through the connection lifecycle.
5. Add a TextMate scope when the grammar is packaged, or a `textMateGrammarLink` when the source is
   intentionally external.
6. Test two files at once, project switching, disable/enable, startup failure, and timeout.

Use `EditorLanguageProvider` directly only when the standard LSP adapter is insufficient.

## Failure clues

| Symptom                           | Check first                                                                                        |
|-----------------------------------|----------------------------------------------------------------------------------------------------|
| Supported file remains plain text | provider enablement, extension match, priority, and `supports` failures                            |
| Editor stays non-editable         | connection timeout/failure recovery in the app adapter                                             |
| LSP parse errors                  | server logs or stderr reaching stdout                                                              |
| Server restarts on tab changes    | shared-process ownership and connection close behavior                                             |
| Custom server survives close      | starter did not `exec` its final command                                                           |
| Linked grammar is not highlighted | URL is not a raw file, document permission expired, file exceeds 5 MB, or grammar is malformed     |
| HTTPS grammar does not update yet | valid cache is younger than seven days; change the URL or clear app cache for an immediate refetch |
| Format command changes nothing    | the selected provider may be one of the current pass-through built-ins                             |
