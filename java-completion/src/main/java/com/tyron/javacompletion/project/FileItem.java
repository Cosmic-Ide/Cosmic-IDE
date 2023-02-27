/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.project;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;

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