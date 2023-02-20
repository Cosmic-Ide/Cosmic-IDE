package com.tyron.javacompletion.file;

import com.github.marschall.com.sun.nio.zipfs.ZipFileSystemProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tyron.javacompletion.logging.JLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Utilities for dealing with paths. */
public class PathUtils {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    public static final ImmutableList<PathMatcher> DEFAULT_IGNORE_MATCHERS;
    private static final Path PSUEDO_ROOT_PATH = Paths.get("/");

    static {
        FileSystem fs = FileSystems.getDefault();
        DEFAULT_IGNORE_MATCHERS =
                ImmutableList.of(
                        // File names starting with a dot are hidden files in *nix systems.
                        fs.getPathMatcher("glob:.*"),
                        // File names ending with ~ are common backup file names.
                        fs.getPathMatcher("glob:*~"),
                        // File names ending with .bak are common backup file names.
                        fs.getPathMatcher("glob:*.bak"));
    }

    private PathUtils() {}

    /**
     * Checks whether {@code entryPath} should be ignored according to any of the matchers in {@code
     * ignorePathMatchers}.
     *
     * <p>{@code ignorePathMatchers} are used on the filename part of {@code entryPath}, and the
     * pseudo absolute path created from {@code entryPath} and {@code projectRootPath}.
     *
     * <p>A pseudo absolute is {@code entryPath} itself if it's not under {@code projectRootPath} or
     * {@code entryPath} with the {@code projectRootPath} part removed and the remaining path as an
     * absolute path. For * example, if {@code entryPath} is {@code /root/path/foo/bar}, and {@code
     * projectRootPath} is {@code /root/path}, then the pseudo absolute path is {@code /foo/bar}. This
     * allows clients easily configre matchers relative to the project root path without worrying what
     * the root path is.
     */
    public static boolean shouldIgnorePath(
            Path entryPath, Path projectRootPath, List<PathMatcher> ignorePathMatchers) {
        if (ignorePathMatchers.isEmpty()) {
            return false;
        }

        Path pathName = entryPath.getFileName();
        Path relativePath;
        try {
            relativePath = projectRootPath.relativize(entryPath);
        } catch (ProviderMismatchException e) {
            // entryPath and projectRootPath are not provided by the same filesystem,
            // e.g. entryPath is a path in a .jar file while the projectRootPath is in
            // the default filesystem.
            return false;
        }
        Path pseudoAbsolutePath = PSUEDO_ROOT_PATH.resolve(relativePath);

        for (PathMatcher matcher : ignorePathMatchers) {
            if (matcher.matches(pseudoAbsolutePath)) {
                return true;
            }
            if (matcher.matches(pathName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param rootPath the root path to walk through
     * @param extensionHandlers map of extension (with leading dot) to consumers The consumers will be
     *     called when accessing files with corresponding extensions. The consumers are only called on
     *     files, not directories
     * @param ignorePredicate a predicate to determine whether a path should be ignored. If it returns
     *     true, the path will not be consumed by {@code extensionHandlers} if its a file, or walked
     *     through if it's a directory
     */
    public static void walkDirectory(
            Path rootPath,
            Map<String, Consumer<Path>> extensionHandlers,
            Predicate<Path> ignorePredicate) {
        Deque<Path> queue = new LinkedList<>();
        queue.add(rootPath);
        while (!queue.isEmpty()) {
            Path baseDir = queue.remove();
            try (Stream<Path> entryStream = Files.list(baseDir)) {
                entryStream.forEach(
                        entryPath -> {
                            if (ignorePredicate.test(entryPath)) {
                                logger.info("Ignoring path %s", entryPath);
                                return;
                            }
                            if (Files.isDirectory(entryPath)) {
                                queue.add(entryPath);
                                return;
                            }

                            for (Map.Entry<String, Consumer<Path>> entry : extensionHandlers.entrySet()) {
                                if (entryPath.toString().endsWith(entry.getKey())) {
                                    entry.getValue().accept(entryPath);
                                    return;
                                }
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Returns a {@link Path} that can be use for walking through its content. */
    public static Path getRootPathForJarFile(Path jarFilePath) throws IOException {
        // JAR specific URI pattern.
        // See https://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
        logger.fine("Parsing jar file: %s", jarFilePath);
        URI uri = null;
        try {
            uri = new URI("jar", jarFilePath.toUri().toString(), null);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        FileSystem fs = ZipFileSystemProvider.newJarFileSystem(uri, ImmutableMap.of() /* env */);
        return fs.getPath("/");
    }
}