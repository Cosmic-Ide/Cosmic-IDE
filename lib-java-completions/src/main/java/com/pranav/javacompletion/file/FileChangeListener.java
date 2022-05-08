package com.pranav.javacompletion.file;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface FileChangeListener {
    void onFileChange(Path filePath, WatchEvent.Kind<?> eventKind);
}
