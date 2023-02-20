package com.tyron.javacompletion.project;

import java.nio.file.Path;
import java.util.Optional;
import com.tyron.javacompletion.file.SimpleFileManager;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.options.IndexOptions;
import com.tyron.javacompletion.parser.Parser;

/**
 * A {@link ModuleManager} that requires manually adding files.
 *
 * <p>It's mainly used by tests or tools.
 */
public class SimpleModuleManager implements ModuleManager {
    private final Module module = new Module();
    private final SimpleFileManager fileManager;
    private final Parser parser;

    public SimpleModuleManager() {
        this(new SimpleFileManager(), IndexOptions.FULL_INDEX_BUILDER.build());
    }

    public SimpleModuleManager(SimpleFileManager fileManager, IndexOptions indexOptions) {
        this.fileManager = fileManager;
        this.parser = new Parser(fileManager, indexOptions);
    }

    @Override
    public void initialize() {}

    @Override
    public Optional<FileItem> getFileItem(Path path) {
        Optional<FileScope> fileScope = module.getFileScope(path.toString());
        if (fileScope.isPresent()) {
            return Optional.of(
                    FileItem.newBuilder()
                            .setModule(module)
                            .setFileScope(fileScope.get())
                            .setPath(path)
                            .build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void addOrUpdateFile(Path path, boolean fixContentForParsing) {
        Optional<FileScope> fileScope = parser.parseSourceFile(path, fixContentForParsing);
        if (fileScope.isPresent()) {
            module.addOrReplaceFileScope(fileScope.get());
        }
    }

    @Override
    public void removeFile(Path path) {
        module.removeFile(path);
    }

    @Override
    public void addDependingModule(Module dependingModule) {
        module.addDependingModule(dependingModule);
    }

    public Module getModule() {
        return module;
    }

    public SimpleFileManager getFileManager() {
        return fileManager;
    }
}