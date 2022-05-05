package com.pranav.javacompletion.tool;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.pranav.javacompletion.file.FileManager;
import com.pranav.javacompletion.file.PathUtils;
import com.pranav.javacompletion.file.SimpleFileManager;
import com.pranav.javacompletion.model.FileScope;
import com.pranav.javacompletion.model.Module;
import com.pranav.javacompletion.options.IndexOptions;
import com.pranav.javacompletion.parser.AstScanner;
import com.pranav.javacompletion.parser.ParserContext;
import com.pranav.javacompletion.parser.classfile.ClassModuleBuilder;
import com.pranav.javacompletion.project.Project;
import com.pranav.javacompletion.project.SimpleModuleManager;
import com.pranav.javacompletion.storage.IndexStore;

/**
 * Creates index files for specified source code.
 *
 * <p>Usage: Indexer <root path> <output file> <ignored paths...>
 */
public class Indexer {

    private final ParserContext parserContext = new ParserContext();

    public Indexer() {
    }

    public void run(
            List<String> inputPaths,
            String outputPath,
            List<String> ignorePaths,
            List<String> dependIndexFiles,
            boolean withJdk) {
        // Do not initialize the project. We handle the files on our own.
        SimpleModuleManager moduleManager = new SimpleModuleManager();
        Project project = new Project(moduleManager, moduleManager.getFileManager());
        for (String inputPath : inputPaths) {
            Path path = Paths.get(inputPath);
            // Do not use module manager's file manager because we need to setup root
            // path and ignore paths per directory.
            FileManager fileManager = new SimpleFileManager(path, ignorePaths);
            ClassModuleBuilder classModuleBuilder = new ClassModuleBuilder(moduleManager.getModule());
            ImmutableMap<String, Consumer<Path>> handlers =
                    ImmutableMap.<String, Consumer<Path>>of(
                            ".class",
                            classModuleBuilder::processClassFile,
                            ".java",
                            subpath -> addJavaFile(subpath, moduleManager.getModule(), fileManager));
            if (Files.isDirectory(path)) {
                System.out.println("Indexing directory: " + inputPath);
                PathUtils.walkDirectory(
                        path,
                        handlers,
                        /* ignorePredicate= */ fileManager::shouldIgnorePath);
            } else if (inputPath.endsWith(".jar") || inputPath.endsWith(".srcjar")) {
                System.out.println("Indexing JAR file: " + inputPath);
                try {
                    PathUtils.walkDirectory(
                            PathUtils.getRootPathForJarFile(path),
                            handlers,
                            /* ignorePredicate= */ subpath -> false);
                } catch (IOException t) {
                    throw new RuntimeException(t);
                }
            }
        }
        for (String dependIndexFile : dependIndexFiles) {
            project.loadTypeIndexFile(dependIndexFile);
        }
        if (withJdk) {
            project.loadJdkModule();
        }
        System.out.println("Writing index file to " + outputPath);
        new IndexStore().writeModuleToFile(moduleManager.getModule(), Paths.get(outputPath));
    }

    private void addJavaFile(Path path, Module module, FileManager fileManager) {
        Optional<CharSequence> content = fileManager.getFileContent(path);
        if (content.isPresent()) {
            FileScope fileScope =
                    new AstScanner(IndexOptions.NON_PRIVATE_BUILDER.build())
                            .startScan(
                                    parserContext.parse(path.toString(), content.get()),
                                    path.toString(),
                                    content.get());
            module.addOrReplaceFileScope(fileScope);
        }
    }

    /**
     * Convenience method for invoking the Indexer through code
     * @param jarFiles List of jar paths to index
     * @param outputFile The output file (not a directory)
     * @param ignoredPaths List of paths to ignore
     * @param indexFiles List of other indexes that this library might depend on
     */
    public static void createIndex(List<String> jarFiles,
                                   String outputFile,
                                   List<String> ignoredPaths,
                                   List<String> indexFiles) {
        new Indexer().run(jarFiles, outputFile, ignoredPaths, indexFiles, false);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println(
                    "Usage: Indexer <directory or jar file>[, directory or jar file...]  -o <output file> [options]");
            System.out.println("  Options:");
            System.out.println("    --depend|-d <index files...>");
            System.out.println("    --ignore|-i <ignored paths...>]");
            System.out.println("    --no-jdk      Do not load JDK module.");
            return;
        }
        String outputPath = null;
        List<String> inputPaths = new ArrayList<>();
        List<String> ignorePaths = new ArrayList<>();
        List<String> dependIndexPaths = new ArrayList<>();
        List<String> currentList = inputPaths;
        boolean withJdk = true;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-o".equals(arg)) {
                if (i + 1 < args.length) {
                    outputPath = args[i + 1];
                    i++;
                    currentList = null;
                }
            } else if ("--depend".equals(arg) || "-d".equals(arg)) {
                currentList = dependIndexPaths;
            } else if ("--ignore".equals(arg) || "-i".equals(arg)) {
                currentList = ignorePaths;
            } else if ("--no-jdk".equals(arg)) {
                withJdk = false;
            } else if (currentList == null) {
                System.err.println("-o only accepts one value");
                System.exit(1);
            } else {
                currentList.add(arg);
            }
        }

        if (outputPath == null) {
            System.err.println("-o must be specified with one value");
            System.exit(1);
        }
        if (inputPaths.isEmpty()) {
            System.err.println("One or more input file must be specified");
            System.exit(1);
        }

        new Indexer().run(inputPaths, outputPath, ignorePaths, dependIndexPaths, withJdk);
    }
}