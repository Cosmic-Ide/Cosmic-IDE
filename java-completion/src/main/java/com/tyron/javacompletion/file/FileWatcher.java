package com.tyron.javacompletion.file;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.logging.JLogger;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A wrapper around {@link WatchService} that supports watching both file system files and
 * snapshotted files.
 */
class FileWatcher {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private final WatchService watchService;
    private final Map<Path, WatchKey> watchKeyMap;
    private final Set<Path> fileSnapshotPaths;
    private final ExecutorService executor;
    private final Path projectRoot;
    private final ImmutableList<PathMatcher> ignorePathMatchers;

    private Future<?> watchFuture = null;
    private FileChangeListener listener = null;

    FileWatcher(
            Path projectRoot, ImmutableList<PathMatcher> ignorePathMatchers, ExecutorService executor) {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.watchKeyMap = new HashMap<>();
        this.fileSnapshotPaths = new HashSet<>();
        this.executor = executor;
        this.projectRoot = projectRoot;
        this.ignorePathMatchers = ignorePathMatchers;
    }

    synchronized void setListener(FileChangeListener listener) {
        this.listener = listener;
        unsafeStartWatcher();
    }

    private void unsafeStartWatcher() {
        if (watchFuture != null) {
            return;
        }
        watchFuture = executor.submit(new WatchRunnable());
    }

    synchronized boolean watchDirectory(Path path) {

        if (PathUtils.shouldIgnorePath(path, this.projectRoot, this.ignorePathMatchers)) {
            logger.info("Ignore watching directory %s", path);
            return false;
        }

        Path normalizedPath = path.normalize();
        if (watchKeyMap.containsKey(normalizedPath)) {
            logger.info("Directory %s has already been watched.", path);
            return false;
        }

        try {
            WatchKey watchKey =
                    path.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
            watchKeyMap.put(path, watchKey);
            return true;
        } catch (IOException e) {
            logger.warning(e, "Cannot watch directory %s.", path);
        }
        return false;
    }

    private synchronized void unwatchDirectory(Path path) {
        Path normalizedPath = path.normalize();
        if (!watchKeyMap.containsKey(normalizedPath)) {
            logger.info("Directory %s is not being watched.", path);
            return;
        }

        WatchKey watchKey = watchKeyMap.remove(normalizedPath);
        watchKey.cancel();
    }

    synchronized void watchFileSnapshotPath(Path path) {
        Path normalizedPath = path.normalize();
        fileSnapshotPaths.add(path);
    }

    synchronized void unwatchFileSnapshotPath(Path path) {
        Path normalizedPath = path.normalize();
        fileSnapshotPaths.remove(path);
    }

    synchronized void shutdown() {
        try {
            watchService.close();
        } catch (IOException e) {
            // Ignore.
        }
        if (watchFuture != null) {
            watchFuture.cancel(true /* mayInterrupt */);
            watchFuture = null;
        }
        watchKeyMap.clear();
        fileSnapshotPaths.clear();
    }

    synchronized void notifyFileChange(Path path, WatchEvent.Kind<?> eventKind) {
        if (PathUtils.shouldIgnorePath(path, projectRoot, ignorePathMatchers)) {
            return;
        }

        if (listener == null) {
            return;
        }
        try {
            listener.onFileChange(path, eventKind);
        } catch (Throwable e) {
            logger.warning(e, "File watch listener throws exception.");
        }
    }

    private class WatchRunnable implements Runnable {
        @Override
        public void run() {
            for (; ; ) {
                WatchKey watchKey;
                try {
                    watchKey = watchService.take();
                } catch (ClosedWatchServiceException | InterruptedException e) {
                    // The watcher is shutdown, stop running
                    return;
                }
                @SuppressWarnings("unchecked")
                Path dir = (Path) watchKey.watchable();
                synchronized (FileWatcher.this) {
                    checkState(listener != null, "Watcher doesn't have listener");
                    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) watchEvent;
                        handleWatchEvent(dir, pathEvent);
                    }
                    watchKey.reset();
                }
            }
        }

        private void handleWatchEvent(Path dir, WatchEvent<Path> event) {
            WatchEvent.Kind<?> eventKind = event.kind();
            if (eventKind == StandardWatchEventKinds.OVERFLOW) {
                return;
            }

            Path fullPath = dir.resolve(event.context());

            if (PathUtils.shouldIgnorePath(fullPath, projectRoot, ignorePathMatchers)) {
                return;
            }

            if (fileSnapshotPaths.contains(fullPath)) {
                // The file is managed by file snapshots. Ignore file system events.
                return;
            }

            if (Files.isDirectory(fullPath)) {
                handleDirectoryEvent(fullPath, eventKind);
                return;
            }

            notifyFileChange(fullPath, event.kind());
        }

        private void handleDirectoryEvent(Path path, WatchEvent.Kind<?> eventKind) {
            if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                // New directory created, watch it.
                watchNewDirectory(path);
            } else if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
                unwatchDirectory(path);
            }
        }

        private void watchNewDirectory(Path path) {
            Queue<Path> newDirectories = new LinkedList<>();
            Queue<Path> newFiles = new LinkedList<>();
            newDirectories.add(path);
            while (!newDirectories.isEmpty()) {
                Path dir = newDirectories.remove();
                if (!watchDirectory(dir)) {
                    // The directory is being monitored, skip files under it.
                    continue;
                }
                // There may be delay between the directory is created and the event is signaled.
                // During such delay new files may be created. List all files in the directory and
                // explicitly
                // call listeners and watch new subdirectories.

                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
                    for (Path file : directoryStream) {
                        if (Files.isDirectory(file)) {
                            newDirectories.add(file);
                        } else {
                            newFiles.add(file);
                        }
                    }
                } catch (Throwable t) {
                    logger.severe(t, "Cannot list files in directory %s", path);
                }
            }

            for (Path newFile : newFiles) {
                notifyFileChange(newFile, StandardWatchEventKinds.ENTRY_CREATE);
            }
        }
    }
}