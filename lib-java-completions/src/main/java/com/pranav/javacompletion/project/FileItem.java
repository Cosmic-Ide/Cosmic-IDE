package com.pranav.javacompletion.project;

import com.google.auto.value.AutoValue;
import com.pranav.javacompletion.model.FileScope;
import com.pranav.javacompletion.model.Module;

import java.nio.file.Path;

/** Information about a parsed file in a project. */
@AutoValue
public abstract class FileItem {
    public abstract Module getModule();

    public abstract FileScope getFileScope();

    public abstract Path getPath();

    public static Builder newBuilder() {
        return new AutoValue_FileItem.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setModule(Module module);

        public abstract Builder setFileScope(FileScope fileScope);

        public abstract Builder setPath(Path path);

        public abstract FileItem build();
    }
}
