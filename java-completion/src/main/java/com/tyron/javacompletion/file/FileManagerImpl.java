package com.tyron.javacompletion.file;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.logging.JLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

/** Manages all files for the same project. */
public class FileManagerImpl implements FileManager {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    /**
     * A map from normalized file name to snapshotted files.
     *
     * <p>All snapshotted files are opened by clients. The the truth of the opened files is the
     * snapshot, not the file content stored on file system.
     */
    private final Map<Path, FileSnapshot> fileSnapshots;

    private final Path projectRoot;
    private final FileWatcher fileWatcher;
    private final ImmutableList<PathMatcher> ignorePathMatchers;

    public FileManagerImpl(
            URI projectRootUri, List<String> ignorePathPatterns, ExecutorService executor) {
        projectRoot = Paths.get(projectRootUri);

        if (ignorePathPatterns.isEmpty()) {
            ignorePathMatchers = PathUtils.DEFAULT_IGNORE_MATCHERS;
        } else {
            FileSystem fs = FileSystems.getDefault();
            ImmutableList.Builder<PathMatcher> ignorePathMatchersBuilder = new ImmutableList.Builder<>();
            for (String pattern : ignorePathPatterns) {
                PathMatcher matcher;
                try {
                    matcher = fs.getPathMatcher("glob:" + pattern);
                    ignorePathMatchersBuilder.add(matcher);
                } catch (Throwable t) {
                    logger.warning(t, "Invalid ignore path pattern %s", pattern);
                }
            }
            ignorePathMatchers = ignorePathMatchersBuilder.build();
        }

        fileSnapshots = new HashMap<>();
        fileWatcher = new FileWatcher(projectRoot, ignorePathMatchers, executor);
        watchSubDirectories(uriToNormalizedPath(projectRootUri));
    }

    @Override
    public void openFileForSnapshot(URI fileUri, String content) throws IOException {
        Path filePath = uriToNormalizedPath(fileUri);
        if (fileSnapshots.containsKey(filePath)) {
            throw new IllegalStateException(String.format("File %s has already been opened.", fileUri));
        }
        FileSnapshot fileSnapshot = FileSnapshot.create(filePath.toUri(), content);
        fileSnapshots.put(filePath, fileSnapshot);
        fileWatcher.watchFileSnapshotPath(filePath);
        if (Files.exists(filePath)) {
            fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_MODIFY);
        } else {
            fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_CREATE);
        }
    }

    @Override
    public void applyEditToSnapshot(
            URI fileUri, TextRange editRange, Optional<Integer> rangeLength, String newText) {
        Path filePath = uriToNormalizedPath(fileUri);
        if (!fileSnapshots.containsKey(filePath)) {
            throw new IllegalStateException(
                    String.format("Cannot apply edit to file %s: file is not opened.", fileUri));
        }

        fileSnapshots.get(filePath).applyEdit(editRange, rangeLength, newText);
        fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    public void setSnaphotContent(URI fileUri, String newText) {
        Path filePath = uriToNormalizedPath(fileUri);
        if (!fileSnapshots.containsKey(filePath)) {
            throw new IllegalStateException(
                    String.format("Cannot apply edit to file %s: file is not opened.", fileUri));
        }

        fileSnapshots.get(filePath).setContent(newText);
        fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    public void closeFileForSnapshot(URI fileUri) {
        Path filePath = uriToNormalizedPath(fileUri);
        if (!fileSnapshots.containsKey(filePath)) {
            throw new IllegalStateException(
                    String.format("Cannot close file %s: file is not opened.", fileUri));
        }

        fileSnapshots.remove(filePath);
        if (Files.exists(filePath)) {
            fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_MODIFY);
        } else {
            fileWatcher.notifyFileChange(filePath, StandardWatchEventKinds.ENTRY_DELETE);
        }
    }

    @Override
    public void watchSubDirectories(Path rootDirectory) {
        if (!Files.isDirectory(rootDirectory)) {
            return;
        }
        Queue<Path> directories = new LinkedList<>();
        directories.add(rootDirectory);

        while (!directories.isEmpty()) {
            Path directory = directories.remove();
            if (!fileWatcher.watchDirectory(directory)) {
                continue;
            }

            try (DirectoryStream<Path> directoryStream =
                         Files.newDirectoryStream(directory, file -> Files.isDirectory(file))) {
                for (Path subDir : directoryStream) {
                    directories.add(subDir);
                }
            } catch (Throwable e) {
                logger.warning(e, "Cannot list files in directory %s", directory);
            }
        }
    }

    @Override
    public void setFileChangeListener(FileChangeListener listener) {
        fileWatcher.setListener(listener);
    }

    @Override
    public Optional<CharSequence> getFileContent(Path filePath) {
        Path normalizedPath = filePath.normalize();
        if (fileSnapshots.containsKey(normalizedPath)) {
            return Optional.of(
                    fileSnapshots.get(normalizedPath).getCharContent(true /* ignoreEncodingErrors */));
        }

        try {
            return Optional.of(new String(Files.readAllBytes(normalizedPath), UTF_8));
        } catch (Exception e) {
            logger.severe(e, "Failed to read content from file %s", normalizedPath);
        }
        return Optional.empty();
    }

    @Override
    public Optional<EditHistory> getFileEditHistory(Path filePath) {
        Path normalizedPath = filePath.normalize();
        if (fileSnapshots.containsKey(normalizedPath)) {
            return Optional.of(fileSnapshots.get(normalizedPath).getEditHistory());
        }
        return Optional.empty();
    }

    @Override
    public void shutdown() {
        fileSnapshots.clear();
    }

    @Override
    public boolean shouldIgnorePath(Path path) {
        return PathUtils.shouldIgnorePath(path, projectRoot, ignorePathMatchers);
    }

    @Override
    public Path getProjectRootPath() {
        return projectRoot;
    }

    /**
     * Convert a {@link URI} to a normalized {@link Path}. If the path is relative, throws an
     * exception.
     *
     * @throws IllegalArgumentException thrown if the URI is a relative path
     */
    private static Path uriToNormalizedPath(URI fileUri) {
        Path filePath = Paths.get(fileUri);
        if (!filePath.isAbsolute()) {
            throw new IllegalArgumentException("Cannot open a relative URI: " + fileUri);
        }

        filePath = filePath.normalize();
        return filePath;
    }
}