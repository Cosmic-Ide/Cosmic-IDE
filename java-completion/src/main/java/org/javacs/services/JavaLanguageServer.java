/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs.services;

import com.google.gson.JsonObject;

import org.cosmic.ide.lsp.IDELanguageClient;
import org.cosmic.ide.lsp.IDELanguageClientAware;
import org.cosmic.ide.lsp.IDELanguageServer;
import org.cosmic.ide.lsp.IDETextDocumentService;
import org.cosmic.ide.lsp.IDEWorkspaceService;
import org.cosmic.ide.lsp.SemanticHighlight;
import org.cosmic.ide.lsp.SemanticHighlightParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.DocumentOnTypeFormattingOptions;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.FileOperationFilter;
import org.eclipse.lsp4j.FileOperationOptions;
import org.eclipse.lsp4j.FileOperationPattern;
import org.eclipse.lsp4j.FileOperationsServerCapabilities;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkDoneProgressCancelParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.javacs.FileStore;
import org.javacs.FindNameAt;
import org.javacs.JavaCompilerService;
import org.javacs.JsonHelper;
import org.javacs.completion.CompletionProvider;
import org.javacs.completion.SignatureProvider;
import org.javacs.fold.FoldProvider;
import org.javacs.index.SymbolProvider;
import org.javacs.lens.CodeLensProvider;
import org.javacs.markup.ErrorProvider;
import org.javacs.navigation.DefinitionProvider;
import org.javacs.navigation.ReferenceProvider;
import org.javacs.rewrite.Rewrite;
import org.javacs.semantics.SemanticHighlightProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class JavaLanguageServer implements IDELanguageServer, IDELanguageClientAware, IDETextDocumentService, IDEWorkspaceService {

    public static final Position Position_NONE = new Position(-1, -1);
    public static final Range Range_NONE = new Range(Position_NONE, Position_NONE);
    private static final Logger LOG = Logger.getLogger("main");
    private IDELanguageClient client;
    private JavaCompilerService commonCompilerService;
    private JavaCompilerService lintCompilerService;
    private JsonObject cacheSettings;
    private JsonObject settings = new JsonObject();
    private boolean createCommonCompiler = true;
    private boolean createLintCompiler = true;
    private CompletableFuture<Object> lastLintTask;
    private CompletableFuture<Either<List<CompletionItem>, CompletionList>> lastCompletionRequest;

    public JavaLanguageServer() {
    }

    JavaCompilerService compiler() {
        if (shouldCreateCommonCompiler()) {
            commonCompilerService = createCompiler();
            cacheSettings = settings;
            createCommonCompiler = false;
        }

        return commonCompilerService;
    }

    JavaCompilerService lintCompiler() {
        if (shouldCreateLintCompiler()) {
            lintCompilerService = createCompiler();
            cacheSettings = settings;
            createLintCompiler = false;
        }

        return lintCompilerService;
    }

    private boolean shouldCreateCommonCompiler() {
        if (createCommonCompiler) {
            return true;
        }

        return !settings.equals(cacheSettings);
    }

    private boolean shouldCreateLintCompiler() {
        if (createLintCompiler) {
            return true;
        }

        return !settings.equals(cacheSettings);
    }

    private JavaCompilerService createCompiler() {
        final var classPath = classPath();
        final var addExports = addExports();
        return new JavaCompilerService(classPath, Collections.emptySet(), addExports);
    }

    private Set<Path> classPath() {
        if (!settings.has("classPath")) return Set.of();
        var array = settings.getAsJsonArray("classPath");
        Set<Path> paths = ConcurrentHashMap.newKeySet();
        for (var each : array) {
            paths.add(Paths.get(each.getAsString()).toAbsolutePath());
        }
        return paths;
    }

    private Set<String> addExports() {
        if (!settings.has("addExports")) return Set.of();
        var array = settings.getAsJsonArray("addExports");
        Set<String> strings = ConcurrentHashMap.newKeySet();
        for (var each : array) {
            strings.add(each.getAsString());
        }
        return strings;
    }

    public void lint(Collection<Path> files) {

        cancelLint();

        lastLintTask = CompletableFutures.computeAsync(checker -> {
            if (files.isEmpty()) return null;
            try (var task = lintCompiler().compile(files.toArray(Path[]::new))) {
                for (var errs : new ErrorProvider(task, checker).errors()) {
                    client.publishDiagnostics(errs);
                }
            }
            return null;
        });
    }

    private void cancelLint() {
        cancelFutureQuietly(lastLintTask);
    }

    private void cancelCompletion() {
        cancelFutureQuietly(lastCompletionRequest);
    }

    private void cancelFutureQuietly(CompletableFuture<?> future) {
        try {
            future.cancel(true);
        } catch (Throwable th) {
            // ignored
        }
    }

    private WorkspaceServerCapabilities workspaceCapabilities() {
        var c = new WorkspaceServerCapabilities();
        c.setFileOperations(fileOperationCapabilities());
        c.setWorkspaceFolders(workspaceFolderOptions());
        return c;
    }

    private WorkspaceFoldersOptions workspaceFolderOptions() {
        var options = new WorkspaceFoldersOptions();
        options.setSupported(true);
        options.setChangeNotifications(Either.forRight(true));
        return options;
    }

    private FileOperationsServerCapabilities fileOperationCapabilities() {
        var c = new FileOperationsServerCapabilities();
        c.setDidCreate(fileOperationOptions());
        c.setDidDelete(fileOperationOptions());
        c.setDidRename(fileOperationOptions());
        return c;
    }

    private FileOperationOptions fileOperationOptions() {
        var options = new FileOperationOptions();
        options.setFilters(List.of(new FileOperationFilter(new FileOperationPattern("*.java"), "file")));
        return options;
    }

    private Either<TextDocumentSyncKind, TextDocumentSyncOptions> textDocumentSyncOptions() {
        var options = new TextDocumentSyncOptions();
        options.setChange(TextDocumentSyncKind.Full);
        options.setOpenClose(true);
        options.setSave(new SaveOptions(false));
        return Either.forRight(options);
    }

    private SignatureHelpOptions signatureHelpOptions() {
        var options = new SignatureHelpOptions();
        options.setWorkDoneProgress(false);
        options.setTriggerCharacters(List.of("(", ","));
        return options;
    }

    private ExecuteCommandOptions executeCommandOptions() {
        return new ExecuteCommandOptions();
    }

    private DocumentOnTypeFormattingOptions documentOnTypeFormattingOptions() {
        return new DocumentOnTypeFormattingOptions();
    }

    private DocumentLinkOptions documentLinkOptions() {
        var options = new DocumentLinkOptions();
        options.setResolveProvider(false);
        options.setWorkDoneProgress(false);
        return options;
    }

    private CodeLensOptions codeLensOptions() {
        var options = new CodeLensOptions();
        options.setResolveProvider(false);
        options.setWorkDoneProgress(false);
        return options;
    }

    private CompletionOptions completionOptions() {
        var options = new CompletionOptions();
        options.setResolveProvider(true);
        options.setTriggerCharacters(List.of("."));
        return options;
    }

    private Rewrite createRewrite(RenameParams params) {
        var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        try (var task = compiler().compile(file)) {
            var lines = task.root().getLineMap();
            var position = lines.getPosition(params.getPosition().getLine() + 1, params.getPosition().getCharacter() + 1);
            var path = new FindNameAt(task).scan(task.root(), position);
            return Rewrite.NOT_SUPPORTED;
        }
    }

    private RegistrationParams registerClientCapabilities() {
        var c = new RegistrationParams();
        c.setRegistrations(didChangeRegistrations());
        return c;
    }

    private List<Registration> didChangeRegistrations() {
        var r = new ArrayList<Registration>();
        r.add(new Registration(UUID.randomUUID().toString(), "workspace/didChangeWatchedFiles"));
        return r;
    }

    @Override
    public void initialized() {
        initialized(null);
    }

    @Override
    public void initialized(InitializedParams params) {
        client.registerCapability(registerClientCapabilities());
        LOG.info("Server initialized");
    }

    @Override
    public void connect(IDELanguageClient client) {
        this.client = client;
    }

    @Override
    public void cancelProgress(WorkDoneProgressCancelParams p1) {
    }

    @Override
    public void exit() {
        cancelLint();
        cancelCompletion();
    }

    @Override
    public IDETextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public IDEWorkspaceService getWorkspaceService() {
        return this;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        LOG.info("Initializing Java language server");
        var folders = params.getWorkspaceFolders();
        var roots = new HashSet<Path>();
        for (var folder : folders) {
            roots.add(Paths.get(URI.create(folder.getUri())));
        }

        FileStore.setWorkspaceRoots(roots);

        var result = new InitializeResult();
        var capabilities = new ServerCapabilities();
        capabilities.setCallHierarchyProvider(false);
        capabilities.setCodeActionProvider(true);
        capabilities.setCodeLensProvider(codeLensOptions());
        capabilities.setColorProvider(true);
        capabilities.setCompletionProvider(completionOptions());
        capabilities.setDeclarationProvider(false);
        capabilities.setDefinitionProvider(true);
        capabilities.setDocumentFormattingProvider(true);
        capabilities.setDocumentHighlightProvider(true);
        capabilities.setDocumentLinkProvider(documentLinkOptions());
        capabilities.setDocumentOnTypeFormattingProvider(documentOnTypeFormattingOptions());
        capabilities.setDocumentRangeFormattingProvider(false);
        capabilities.setDocumentSymbolProvider(true);
        capabilities.setExecuteCommandProvider(executeCommandOptions());
        capabilities.setFoldingRangeProvider(true);
        capabilities.setHoverProvider(true);
        capabilities.setImplementationProvider(false);
        capabilities.setLinkedEditingRangeProvider(false);
        capabilities.setMonikerProvider(false);
        capabilities.setReferencesProvider(true);
        capabilities.setRenameProvider(true);
        capabilities.setSelectionRangeProvider(false);
        capabilities.setSignatureHelpProvider(signatureHelpOptions());
        capabilities.setTextDocumentSync(textDocumentSyncOptions());
        capabilities.setTypeDefinitionProvider(true);
        capabilities.setTypeHierarchyProvider(false);
        capabilities.setWorkspace(workspaceCapabilities());
        capabilities.setWorkspaceSymbolProvider(true);
        result.setCapabilities(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        LOG.info("Shutting down Java language server");
        return CompletableFuture.completedFuture(new Object());
    }


    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        if (!FileStore.isJavaFile(URI.create(params.getTextDocument().getUri()))) {
            throw new IllegalArgumentException("File must be Java file");
        }
        var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        var task = compiler().parse(file);
        return CompletableFuture.completedFuture(CodeLensProvider.find(task));
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {

        LOG.info("Got completion request");
        cancelCompletion();
        LOG.info("cancelCompletion()");

        return lastCompletionRequest = CompletableFutures.computeAsync(checker -> {

            LOG.info("Inside completion future");
            var file = Paths.get(URI.create(params.getTextDocument().getUri()));
            var provider = new CompletionProvider(compiler());
            var line = params.getPosition().getLine() + 1;
            var character = params.getPosition().getCharacter() + 1;
            if (!FileStore.isJavaFile(URI.create(params.getTextDocument().getUri()))) {
                throw new IllegalArgumentException("Cannot execute further. File must be a Java file.");
            }

            LOG.info("Calling CompletionProvider.complete()");
            return provider.complete(checker, file, line, character);
        });
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
        var provider = new SymbolProvider(compiler());
        return CompletableFutures.computeAsync(checker -> {
            return provider.findSymbols(checker, params.getQuery(), 50);
        });
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams change) {
        var java = JsonHelper.GSON.toJsonTree(change.getSettings()).getAsJsonObject().get("java");
        LOG.info("Received java settings " + java);
        settings = java.getAsJsonObject();

        lint(FileStore.activeDocuments());
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        for (var c : params.getChanges()) {
            var file = Paths.get(URI.create(c.getUri()));
            if (FileStore.isJavaFile(file)) {
                switch (c.getType()) {
                    case Created:
                        FileStore.externalCreate(file);
                        break;
                    case Changed:
                        FileStore.externalChange(file);
                        break;
                    case Deleted:
                        FileStore.externalDelete(file);
                        break;
                }
                return;
            }
            var name = file.getFileName().toString();
            switch (name) {
                case "BUILD":
                case "pom.xml":
                    LOG.info("Compiler needs to be re-created because " + file + " has changed");
                    createCommonCompiler = createLintCompiler = true;
            }
        }
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        if (!FileStore.isJavaFile(URI.create(params.getTextDocument().getUri()))) {
            throw new IllegalArgumentException("File must be a Java file");
        }
        var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        var line = params.getPosition().getLine() + 1;
        var column = params.getPosition().getCharacter() + 1;
        var provider = new SignatureProvider(compiler());
        return CompletableFutures.computeAsync(checker -> {
            var help = provider.signatureHelp(checker, file, line, column);
            if (help == null) {
                help = new SignatureHelp();
                help.setActiveParameter(-1);
                help.setActiveSignature(-1);
                help.setSignatures(List.of());
            }
            return help;
        });
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams position) {
        if (!FileStore.isJavaFile(URI.create(position.getTextDocument().getUri()))) {
            throw new IllegalArgumentException("File must be a Java file");
        }
        var file = Paths.get(URI.create(position.getTextDocument().getUri()));
        var line = position.getPosition().getLine() + 1;
        var column = position.getPosition().getCharacter() + 1;
        var provider = new DefinitionProvider(compiler(), file, line, column);
        return CompletableFutures.computeAsync(checker -> {
            var found = provider.find(checker);
            return Either.forLeft(found);
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams position) {
        if (!FileStore.isJavaFile(URI.create(position.getTextDocument().getUri()))) {
            throw new IllegalArgumentException("File must be a Java file");
        }
        var file = Paths.get(URI.create(position.getTextDocument().getUri()));
        var line = position.getPosition().getLine() + 1;
        var column = position.getPosition().getCharacter() + 1;
        var provider = new ReferenceProvider(compiler(), file, line, column);
        return CompletableFutures.computeAsync(checker -> {
            return provider.find(checker);
        });
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        if (!FileStore.isJavaFile(URI.create(params.getTextDocument().getUri())))
            return CompletableFuture.completedFuture(null);
        var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        var provider = new SymbolProvider(compiler());
        return CompletableFutures.computeAsync(checker -> {
            return provider.documentSymbols(checker, file);
        });
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        if (!FileStore.isJavaFile(URI.create(params.getTextDocument().getUri())))
            return CompletableFuture.completedFuture(List.of());
        var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        var provider = new FoldProvider(compiler());

        return CompletableFutures.computeAsync(checker -> {
            return provider.foldingRanges(checker, file);
        });
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        var rw = createRewrite(params);
        var response = new WorkspaceEdit();
        return CompletableFutures.computeAsync(checker -> {
            var map = rw.rewrite(compiler());
            checker.checkCanceled();
            response.setChanges(new HashMap<>());
            for (var editedFile : map.keySet()) {
                response.getChanges().put(editedFile.toUri().toString(), List.of(map.get(editedFile)));
            }
            return response;
        });
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        FileStore.change(params);
        final var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        if (FileStore.isJavaFile(file)) {
            lint(List.of(file));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        FileStore.close(params);
        if (FileStore.isJavaFile(URI.create(params.getTextDocument().getUri()))) {
            client.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), List.of()));
        }
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        FileStore.open(params);
        final var file = Paths.get(URI.create(params.getTextDocument().getUri()));
        if (!FileStore.isJavaFile(file)) return;
        lint(List.of(file));
    }

    @Override
    public void didSave(@NotNull DidSaveTextDocumentParams params) {
        if (FileStore.isJavaFile(URI.create(params.getTextDocument().getUri()))) {
            lint(FileStore.activeDocuments());
        }
    }

    @Override
    public CompletableFuture<List<SemanticHighlight>> semanticHighlights(SemanticHighlightParams params) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            final var path = Paths.get(URI.create(params.getTextDocument().getUri()));
            if (!FileStore.isJavaFile(path)) {
                throw new ResponseErrorException(new ResponseError(ResponseErrorCode.InvalidParams, "File is not a java file", null));
            }

            try (var task = lintCompiler().compile(path)) {
                final var highlighter = new SemanticHighlightProvider(task, cancelChecker);
                return highlighter.highlights();
            }
        });
    }
}