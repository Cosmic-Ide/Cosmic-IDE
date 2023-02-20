package com.tyron.javacompletion.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/** Manages all files for the same project. */
public interface FileManager {
    /**
     * Opens a file and reads its content into a {@link FileSnapshot}.
     *
     * <p>The truth of the file becomes the snapshot.
     */
    void openFileForSnapshot(URI fileUri, String content) throws IOException;

    /**
     * Applies file content changes to a file opened for snapshotting.
     *
     * @param fileUri the URI to identify the file snapshot opened by {@link #openFileForSnapshot}
     * @param editRange the range of the content being modified. Note that only part of the content
     *     within the range will be replaced, depending on the value of {@code rangeLength}
     * @param rangeLength the length of the content within {@code editRange} to be replaced. If it's
     *     shorter than the actual range in {@code editRange}, only the partial of the range starting
     *     from the begin of {@code editRange} will be replaced. If it's longer than the actual range
     *     in {@code editRange}, or it's absent, the whole {@code editRange} will be replaced
     * @param newText the new content to replace the original content with {@code editRange}
     */
    void applyEditToSnapshot(
            URI fileUri, TextRange editRange, Optional<Integer> rangeLength, String newText);

    /**
     * Replace the content of the file snapshot.
     *
     * @param fileUri the URI to identify the file snapshot opened by {@link #openFileForSnapshot}
     * @param newText the new content of the snapshot
     */
    void setSnaphotContent(URI fileUri, String newText);

    /**
     * Closes a file opened for snapshotting.
     *
     * <p>The truth of the file becomes the content in the filesystem.
     */
    void closeFileForSnapshot(URI fileUri);

    /** Watches file changes under {@code rootDirectory} and all its subdirectories. */
    void watchSubDirectories(Path rootDirectory);

    /** Sets listener for file changes under watched directories and snapshots. */
    void setFileChangeListener(FileChangeListener listener);

    /**
     * Gets the content of a file.
     *
     * <p>If the file is opened for snapshotting by {@link #openFileForSnapshot}, return the content
     * of the snapshot. Otherwise return the content of the file in filesystem.
     */
    Optional<CharSequence> getFileContent(Path filePath);

    /**
     * Gets the edit history of a file.
     *
     * <p>It's only present if the file is opened for snapshotting.
     */
    Optional<EditHistory> getFileEditHistory(Path filePath);

    /**
     * Shuts down the file manager.
     *
     * <p>All snapshotted files opened by {@link #openFileForSnapshot} will be closed.
     */
    void shutdown();

    /** Whether a given path should be ignored. */
    boolean shouldIgnorePath(Path path);

    Path getProjectRootPath();
}
